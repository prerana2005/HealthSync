package com.healthsync.repository;

import com.healthsync.model.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Integer> {
    List<Notification> findByUserUserIdAndIsReadFalseOrderBySentAtDesc(String userId);
    List<Notification> findByUserUserIdOrderBySentAtDesc(String userId);
    long countByUserUserIdAndIsReadFalse(String userId);
}
