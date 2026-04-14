package com.healthsync.controller;

import com.healthsync.factory.HealthSyncFactory;
import com.healthsync.model.*;
import com.healthsync.repository.*;
import com.healthsync.singleton.AuditLogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * MedicalRecordController — UC3: Manage Medical Records (EMR)
 *
 * PATTERNS USED:
 *   Factory Method (Creational #1): HealthSyncFactory.createMedicalRecord()
 *     — removes inline entity construction from the controller.
 *   Singleton (Creational #2): AuditLogService — single instance audits
 *     every EMR creation and update across the application.
 *
 * SOLID SRP: this controller ONLY handles medical-record HTTP endpoints.
 * GRASP Low Coupling: no raw "new MedicalRecord()" inside the controller.
 * GRASP Information Expert: MedicalRecord.getStatus() drives state badge.
 */
@RestController
@RequestMapping("/api/records")
@CrossOrigin(origins = "*")
public class MedicalRecordController {

    @Autowired private MedicalRecordRepository recordRepo;
    @Autowired private PatientRepository patientRepo;
    @Autowired private DoctorRepository doctorRepo;
    @Autowired private AppointmentRepository apptRepo;

    /** Singleton (Creational #2) */
    @Autowired private AuditLogService auditLogService;

    // ─── Read endpoints ──────────────────────────────────────────────────

    @GetMapping
    public List<Map<String, Object>> getAll() {
        return recordRepo.findAll().stream().map(this::toMap).toList();
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getById(@PathVariable String id) {
        return recordRepo.findById(id)
                .map(r -> ResponseEntity.ok(toMap(r)))
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/patient/{patientId}")
    public List<Map<String, Object>> byPatient(@PathVariable String patientId) {
        return recordRepo.findByPatientPatientId(patientId).stream().map(this::toMap).toList();
    }

    @GetMapping("/doctor/{doctorId}")
    public List<Map<String, Object>> byDoctor(@PathVariable String doctorId) {
        return recordRepo.findByDoctorDoctorId(doctorId).stream().map(this::toMap).toList();
    }

    // ─── Create — UC3 Activity Diagram: Doctor creates EMR ───────────────

    @PostMapping
    public ResponseEntity<?> create(@RequestBody Map<String, String> body) {
        try {
            Patient patient = patientRepo.findById(body.get("patientId")).orElse(null);
            Doctor doctor = doctorRepo.findById(body.get("doctorId")).orElse(null);
            if (patient == null || doctor == null)
                return ResponseEntity.badRequest().body(Map.of("error", "Invalid patient or doctor"));

            int nextId = recordRepo.findMaxRecordId() + 1;

            // Factory Method (Creational #1): centralised EMR construction
            MedicalRecord rec = HealthSyncFactory.createMedicalRecord(nextId, patient, doctor, body);

            if (body.containsKey("appointmentId"))
                apptRepo.findById(body.get("appointmentId")).ifPresent(rec::setAppointment);

            recordRepo.save(rec);

            // Singleton (Creational #2): audit EMR creation — UC3, NFR-06
            auditLogService.logAction(
                    doctor.getUser().getUserId(),
                    "CREATE_EMR",
                    "medical_records",
                    rec.getRecordId(),
                    "EMR created for patient " + patient.getUser().getFullName()
                            + " by Dr. " + doctor.getUser().getFullName()
            );

            return ResponseEntity.ok(toMap(rec));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    // ─── Update — UC3: Doctor updates diagnosis / prescription ───────────

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable String id,
                                     @RequestBody Map<String, String> body) {
        return recordRepo.findById(id).map(rec -> {
            if (body.containsKey("diagnosisNotes"))
                rec.setDiagnosisNotes(body.get("diagnosisNotes"));
            if (body.containsKey("labResults"))
                rec.setLabResults(body.get("labResults"));
            if (body.containsKey("prescriptionDetails"))
                rec.setPrescriptionDetails(body.get("prescriptionDetails"));
            if (body.containsKey("status"))
                rec.setStatus(MedicalRecord.RecordStatus.valueOf(body.get("status")));
            recordRepo.save(rec);

            // Singleton: audit update
            auditLogService.logAction(
                    null, "UPDATE_EMR",
                    "medical_records", id,
                    "EMR " + id + " updated — status: " + rec.getStatus()
            );

            return ResponseEntity.ok(toMap(rec));
        }).orElse(ResponseEntity.notFound().build());
    }

    // ─── DTO mapper ──────────────────────────────────────────────────────

    private Map<String, Object> toMap(MedicalRecord r) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("recordId", r.getRecordId());
        m.put("patientId", r.getPatient().getPatientId());
        m.put("patientName", r.getPatient().getUser().getFullName());
        m.put("doctorId", r.getDoctor().getDoctorId());
        m.put("doctorName", r.getDoctor().getUser().getFullName());
        m.put("diagnosisNotes", r.getDiagnosisNotes());
        m.put("labResults", r.getLabResults());
        m.put("prescriptionDetails", r.getPrescriptionDetails());
        m.put("status", r.getStatus().name());
        m.put("dateCreated", r.getDateCreated() != null ? r.getDateCreated().toString() : null);
        m.put("lastUpdated", r.getLastUpdated() != null ? r.getLastUpdated().toString() : null);
        if (r.getAppointment() != null)
            m.put("appointmentId", r.getAppointment().getAppointmentId());
        return m;
    }
}
