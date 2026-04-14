package com.healthsync.singleton;

import com.healthsync.model.AuditLog;
import com.healthsync.repository.AuditLogRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * AuditLogService — Creational Design Pattern: SINGLETON
 * =====================================================================
 *
 * PATTERN: Singleton (Creational) — 2nd Creational pattern
 *
 * INTENT: Ensure that a class has only ONE instance throughout the
 * application lifetime, and provide a global point of access to it.
 *
 * WHY SINGLETON HERE:
 * The AuditLog tracks every user action (login, invoice generated,
 * appointment booked, ward allotted) across the entire system.
 * If multiple instances of this service existed:
 *   1. Log entries could be written in non-sequential order.
 *   2. In-memory buffers could diverge — log entries lost.
 *   3. Different controllers getting different instances breaks
 *      the "single source of truth" for audit data.
 *
 * HOW IT IS IMPLEMENTED:
 * Spring's @Component with default scope is SINGLETON by default —
 * Spring container creates exactly one bean and injects the SAME
 * instance everywhere. This is the idiomatic Spring Singleton.
 * The @PostConstruct init() method runs exactly once at startup,
 * confirming single instantiation.
 *
 * USE CASE MAPPING:
 * UC4 — Billing & Insurance: every payment processed is audited.
 * UC2 — Appointment Scheduling: every booking/cancellation is logged.
 * UC1 — Patient Registration: new patient creation is logged.
 * UC3 — EMR: record creation and updates are logged.
 *
 * SOLID: SRP — AuditLogService only writes audit logs.
 * SOLID: OCP — new action types (strings) extend without code change.
 * GRASP: Information Expert — knows how to log any system event.
 * GRASP: Low Coupling — callers just call logAction(); they do not
 *        know about AuditLogRepository or AuditLog entity.
 * =====================================================================
 */
@Component  // Spring-managed Singleton — one instance for entire app
public class AuditLogService {

    @Autowired
    private AuditLogRepository auditLogRepo;

    // Tracks how many times this bean was constructed — should always be 1
    private static int instanceCount = 0;

    @PostConstruct
    public void init() {
        instanceCount++;
        System.out.println("[AuditLogService] Singleton instance #" + instanceCount + " created.");
        // If this ever prints > 1, the Singleton guarantee is broken.
    }

    public static int getInstanceCount() { return instanceCount; }

    // ─── Public API ──────────────────────────────────────────────────────

    /**
     * Log a user action to the audit_log table.
     *
     * @param userId      the user performing the action (nullable for system)
     * @param action      short action name e.g. "LOGIN", "BOOK_APPOINTMENT",
     *                    "GENERATE_INVOICE", "CANCEL_APPOINTMENT", "CREATE_EMR"
     * @param tableName   entity/table affected e.g. "appointments", "invoices"
     * @param recordRef   the ID of the affected record e.g. "APT00002"
     * @param description human-readable description of what happened
     */
    public void logAction(String userId, String action,
                          String tableName, String recordRef,
                          String description) {
        try {
            AuditLog log = new AuditLog();
            log.setUserId(userId);
            log.setAction(action);
            log.setTableName(tableName);
            log.setRecordRef(recordRef);
            log.setDescription(description);
            log.setLogTime(LocalDateTime.now());
            auditLogRepo.save(log);
        } catch (Exception e) {
            // Audit logging must never crash the main flow
            System.err.println("[AuditLogService] Failed to write audit log: " + e.getMessage());
        }
    }

    /**
     * Convenience overload — log without a specific record reference.
     */
    public void logAction(String userId, String action, String description) {
        logAction(userId, action, null, null, description);
    }

    /**
     * Retrieve recent audit logs — used by admin dashboard.
     */
    public List<AuditLog> getRecentLogs() {
        try {
            return auditLogRepo.findTop50ByOrderByLogTimeDesc();
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    /**
     * Retrieve logs for a specific user.
     */
    public List<AuditLog> getLogsForUser(String userId) {
        try {
            return auditLogRepo.findByUserIdOrderByLogTimeDesc(userId);
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }
}
