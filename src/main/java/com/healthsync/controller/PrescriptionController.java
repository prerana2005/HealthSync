package com.healthsync.controller;

import com.healthsync.config.RoleGuard;
import com.healthsync.model.*;
import com.healthsync.observer.NotificationObserver;
import com.healthsync.repository.*;
import com.healthsync.singleton.AuditLogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * PrescriptionController — UC3: Generate E-Prescription (<<extend>> of Manage Medical Records)
 *
 * Closes the gap between the use case diagram ("Generate E-Prescription" extends UC3)
 * and the implementation.  A Doctor creates a prescription linked to a MedicalRecord;
 * the Observer pattern notifies the patient and the audit log records the action.
 *
 * PATTERNS USED:
 *   Observer  (Behavioral): NotificationObserver.onEvent() — notifies patient when
 *     prescription is generated (state: PrescriptionAdded → sendToPharmacy() in state diagram).
 *   Singleton (Creational #2): AuditLogService — single instance audits every prescription event.
 *
 * SOLID SRP : Only handles prescription HTTP concerns.
 * GRASP Low Coupling : no direct dependency on notification table.
 */
@RestController
@RequestMapping("/api/prescriptions")
@CrossOrigin(origins = "*")
public class PrescriptionController {

    @Autowired private PrescriptionRepository prescriptionRepo;
    @Autowired private MedicalRecordRepository recordRepo;
    @Autowired private DoctorRepository doctorRepo;
    @Autowired private PatientRepository patientRepo;

    /** Singleton (Creational #2) */
    @Autowired private AuditLogService auditLogService;

    /** Observer (Behavioral) — decoupled notification delivery */
    @Autowired private NotificationObserver notificationObserver;

    // ─── Read ────────────────────────────────────────────────────────────

    @GetMapping("/patient/{patientId}")
    public ResponseEntity<?> byPatient(@PathVariable String patientId) {
        return ResponseEntity.ok(
                prescriptionRepo.findByPatientPatientId(patientId)
                        .stream().map(this::toMap).toList()
        );
    }

    @GetMapping("/pending")
    public ResponseEntity<?> pending(@RequestHeader(value = "X-User-Role", required = false) String role) {
        ResponseEntity<?> denied = RoleGuard.requireRole(role, "ADMINISTRATOR", "STAFF");
        if (denied != null) return denied;
        return ResponseEntity.ok(
                prescriptionRepo.findByStatus(Prescription.PrescriptionStatus.PENDING)
                        .stream().map(this::toMap).toList()
        );
    }

    // ─── Create — UC3: Doctor generates e-prescription ──────────────────

    /**
     * Activity Diagram UC3: Doctor → Write Prescription → Send to Pharmacy.
     * State Diagram: AddingNotes → writePrescription() [confirmed] → PrescriptionAdded
     *                → do/generatePrescription() → do/sendToPharmacy().
     */
    @PostMapping
    public ResponseEntity<?> create(
            @RequestHeader(value = "X-User-Role", required = false) String role,
            @RequestBody Map<String, String> body) {

        ResponseEntity<?> denied = RoleGuard.requireRole(role, "DOCTOR", "ADMINISTRATOR");
        if (denied != null) return denied;

        try {
            MedicalRecord record = recordRepo.findById(body.get("recordId")).orElse(null);
            Doctor doctor = doctorRepo.findById(body.get("doctorId")).orElse(null);
            Patient patient = patientRepo.findById(body.get("patientId")).orElse(null);

            if (record == null || doctor == null || patient == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "Invalid record, doctor, or patient"));
            }

            int nextId = prescriptionRepo.findMaxPrescriptionId() + 1;
            Prescription presc = new Prescription();
            presc.setPrescriptionId(String.format("RX%08d", nextId));
            presc.setRecord(record);
            presc.setDoctor(doctor);
            presc.setPatient(patient);
            presc.setStatus(Prescription.PrescriptionStatus.PENDING);
            prescriptionRepo.save(presc);

            // Observer: notify patient — state PrescriptionAdded → sendToPharmacy()
            notificationObserver.onEvent(
                    patient.getUser(),
                    Notification.NotifType.PRESCRIPTION,
                    "E-Prescription Ready",
                    "Dr. " + doctor.getUser().getFullName()
                            + " has generated a prescription for you (Ref: "
                            + presc.getPrescriptionId() + "). It has been sent to the pharmacy."
            );

            // Singleton: audit
            auditLogService.logAction(
                    doctor.getUser().getUserId(),
                    "GENERATE_PRESCRIPTION",
                    "prescriptions",
                    presc.getPrescriptionId(),
                    "E-Prescription " + presc.getPrescriptionId()
                            + " generated for patient " + patient.getUser().getFullName()
                            + " by Dr. " + doctor.getUser().getFullName()
            );

            return ResponseEntity.ok(toMap(presc));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    // ─── Dispense — Pharmacy marks prescription as dispensed ─────────────

    @PutMapping("/{id}/dispense")
    public ResponseEntity<?> dispense(
            @PathVariable String id,
            @RequestHeader(value = "X-User-Role", required = false) String role) {

        ResponseEntity<?> denied = RoleGuard.requireRole(role, "ADMINISTRATOR", "STAFF");
        if (denied != null) return denied;

        return prescriptionRepo.findById(id).map(presc -> {
            presc.setStatus(Prescription.PrescriptionStatus.DISPENSED);
            prescriptionRepo.save(presc);

            auditLogService.logAction(null, "DISPENSE_PRESCRIPTION",
                    "prescriptions", id,
                    "Prescription " + id + " dispensed by pharmacy.");

            return ResponseEntity.ok(toMap(presc));
        }).orElse(ResponseEntity.notFound().build());
    }

    // ─── DTO mapper ──────────────────────────────────────────────────────

    private Map<String, Object> toMap(Prescription p) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("prescriptionId", p.getPrescriptionId());
        m.put("recordId", p.getRecord().getRecordId());
        m.put("doctorId", p.getDoctor().getDoctorId());
        m.put("doctorName", p.getDoctor().getUser().getFullName());
        m.put("patientId", p.getPatient().getPatientId());
        m.put("patientName", p.getPatient().getUser().getFullName());
        m.put("issuedDate", p.getIssuedDate() != null ? p.getIssuedDate().toString() : null);
        m.put("status", p.getStatus().name());
        return m;
    }
}
