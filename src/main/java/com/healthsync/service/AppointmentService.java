package com.healthsync.service;

import com.healthsync.model.*;
import com.healthsync.observer.NotificationObserver;
import com.healthsync.repository.*;
import com.healthsync.singleton.AuditLogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

/**
 * AppointmentService — Service layer for UC2: Appointment Scheduling.
 *
 * =====================================================================
 * DESIGN PATTERNS USED:
 *   - Observer  (Behavioral): NotificationObserver.onEvent() called on
 *     every state change (SCHEDULED, CANCELLED).
 *   - Singleton (Creational): AuditLogService injected — same instance
 *     used across all service calls in the entire application.
 *
 * =====================================================================
 * SOLID:
 *   S — Only responsible for appointment scheduling logic.
 *   O — New scheduling rules added via strategy; class not modified.
 *   D — Depends on repository/observer interfaces, not concretions.
 *
 * GRASP:
 *   Controller  — Co-ordinates UC2 between HTTP layer and domain.
 *   Creator     — Creates Appointment objects with all required data.
 *   Low Coupling — Controllers do not touch repositories directly.
 *   High Cohesion — Every method relates to appointment lifecycle.
 * =====================================================================
 */
@Service
public class AppointmentService {

    @Autowired private AppointmentRepository apptRepo;
    @Autowired private PatientRepository patientRepo;
    @Autowired private DoctorRepository doctorRepo;
    @Autowired private SlotRepository slotRepo;
    @Autowired private NotificationRepository notifRepo;

    /** Singleton (Creational pattern #2) — same instance app-wide */
    @Autowired private AuditLogService auditLogService;

    /** Observer (Behavioral pattern) — decouples notification delivery */
    @Autowired private NotificationObserver notificationObserver;

    // ─── UC2: Schedule Appointment ───────────────────────────────────────

    /**
     * Schedule a new appointment.
     * Activity Diagram UC2 flow: Patient selects slot → system checks
     * availability → confirms → stores → notifies.
     *
     * @param patientId patient booking the appointment
     * @param doctorId  doctor being booked
     * @param slotId    selected availability slot
     * @param date      appointment date (ISO: yyyy-MM-dd)
     * @param notes     optional patient notes
     */
    public Appointment scheduleAppointment(String patientId, String doctorId,
                                            int slotId, String date, String notes) {

        Patient patient = patientRepo.findById(patientId)
                .orElseThrow(() -> new IllegalArgumentException("Patient not found: " + patientId));
        Doctor doctor = doctorRepo.findById(doctorId)
                .orElseThrow(() -> new IllegalArgumentException("Doctor not found: " + doctorId));
        DoctorAvailabilitySlot slot = slotRepo.findById(slotId)
                .orElseThrow(() -> new IllegalArgumentException("Slot not found: " + slotId));

        // FR-02: Real-time conflict prevention
        if (slot.getIsBooked()) {
            throw new IllegalStateException("Slot already booked. Please select another slot.");
        }

        // Build appointment
        int nextId = apptRepo.findMaxAppointmentId() + 1;
        Appointment appt = new Appointment();
        appt.setAppointmentId(String.format("APT%05d", nextId));
        appt.setPatient(patient);
        appt.setDoctor(doctor);
        appt.setSlot(slot);
        appt.setAppointmentDate(LocalDate.parse(date));
        appt.setTimeSlot(slot.getStartTime());
        appt.setStatus(Appointment.AppointmentStatus.SCHEDULED);
        appt.setNotes(notes != null ? notes : "");
        appt.setBookingTime(LocalDateTime.now());
        apptRepo.save(appt);

        // Mark slot booked
        slot.setIsBooked(true);
        slotRepo.save(slot);

        // Observer pattern: notify patient — state SCHEDULED triggers notification
        notificationObserver.onEvent(
                patient.getUser(),
                Notification.NotifType.APPOINTMENT,
                "Appointment Confirmed",
                "Your appointment with " + doctor.getUser().getFullName()
                        + " is confirmed for " + date + " at " + slot.getStartTime() + "."
        );

        // Singleton pattern: audit the action — one AuditLogService across app
        auditLogService.logAction(
                patient.getUser().getUserId(),
                "BOOK_APPOINTMENT",
                "appointments",
                appt.getAppointmentId(),
                "Patient " + patient.getUser().getFullName()
                        + " booked appointment with " + doctor.getUser().getFullName()
                        + " on " + date
        );

        return appt;
    }

    // ─── Cancel Appointment ──────────────────────────────────────────────

    /**
     * Cancel appointment — frees slot, notifies patient, audits action.
     * State diagram: SCHEDULED → CANCELLED (on cancel event).
     */
    public Appointment cancelAppointment(String appointmentId, String reason) {
        Appointment appt = apptRepo.findById(appointmentId)
                .orElseThrow(() -> new IllegalArgumentException("Appointment not found"));

        appt.setStatus(Appointment.AppointmentStatus.CANCELLED);
        appt.setCancelReason(reason != null ? reason : "Cancelled by user");
        apptRepo.save(appt);

        // Free slot
        DoctorAvailabilitySlot slot = appt.getSlot();
        slot.setIsBooked(false);
        slotRepo.save(slot);

        // Observer: notify cancellation
        notificationObserver.onEvent(
                appt.getPatient().getUser(),
                Notification.NotifType.APPOINTMENT,
                "Appointment Cancelled",
                "Your appointment on " + appt.getAppointmentDate() + " has been cancelled."
        );

        // Singleton: audit
        auditLogService.logAction(
                appt.getPatient().getUser().getUserId(),
                "CANCEL_APPOINTMENT",
                "appointments",
                appointmentId,
                "Appointment cancelled. Reason: " + appt.getCancelReason()
        );

        return appt;
    }

    // ─── Complete Appointment ────────────────────────────────────────────

    /**
     * State diagram: SCHEDULED / IN_CONSULTATION → COMPLETED.
     */
    public Appointment completeAppointment(String appointmentId) {
        Appointment appt = apptRepo.findById(appointmentId)
                .orElseThrow(() -> new IllegalArgumentException("Appointment not found"));
        appt.setStatus(Appointment.AppointmentStatus.COMPLETED);
        apptRepo.save(appt);

        auditLogService.logAction(
                null, "COMPLETE_APPOINTMENT",
                "appointments", appointmentId,
                "Appointment " + appointmentId + " marked COMPLETED."
        );
        return appt;
    }

    // ─── Conflict check — FR-02 ──────────────────────────────────────────

    public boolean checkConflict(String doctorId, String date, int newSlotId) {
        LocalDate localDate = LocalDate.parse(date);
        return slotRepo.findByDoctorDoctorIdAndSlotDate(doctorId, localDate)
                .stream()
                .anyMatch(s -> s.getSlotId().equals(newSlotId) && s.getIsBooked());
    }

    // ─── Search doctors — FR-08 ──────────────────────────────────────────

    public List<Doctor> searchDoctors(String specialization, String deptId) {
        if (specialization != null && !specialization.isEmpty())
            return doctorRepo.findBySpecializationContainingIgnoreCase(specialization);
        if (deptId != null && !deptId.isEmpty())
            return doctorRepo.findByDepartmentDeptId(deptId);
        return doctorRepo.findAll();
    }
}
