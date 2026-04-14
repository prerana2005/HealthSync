package com.healthsync.controller;

import com.healthsync.factory.HealthSyncFactory;
import com.healthsync.model.*;
import com.healthsync.repository.*;
import com.healthsync.singleton.AuditLogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;

/**
 * AuthController — UC1: Patient Registration & Profile Management
 *                  UC5 (minor): Role-Based Authentication (FR-05)
 *
 * PATTERNS USED:
 *   Factory Method (Creational #1): HealthSyncFactory.createUser()
 *     and createPatient() — removes inline "new Entity()" construction.
 *   Singleton (Creational #2): AuditLogService — same instance audits
 *     every login and registration event across the application.
 *
 * SOLID SRP: AuthController only handles authentication HTTP endpoints.
 * GRASP Low Coupling: object creation delegated to HealthSyncFactory.
 */
@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*")
public class AuthController {

    @Autowired private UserRepository userRepo;
    @Autowired private PatientRepository patientRepo;
    @Autowired private DoctorRepository doctorRepo;

    /** Singleton (Creational #2) */
    @Autowired private AuditLogService auditLogService;

    // ─── Login ───────────────────────────────────────────────────────────

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

            if (user.getRoleType() == User.RoleType.PATIENT) {
                patientRepo.findByUserUserId(user.getUserId())
                        .ifPresent(p -> response.put("patientId", p.getPatientId()));
            } else if (user.getRoleType() == User.RoleType.DOCTOR) {
                doctorRepo.findByUserUserId(user.getUserId())
                        .ifPresent(d -> response.put("doctorId", d.getDoctorId()));
            }

            // Singleton audit — FR-05 audit logging (NFR-06)
            auditLogService.logAction(user.getUserId(), "LOGIN",
                    "users", user.getUserId(),
                    user.getFullName() + " logged in as " + user.getRoleType());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    // ─── Register — UC1: Patient Registration ────────────────────────────

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody Map<String, String> body) {
        try {
            if (userRepo.findByEmail(body.get("email")).isPresent()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Email already registered"));
            }

            int nextUserId = userRepo.findMaxUserId() + 1;

            // Factory Method (Creational #1): centralised User construction
            User user = HealthSyncFactory.createUser(
                    nextUserId,
                    body.get("fullName"),
                    body.get("email"),
                    body.get("phone"),
                    sha256(body.get("password")),
                    User.RoleType.PATIENT
            );
            userRepo.save(user);

            int nextPatId = patientRepo.findMaxPatientId() + 1;

            // Factory Method (Creational #1): centralised Patient construction
            Patient patient = HealthSyncFactory.createPatient(
                    nextPatId,
                    user,
                    body.getOrDefault("bloodGroup", ""),
                    body.getOrDefault("emergencyContact", "")
            );
            patientRepo.save(patient);

            // Singleton (Creational #2): audit registration — NFR-06
            auditLogService.logAction(
                    user.getUserId(),
                    "REGISTER_PATIENT",
                    "patients",
                    patient.getPatientId(),
                    "New patient registered: " + user.getFullName() + " (" + user.getEmail() + ")"
            );

            return ResponseEntity.ok(Map.of(
                    "message", "Registration successful",
                    "userId", user.getUserId(),
                    "patientId", patient.getPatientId()
            ));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    // ─── Helper ──────────────────────────────────────────────────────────

    private String sha256(String input) throws Exception {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        byte[] hash = md.digest(input.getBytes(StandardCharsets.UTF_8));
        StringBuilder sb = new StringBuilder();
        for (byte b : hash) sb.append(String.format("%02x", b));
        return sb.toString();
    }
}
