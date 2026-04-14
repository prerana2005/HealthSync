package com.healthsync.observer;

import com.healthsync.model.Notification;
import com.healthsync.model.User;
import com.healthsync.repository.NotificationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * PersistentNotificationObserver — Concrete Observer (Behavioral: Observer pattern)
 *
 * Persists every notification event to the notifications table so patients
 * and staff can retrieve them via /api/dashboard/notifications/{userId}.
 *
 * To add an EMAIL channel: create EmailNotificationObserver implements
 * NotificationObserver and register it — no existing code changes required.
 */
@Component
public class PersistentNotificationObserver implements NotificationObserver {

    @Autowired
    private NotificationRepository notifRepo;

    @Override
    public void onEvent(User user, Notification.NotifType type, String title, String message) {
        Notification n = new Notification();
        n.setUser(user);
        n.setNotifType(type);
        n.setTitle(title);
        n.setMessage(message);
        n.setIsRead(false);
        notifRepo.save(n);
    }
}
