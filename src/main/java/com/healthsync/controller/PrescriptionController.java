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
 * UC3 Activity Diagram flow (from project diagrams):
 *   Doctor → Write Prescription → Staff/Nurse → Send Prescription to Pharmacy → patient notified
 *
 * Two separate prescription concepts co-exist intentionally:
 *   1. MedicalRecord.prescriptionDetails  — free-text doctor notes ("Tab. Aspirin 75mg once daily")
 *      Entered inside the Create Medical Record modal. Quick clinical note. No formal ID.
 *   2. Prescription entity (this controller) — formal E-Prescription object with unique ID (RX…),
 *      PENDING → DISPENSED lifecycle, linked to MedicalRecord, triggers Observer notification.
 *      Created via the 💊 Rx button on the Medical Records table, or via Prescriptions page.
 *
 * Who sees what on the Prescriptions page:
 *   DOCTOR        → "My Generated Prescriptions" table (all Rx they wrote, any status)
 *   STAFF / ADMIN → "Pending Dispensing" table (all PENDING Rx, Dispense button)
 *   PATIENT       → "My Prescriptions" table (all Rx written for them)
 *
 * PATTERNS USED:
 *   Observer  (Behavioral): notificationObserver.onEvent() — notifies patient on generation
 *             (MedicalRecord state: PrescriptionAdded → do/generatePrescription() → do/sendToPharmacy())
 *   Singleton (Creational #2): AuditLogService — single instance audits every Rx event.
 *
 * SOLID SRP  : Only handles prescription HTTP concerns.
 * GRASP Low Coupling : No direct dependency on notification or audit tables in callers.
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

    // ─── Read endpoints ──────────────────────────────────────────────────────

    /**
     * PATIENT view — "My Prescriptions" table.
     * Shows all prescriptions written for this patient, any status.
     */
    @GetMapping("/patient/{patientId}")
    public ResponseEntity<?> byPatient(@PathVariable String patientId) {
        return ResponseEntity.ok(
                prescriptionRepo.findByPatientPatientId(patientId)
                        .stream().map(this::toMap).toList()
        );
    }

    /**
     * DOCTOR view — "My Generated Prescriptions" table.
     * Shows all prescriptions this doctor has generated, any status.
     * This fixes the blank page a doctor sees on the Prescriptions screen.
     */
    @GetMapping("/doctor/{doctorId}")
    public ResponseEntity<?> byDoctor(@PathVariable String doctorId) {
        return ResponseEntity.ok(
                prescriptionRepo.findByDoctorDoctorId(doctorId)
                        .stream().map(this::toMap).toList()
        );
    }

    /**
     * STAFF / ADMIN view — "Pending Dispensing" table.
     * Shows only PENDING prescriptions for the pharmacy dispensing queue.
     * RoleGuard: STAFF and ADMINISTRATOR only.
     */
    @GetMapping("/pending")
    public ResponseEntity<?> pending(
            @RequestHeader(value = "X-User-Role", required = false) String role) {
        ResponseEntity<?> denied = RoleGuard.requireRole(role, "ADMINISTRATOR", "STAFF");
        if (denied != null) return denied;
        return ResponseEntity.ok(
                prescriptionRepo.findByStatus(Prescription.PrescriptionStatus.PENDING)
                        .stream().map(this::toMap).toList()
        );
    }

    // ─── Create — UC3: Doctor generates e-prescription ──────────────────────

    /**
     * Called by:
     *   - 💊 Rx button on Medical Records table row → pre-fills recordId + patientId
     *   - "+ Generate Prescription" button on Prescriptions page → manual entry
     *
     * State Diagram (MedicalRecord): AddingNotes
     *   → writePrescription() [confirmed] → PrescriptionAdded
     *   → do/generatePrescription() → do/sendToPharmacy()
     *
     * After creation:
     *   - Observer fires PRESCRIPTION notification to patient
     *   - Singleton AuditLogService logs the action
     *   - MedicalRecord state implicitly moves to UPDATED (doctor should update it separately)
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
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Invalid record ID, doctor ID, or patient ID. "
                                + "Make sure the record exists and belongs to this patient."));
            }

            int nextId = prescriptionRepo.findMaxPrescriptionId() + 1;
            Prescription presc = new Prescription();
            presc.setPrescriptionId(String.format("RX%08d", nextId));
            presc.setRecord(record);
            presc.setDoctor(doctor);
            presc.setPatient(patient);
            presc.setStatus(Prescription.PrescriptionStatus.PENDING);
            prescriptionRepo.save(presc);

            // Observer (Behavioral): notify patient — state PrescriptionAdded → sendToPharmacy()
            notificationObserver.onEvent(
                    patient.getUser(),
                    Notification.NotifType.PRESCRIPTION,
                    "E-Prescription Ready",
                    "Dr. " + doctor.getUser().getFullName()
                            + " has generated a prescription for you (Ref: "
                            + presc.getPrescriptionId() + "). It has been sent to the pharmacy."
            );

            // Singleton (Creational #2): audit
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

    // ─── Dispense — Staff marks prescription as dispensed ────────────────────

    /**
     * Called from the "Dispense" button in the Staff/Admin Pending Dispensing table.
     * Transitions Prescription: PENDING → DISPENSED.
     * RoleGuard: STAFF and ADMINISTRATOR only (pharmacy staff action).
     */
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
                    "Prescription " + id + " dispensed by pharmacy staff.");

            return ResponseEntity.ok(toMap(presc));
        }).orElse(ResponseEntity.notFound().build());
    }

    // ─── DTO mapper ──────────────────────────────────────────────────────────

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
