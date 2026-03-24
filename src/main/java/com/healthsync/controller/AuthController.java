package com.healthsync.controller;

import com.healthsync.model.*;
import com.healthsync.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*")
public class AuthController {

    @Autowired private UserRepository userRepo;
    @Autowired private PatientRepository patientRepo;
    @Autowired private DoctorRepository doctorRepo;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> body) {
        try {
            String email = body.get("email");
            String password = body.get("password");
            String hash = sha256(password);

            Optional<User> userOpt = userRepo.findByEmailAndPasswordHash(email, hash);
            if (userOpt.isEmpty()) {
                return ResponseEntity.status(401).body(Map.of("error", "Invalid credentials"));
            }

            User user = userOpt.get();
            Map<String, Object> response = new LinkedHashMap<>();
            response.put("userId", user.getUserId());
            response.put("fullName", user.getFullName());
            response.put("email", user.getEmail());
            response.put("role", user.getRoleType().name());

            // Include patient/doctor ID if applicable
            if (user.getRoleType() == User.RoleType.PATIENT) {
                patientRepo.findByUserUserId(user.getUserId())
                    .ifPresent(p -> response.put("patientId", p.getPatientId()));
            } else if (user.getRoleType() == User.RoleType.DOCTOR) {
                doctorRepo.findByUserUserId(user.getUserId())
                    .ifPresent(d -> response.put("doctorId", d.getDoctorId()));
            }

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody Map<String, String> body) {
        try {
            if (userRepo.findByEmail(body.get("email")).isPresent()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Email already registered"));
            }

            int nextId = userRepo.findMaxUserId() + 1;
            String userId = String.format("USR%05d", nextId);

            User user = new User();
            user.setUserId(userId);
            user.setFullName(body.get("fullName"));
            user.setEmail(body.get("email"));
            user.setPhone(body.get("phone"));
            user.setPasswordHash(sha256(body.get("password")));
            user.setRoleType(User.RoleType.PATIENT);
            userRepo.save(user);

            // Create patient record
            int nextPatId = patientRepo.findMaxPatientId() + 1;
            Patient patient = new Patient();
            patient.setPatientId(String.format("PAT%05d", nextPatId));
            patient.setUser(user);
            patient.setBloodGroup(body.getOrDefault("bloodGroup", ""));
            patient.setEmergencyContact(body.getOrDefault("emergencyContact", ""));
            patientRepo.save(patient);

            return ResponseEntity.ok(Map.of("message", "Registration successful", "userId", userId, "patientId", patient.getPatientId()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    private String sha256(String input) throws Exception {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        byte[] hash = md.digest(input.getBytes(StandardCharsets.UTF_8));
        StringBuilder sb = new StringBuilder();
        for (byte b : hash) sb.append(String.format("%02x", b));
        return sb.toString();
    }
}
