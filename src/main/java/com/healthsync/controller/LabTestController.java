package com.healthsync.controller;

import com.healthsync.config.RoleGuard;
import com.healthsync.model.*;
import com.healthsync.observer.NotificationObserver;
import com.healthsync.repository.*;
import com.healthsync.singleton.AuditLogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;

/**
 * LabTestController — FR-06: Lab Report Notifications
 *
 * Closes the gap: LabTest.notificationSent existed but nothing ever called
 * the NotificationObserver when a test was completed.
 *
 * State Diagram UC3 (MedicalRecord — State Diagram):
 *   AddingNotes → orderLabTest() → LabPending
 *                               → do/notifyLab() + do/awaitResults()
 *   LabPending  → resultsReceived() → AddingNotes (doctor reviews)
 *
 * PATTERNS USED:
 *   Observer  (Behavioral): onEvent() fired when test COMPLETED — notifies patient (FR-06).
 *   Singleton (Creational #2): AuditLogService — one instance across entire app.
 *
 * SOLID SRP: only handles lab-test HTTP concerns.
 * GRASP Information Expert: LabTest knows its own status + notificationSent flag.
 */
@RestController
@RequestMapping("/api/lab-tests")
@CrossOrigin(origins = "*")
public class LabTestController {

    @Autowired private LabTestRepository labTestRepo;
    @Autowired private MedicalRecordRepository recordRepo;
    @Autowired private DoctorRepository doctorRepo;

    /** Singleton (Creational #2) */
    @Autowired private AuditLogService auditLogService;

    /** Observer (Behavioral) — decoupled lab-result notification */
    @Autowired private NotificationObserver notificationObserver;

    // ─── Read ────────────────────────────────────────────────────────────

    @GetMapping("/record/{recordId}")
    public List<Map<String, Object>> byRecord(@PathVariable String recordId) {
        return labTestRepo.findByRecordRecordId(recordId)
                .stream().map(this::toMap).toList();
    }

    @GetMapping("/pending")
    public ResponseEntity<?> pending(@RequestHeader(value = "X-User-Role", required = false) String role) {
        ResponseEntity<?> denied = RoleGuard.requireRole(role, "ADMINISTRATOR", "STAFF", "DOCTOR");
        if (denied != null) return denied;
        return ResponseEntity.ok(
                labTestRepo.findByStatus(LabTest.LabTestStatus.ORDERED)
                        .stream().map(this::toMap).toList()
        );
    }

    // ─── Order a lab test — UC3 Activity: [Lab Test Required] branch ────

    @PostMapping
    public ResponseEntity<?> order(
            @RequestHeader(value = "X-User-Role", required = false) String role,
            @RequestBody Map<String, String> body) {

        ResponseEntity<?> denied = RoleGuard.requireRole(role, "DOCTOR", "ADMINISTRATOR");
        if (denied != null) return denied;

        try {
            MedicalRecord record = recordRepo.findById(body.get("recordId")).orElse(null);
            Doctor doctor = doctorRepo.findById(body.get("doctorId")).orElse(null);
            if (record == null || doctor == null)
                return ResponseEntity.badRequest().body(Map.of("error", "Invalid record or doctor"));

            LabTest test = new LabTest();
            test.setRecord(record);
            test.setTestName(body.get("testName"));
            test.setOrderedBy(doctor);
            test.setStatus(LabTest.LabTestStatus.ORDERED);
            test.setNotificationSent(false);
            labTestRepo.save(test);

            auditLogService.logAction(
                    doctor.getUser().getUserId(), "ORDER_LAB_TEST",
                    "lab_tests", String.valueOf(test.getTestId()),
                    "Lab test '" + test.getTestName() + "' ordered for record " + record.getRecordId()
            );

            return ResponseEntity.ok(toMap(test));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    // ─── Complete a lab test — triggers FR-06 notification ───────────────

    /**
     * State Diagram: LabPending → resultsReceived() → notifyLab()
     * This is the key missing piece: fires the Observer when results arrive.
     */
    @PutMapping("/{id}/complete")
    public ResponseEntity<?> complete(
            @PathVariable Integer id,
            @RequestHeader(value = "X-User-Role", required = false) String role,
            @RequestBody Map<String, String> body) {

        ResponseEntity<?> denied = RoleGuard.requireRole(role, "ADMINISTRATOR", "STAFF", "DOCTOR");
        if (denied != null) return denied;

        return labTestRepo.findById(id).map(test -> {
            test.setResultSummary(body.getOrDefault("resultSummary", ""));
            test.setResultDate(LocalDateTime.now());
            test.setStatus(LabTest.LabTestStatus.COMPLETED);

            // Observer: notify patient — FR-06 Lab Report Notifications
            // Only send once (notificationSent flag prevents duplicates)
            if (!Boolean.TRUE.equals(test.getNotificationSent())) {
                User patient = test.getRecord().getPatient().getUser();
                notificationObserver.onEvent(
                        patient,
                        Notification.NotifType.LAB_REPORT,
                        "Lab Results Ready — " + test.getTestName(),
                        "Your lab test '" + test.getTestName()
                                + "' results are now available. Please consult your doctor."
                );
                test.setNotificationSent(true);
            }

            labTestRepo.save(test);

            // Singleton: audit
            auditLogService.logAction(
                    null, "LAB_TEST_COMPLETED",
                    "lab_tests", String.valueOf(id),
                    "Lab test '" + test.getTestName() + "' completed for record "
                            + test.getRecord().getRecordId()
            );

            return ResponseEntity.ok(toMap(test));
        }).orElse(ResponseEntity.notFound().build());
    }

    // ─── DTO mapper ──────────────────────────────────────────────────────

    private Map<String, Object> toMap(LabTest t) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("testId", t.getTestId());
        m.put("recordId", t.getRecord().getRecordId());
        m.put("patientName", t.getRecord().getPatient().getUser().getFullName());
        m.put("testName", t.getTestName());
        m.put("orderedBy", t.getOrderedBy().getUser().getFullName());
        m.put("orderedDate", t.getOrderedDate() != null ? t.getOrderedDate().toString() : null);
        m.put("resultSummary", t.getResultSummary());
        m.put("resultDate", t.getResultDate() != null ? t.getResultDate().toString() : null);
        m.put("status", t.getStatus().name());
        m.put("notificationSent", t.getNotificationSent());
        return m;
    }
}
