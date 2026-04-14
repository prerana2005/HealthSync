package com.healthsync.observer;

import com.healthsync.model.Notification;
import com.healthsync.model.User;

/**
 * NotificationObserver — Behavioral Design Pattern: OBSERVER
 * =====================================================================
 *
 * PATTERN: Observer (Behavioral)
 *
 * INTENT: Define a one-to-many dependency so that when a subject (e.g.,
 * an Appointment or Invoice) changes state, all registered observers are
 * notified and updated automatically.
 *
 * WHY IT IS NEEDED HERE:
 * State transitions in HealthSync trigger notifications — appointment
 * confirmed, lab result ready, payment received, etc. (see State
 * Diagrams in OOAD docs). Without Observer:
 *   1. Every service method would contain notification logic → violates SRP.
 *   2. Adding a new channel (SMS, email, push) requires modifying
 *      service classes → violates OCP.
 *   3. Notification and business logic are tightly coupled → high coupling.
 *
 * HOW OBSERVER SOLVES THIS:
 * NotificationObserver is the observer interface. Concrete observers
 * (PersistentNotificationObserver for DB, future EmailObserver, etc.)
 * implement it. Subject classes (AppointmentService, BillingFacade)
 * accept a list of observers and call notifyObservers() on state change.
 * New delivery channels can be added without touching existing code.
 *
 * SOLID: OCP — add new observers without modifying existing subjects.
 * SOLID: DIP — subjects depend on the interface, not concrete classes.
 * GRASP: Low Coupling — subjects do not know HOW notifications are sent.
 * GRASP: Polymorphism — each observer handles notification its own way.
 *
 * MAPS TO: State Diagrams in OOAD docs — every state transition that
 * requires user feedback uses this observer to trigger a notification.
 * =====================================================================
 */
public interface NotificationObserver {

    /**
     * Called by a subject when a notable event occurs.
     *
     * @param user    the user who should receive the notification
     * @param type    notification category
     * @param title   short title shown in the notification header
     * @param message full notification body text
     */
    void onEvent(User user, Notification.NotifType type, String title, String message);
}
