package com.healthsync.model;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Entity
@Table(name = "appointments")
public class Appointment {

    @Id
    @Column(name = "appointment_id", length = 10)
    private String appointmentId;

    @ManyToOne
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;

    @ManyToOne
    @JoinColumn(name = "doctor_id", nullable = false)
    private Doctor doctor;

    @ManyToOne
    @JoinColumn(name = "slot_id", nullable = false)
    private DoctorAvailabilitySlot slot;

    @Column(name = "appointment_date", nullable = false)
    private LocalDate appointmentDate;

    @Column(name = "time_slot", nullable = false)
    private LocalTime timeSlot;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private AppointmentStatus status = AppointmentStatus.SCHEDULED;

    @Column(name = "booking_time", nullable = false)
    private LocalDateTime bookingTime;

    @Column(name = "cancel_reason", length = 255)
    private String cancelReason;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    /**
     * AppointmentStatus — aligned with Appointment Object State Diagram:
     *
     *   CREATED             → initial state after request (maps to "Created")
     *   PENDING_CONFIRMATION → availability check in progress ("PendingConfirmation")
     *   SCHEDULED           → slot confirmed ("Scheduled")
     *   IN_CONSULTATION     → consultation start ("InProgress")
     *   RESCHEDULED         → change request + new slot assigned ("Rescheduled")
     *   COMPLETED           → consultation done ("Completed")
     *   CANCELLED           → cancelled event ("Cancelled")
     *   WAITLISTED          → no slots available, patient on waitlist
     *   CLOSED              → archived final state ("Closed")
     *   FOLLOW_UP           → follow-up appointment scheduled
     *
     * State Diagram path:
     *   CREATED → PENDING_CONFIRMATION → SCHEDULED → IN_CONSULTATION → COMPLETED → CLOSED
     *                                 ↘ RESCHEDULED ↗
     *                                 ↘ CANCELLED → CLOSED
     *                                 ↘ WAITLISTED (slot unavailable)
     */
    public enum AppointmentStatus {
        CREATED,
        PENDING_CONFIRMATION,
        SCHEDULED,
        IN_CONSULTATION,
        RESCHEDULED,
        COMPLETED,
        CANCELLED,
        WAITLISTED,
        CLOSED,
        FOLLOW_UP
    }

    @PrePersist
    protected void onCreate() {
        if (bookingTime == null) bookingTime = LocalDateTime.now();
    }

    // Getters and Setters
    public String getAppointmentId() { return appointmentId; }
    public void setAppointmentId(String appointmentId) { this.appointmentId = appointmentId; }
    public Patient getPatient() { return patient; }
    public void setPatient(Patient patient) { this.patient = patient; }
    public Doctor getDoctor() { return doctor; }
    public void setDoctor(Doctor doctor) { this.doctor = doctor; }
    public DoctorAvailabilitySlot getSlot() { return slot; }
    public void setSlot(DoctorAvailabilitySlot slot) { this.slot = slot; }
    public LocalDate getAppointmentDate() { return appointmentDate; }
    public void setAppointmentDate(LocalDate appointmentDate) { this.appointmentDate = appointmentDate; }
    public LocalTime getTimeSlot() { return timeSlot; }
    public void setTimeSlot(LocalTime timeSlot) { this.timeSlot = timeSlot; }
    public AppointmentStatus getStatus() { return status; }
    public void setStatus(AppointmentStatus status) { this.status = status; }
    public LocalDateTime getBookingTime() { return bookingTime; }
    public void setBookingTime(LocalDateTime bookingTime) { this.bookingTime = bookingTime; }
    public String getCancelReason() { return cancelReason; }
    public void setCancelReason(String cancelReason) { this.cancelReason = cancelReason; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
}
