package com.healthsync.controller;

import com.healthsync.model.*;
import com.healthsync.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/records")
@CrossOrigin(origins = "*")
public class MedicalRecordController {

    @Autowired private MedicalRecordRepository recordRepo;
    @Autowired private PatientRepository patientRepo;
    @Autowired private DoctorRepository doctorRepo;
    @Autowired private AppointmentRepository apptRepo;

    @GetMapping
    public List<Map<String, Object>> getAll() {
        return recordRepo.findAll().stream().map(this::toMap).toList();
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getById(@PathVariable String id) {
        return recordRepo.findById(id).map(r -> ResponseEntity.ok(toMap(r)))
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

    @PostMapping
    public ResponseEntity<?> create(@RequestBody Map<String, String> body) {
        try {
            Patient patient = patientRepo.findById(body.get("patientId")).orElse(null);
            Doctor doctor = doctorRepo.findById(body.get("doctorId")).orElse(null);
            if (patient == null || doctor == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "Invalid patient or doctor"));
            }

            int nextId = recordRepo.findMaxRecordId() + 1;
            MedicalRecord rec = new MedicalRecord();
            rec.setRecordId(String.format("REC%05d", nextId));
            rec.setPatient(patient);
            rec.setDoctor(doctor);
            rec.setDiagnosisNotes(body.getOrDefault("diagnosisNotes", ""));
            rec.setLabResults(body.getOrDefault("labResults", ""));
            rec.setPrescriptionDetails(body.getOrDefault("prescriptionDetails", ""));
            rec.setStatus(MedicalRecord.RecordStatus.OPEN);

            if (body.containsKey("appointmentId")) {
                apptRepo.findById(body.get("appointmentId")).ifPresent(rec::setAppointment);
            }

            recordRepo.save(rec);
            return ResponseEntity.ok(toMap(rec));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable String id, @RequestBody Map<String, String> body) {
        return recordRepo.findById(id).map(rec -> {
            if (body.containsKey("diagnosisNotes")) rec.setDiagnosisNotes(body.get("diagnosisNotes"));
            if (body.containsKey("labResults")) rec.setLabResults(body.get("labResults"));
            if (body.containsKey("prescriptionDetails")) rec.setPrescriptionDetails(body.get("prescriptionDetails"));
            if (body.containsKey("status")) rec.setStatus(MedicalRecord.RecordStatus.valueOf(body.get("status")));
            recordRepo.save(rec);
            return ResponseEntity.ok(toMap(rec));
        }).orElse(ResponseEntity.notFound().build());
    }

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
        if (r.getAppointment() != null) m.put("appointmentId", r.getAppointment().getAppointmentId());
        return m;
    }
}
