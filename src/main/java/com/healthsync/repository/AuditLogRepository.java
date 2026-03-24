package com.healthsync.repository;

import com.healthsync.model.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
    List<AuditLog> findTop50ByOrderByLogTimeDesc();
    List<AuditLog> findByUserIdOrderByLogTimeDesc(String userId);
}
