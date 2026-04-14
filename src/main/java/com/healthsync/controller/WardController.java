package com.healthsync.controller;

import com.healthsync.model.*;
import com.healthsync.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * WardController — REST controller for Ward Allotment (Use Case: Ward Allotment).
 *
 * Present in the use case diagram but previously missing from implementation.
 * Satisfies GRASP Controller principle — this class handles HTTP requests
 * for the Ward Allotment use case and delegates to repositories.
 *
 * SOLID — SRP: This controller ONLY handles ward/allotment HTTP concerns.
 */
@RestController
@RequestMapping("/api/wards")
@CrossOrigin(origins = "*")
public class WardController {

    @Autowired private WardRepository wardRepo;
    @Autowired private WardAllotmentRepository allotmentRepo;
    @Autowired private PatientRepository patientRepo;

    // ─── Ward endpoints ──────────────────────────────────────────────────

    @GetMapping
    public List<Map<String, Object>> getAllWards() {
        return wardRepo.findAll().stream().map(this::wardToMap).toList();
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getWard(@PathVariable String id) {
        return wardRepo.findById(id)
                .map(w -> ResponseEntity.ok(wardToMap(w)))
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/available")
    public List<Map<String, Object>> availableWards() {
        return wardRepo.findByAvailableBedsGreaterThan(0)
                .stream().map(this::wardToMap).toList();
    }

    @PostMapping
    public ResponseEntity<?> createWard(@RequestBody Map<String, String> body) {
        try {
            Ward ward = new Ward();
            ward.setWardId(body.get("wardId"));
            ward.setWardName(body.get("wardName"));
            ward.setWardType(Ward.WardType.valueOf(body.get("wardType")));
            ward.setTotalBeds(Integer.parseInt(body.getOrDefault("totalBeds", "10")));
            ward.setAvailableBeds(Integer.parseInt(body.getOrDefault("availableBeds", "10")));
            ward.setFloorNumber(Integer.parseInt(body.getOrDefault("floorNumber", "1")));
            wardRepo.save(ward);
            return ResponseEntity.ok(wardToMap(ward));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    // ─── Allotment endpoints ─────────────────────────────────────────────

    @GetMapping("/allotments")
    public List<Map<String, Object>> getAllAllotments() {
        return allotmentRepo.findAll().stream().map(this::allotmentToMap).toList();
    }

    @GetMapping("/allotments/patient/{patientId}")
    public List<Map<String, Object>> allotmentsByPatient(@PathVariable String patientId) {
        return allotmentRepo.findByPatientPatientId(patientId)
                .stream().map(this::allotmentToMap).toList();
    }

    /**
     * Allot a bed — corresponds to "Ward Allotment" in the use case diagram.
     * State: Ward.availableBeds decrements; WardAllotment status = ACTIVE.
     */
    @PostMapping("/allotments")
    public ResponseEntity<?> allotBed(@RequestBody Map<String, String> body) {
        try {
            String patientId = body.get("patientId");
            String wardId    = body.get("wardId");

            Patient patient = patientRepo.findById(patientId)
                    .orElseThrow(() -> new IllegalArgumentException("Patient not found"));
            Ward ward = wardRepo.findById(wardId)
                    .orElseThrow(() -> new IllegalArgumentException("Ward not found"));

            if (!ward.checkAvailability()) {
                return ResponseEntity.badRequest().body(Map.of("error", "No beds available in this ward"));
            }

            WardAllotment allotment = new WardAllotment();
            allotment.setPatient(patient);
            allotment.setWard(ward);
            allotment.setBedNumber(body.getOrDefault("bedNumber", "TBD"));
            allotment.setIsICU(Boolean.parseBoolean(body.getOrDefault("isICU", "false")));
            allotment.setNurseAssigned(body.getOrDefault("nurseAssigned", ""));
            allotment.setStatus(WardAllotment.AllotmentStatus.ACTIVE);

            // Ward tracks its own bed count (Information Expert / GRASP)
            ward.allotBed();
            wardRepo.save(ward);
            allotmentRepo.save(allotment);

            return ResponseEntity.ok(allotmentToMap(allotment));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Discharge a patient — state transition in WardAllotment state diagram.
     * Status: ACTIVE → DISCHARGED; Ward.availableBeds increments.
     */
    @PutMapping("/allotments/{id}/discharge")
    public ResponseEntity<?> discharge(@PathVariable Integer id) {
        return allotmentRepo.findById(id).map(allotment -> {
            allotment.discharge();        // domain method handles state + ward.freeBed()
            wardRepo.save(allotment.getWard());
            allotmentRepo.save(allotment);
            return ResponseEntity.ok(allotmentToMap(allotment));
        }).orElse(ResponseEntity.notFound().build());
    }

    // ─── DTO mappers ─────────────────────────────────────────────────────

    private Map<String, Object> wardToMap(Ward w) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("wardId", w.getWardId());
        m.put("wardName", w.getWardName());
        m.put("wardType", w.getWardType().name());
        m.put("totalBeds", w.getTotalBeds());
        m.put("availableBeds", w.getAvailableBeds());
        m.put("floorNumber", w.getFloorNumber());
        m.put("available", w.checkAvailability());
        return m;
    }

    private Map<String, Object> allotmentToMap(WardAllotment a) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("allotmentId", a.getAllotmentId());
        m.put("patientId", a.getPatient().getPatientId());
        m.put("patientName", a.getPatient().getUser().getFullName());
        m.put("wardId", a.getWard().getWardId());
        m.put("wardName", a.getWard().getWardName());
        m.put("bedNumber", a.getBedNumber());
        m.put("isICU", a.getIsICU());
        m.put("nurseAssigned", a.getNurseAssigned());
        m.put("allotmentDate", a.getAllotmentDate() != null ? a.getAllotmentDate().toString() : null);
        m.put("dischargeDate", a.getDischargeDate() != null ? a.getDischargeDate().toString() : null);
        m.put("status", a.getStatus().name());
        return m;
    }
}
