package com.healthsync.controller;

import com.healthsync.config.RoleGuard;
import com.healthsync.model.*;
import com.healthsync.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/patients")
@CrossOrigin(origins = "*")
public class PatientController {

    @Autowired private PatientRepository patientRepo;

    @GetMapping
    public ResponseEntity<?> getAll(@RequestHeader(value = "X-User-Role", required = false) String role) {
        ResponseEntity<?> denied = RoleGuard.requireRole(role, "ADMINISTRATOR", "DOCTOR", "STAFF");
        if (denied != null) return denied;
        return ResponseEntity.ok(patientRepo.findAll().stream().map(this::toMap).toList());
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getById(@PathVariable String id) {
        return patientRepo.findById(id)
            .map(p -> ResponseEntity.ok(toMap(p)))
            .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/search")
    public List<Map<String, Object>> search(@RequestParam String q) {
        return patientRepo.search(q).stream().map(this::toMap).toList();
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable String id, @RequestBody Map<String, String> body) {
        return patientRepo.findById(id).map(p -> {
            if (body.containsKey("medicalHistory")) p.setMedicalHistory(body.get("medicalHistory"));
            if (body.containsKey("currentAilment")) p.setCurrentAilment(body.get("currentAilment"));
            if (body.containsKey("emergencyContact")) p.setEmergencyContact(body.get("emergencyContact"));
            if (body.containsKey("bloodGroup")) p.setBloodGroup(body.get("bloodGroup"));
            if (body.containsKey("address")) p.setAddress(body.get("address"));
            if (body.containsKey("fullName")) p.getUser().setFullName(body.get("fullName"));
            if (body.containsKey("phone")) p.getUser().setPhone(body.get("phone"));
            patientRepo.save(p);
            return ResponseEntity.ok(toMap(p));
        }).orElse(ResponseEntity.notFound().build());
    }

    private Map<String, Object> toMap(Patient p) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("patientId", p.getPatientId());
        m.put("userId", p.getUser().getUserId());
        m.put("fullName", p.getUser().getFullName());
        m.put("email", p.getUser().getEmail());
        m.put("phone", p.getUser().getPhone());
        m.put("bloodGroup", p.getBloodGroup());
        m.put("dateOfBirth", p.getDateOfBirth());
        m.put("medicalHistory", p.getMedicalHistory());
        m.put("currentAilment", p.getCurrentAilment());
        m.put("emergencyContact", p.getEmergencyContact());
        m.put("address", p.getAddress());
        return m;
    }
}
