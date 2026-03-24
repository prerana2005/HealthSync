package com.healthsync.controller;

import com.healthsync.model.*;
import com.healthsync.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/doctors")
@CrossOrigin(origins = "*")
public class DoctorController {

    @Autowired private DoctorRepository doctorRepo;
    @Autowired private DepartmentRepository deptRepo;

    @GetMapping
    public List<Map<String, Object>> getAll() {
        return doctorRepo.findAll().stream().map(this::toMap).toList();
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getById(@PathVariable String id) {
        return doctorRepo.findById(id)
            .map(d -> ResponseEntity.ok(toMap(d)))
            .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/search")
    public List<Map<String, Object>> search(@RequestParam String q) {
        return doctorRepo.search(q).stream().map(this::toMap).toList();
    }

    @GetMapping("/department/{deptId}")
    public List<Map<String, Object>> byDepartment(@PathVariable String deptId) {
        return doctorRepo.findByDepartmentDeptId(deptId).stream().map(this::toMap).toList();
    }

    @GetMapping("/available")
    public List<Map<String, Object>> available() {
        return doctorRepo.findByAvailabilityStatus(Doctor.AvailabilityStatus.AVAILABLE)
                .stream().map(this::toMap).toList();
    }

    @GetMapping("/departments")
    public List<Map<String, Object>> getDepartments() {
        return deptRepo.findAll().stream().map(d -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("deptId", d.getDeptId());
            m.put("deptName", d.getDeptName());
            if (d.getHod() != null) m.put("hodName", d.getHod().getFullName());
            return m;
        }).toList();
    }

    private Map<String, Object> toMap(Doctor d) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("doctorId", d.getDoctorId());
        m.put("userId", d.getUser().getUserId());
        m.put("fullName", d.getUser().getFullName());
        m.put("email", d.getUser().getEmail());
        m.put("specialization", d.getSpecialization());
        m.put("department", d.getDepartment().getDeptName());
        m.put("deptId", d.getDepartment().getDeptId());
        m.put("consultationFee", d.getConsultationFee());
        m.put("availability", d.getAvailabilityStatus().name());
        m.put("qualification", d.getQualification());
        m.put("experienceYears", d.getExperienceYears());
        return m;
    }
}
