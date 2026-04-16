package com.healthsync.controller;

import com.healthsync.config.RoleGuard;
import com.healthsync.model.*;
import com.healthsync.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.time.*;
import java.util.*;

@RestController
@RequestMapping("/api/appointments")
@CrossOrigin(origins = "*")
public class AppointmentController {

    @Autowired private AppointmentRepository apptRepo;
    @Autowired private PatientRepository patientRepo;
    @Autowired private DoctorRepository doctorRepo;
    @Autowired private SlotRepository slotRepo;

    @GetMapping
    public ResponseEntity<?> getAll(@RequestHeader(value = "X-User-Role", required = false) String role) {
        ResponseEntity<?> denied = RoleGuard.requireRole(role, "ADMINISTRATOR", "DOCTOR", "STAFF");
        if (denied != null) return denied;
        return ResponseEntity.ok(apptRepo.findAll().stream().map(this::toMap).toList());
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getById(@PathVariable String id) {
        return apptRepo.findById(id).map(a -> ResponseEntity.ok(toMap(a)))
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/patient/{patientId}")
    public List<Map<String, Object>> byPatient(@PathVariable String patientId) {
        return apptRepo.findByPatientPatientId(patientId).stream().map(this::toMap).toList();
    }

    @GetMapping("/doctor/{doctorId}")
    public List<Map<String, Object>> byDoctor(@PathVariable String doctorId) {
        return apptRepo.findByDoctorDoctorId(doctorId).stream().map(this::toMap).toList();
    }

    @GetMapping("/slots")
    public List<Map<String, Object>> getSlots(@RequestParam String doctorId, @RequestParam String date) {
        LocalDate d = LocalDate.parse(date);
        return slotRepo.findByDoctorDoctorIdAndSlotDateAndIsBookedFalse(doctorId, d).stream().map(s -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("slotId", s.getSlotId());
            m.put("startTime", s.getStartTime().toString());
            m.put("endTime", s.getEndTime().toString());
            return m;
        }).toList();
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody Map<String, String> body) {
        try {
            String patientId = body.get("patientId");
            String doctorId = body.get("doctorId");
            int slotId = Integer.parseInt(body.get("slotId"));
            String date = body.get("date");

            Patient patient = patientRepo.findById(patientId).orElse(null);
            Doctor doctor = doctorRepo.findById(doctorId).orElse(null);
            DoctorAvailabilitySlot slot = slotRepo.findById(slotId).orElse(null);

            if (patient == null || doctor == null || slot == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "Invalid patient, doctor, or slot"));
            }

            if (slot.getIsBooked()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Slot already booked"));
            }

            int nextId = apptRepo.findMaxAppointmentId() + 1;
            Appointment appt = new Appointment();
            appt.setAppointmentId(String.format("APT%05d", nextId));
            appt.setPatient(patient);
            appt.setDoctor(doctor);
            appt.setSlot(slot);
            appt.setAppointmentDate(LocalDate.parse(date));
            appt.setTimeSlot(slot.getStartTime());
            appt.setStatus(Appointment.AppointmentStatus.SCHEDULED);
            appt.setNotes(body.getOrDefault("notes", ""));
            apptRepo.save(appt);

            // Mark slot as booked
            slot.setIsBooked(true);
            slotRepo.save(slot);

            return ResponseEntity.ok(toMap(appt));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/{id}/cancel")
    public ResponseEntity<?> cancel(@PathVariable String id, @RequestBody Map<String, String> body) {
        return apptRepo.findById(id).map(appt -> {
            appt.setStatus(Appointment.AppointmentStatus.CANCELLED);
            appt.setCancelReason(body.getOrDefault("reason", "Cancelled by user"));
            apptRepo.save(appt);

            // Free up the slot
            DoctorAvailabilitySlot slot = appt.getSlot();
            slot.setIsBooked(false);
            slotRepo.save(slot);

            return ResponseEntity.ok(toMap(appt));
        }).orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}/complete")
    public ResponseEntity<?> complete(@PathVariable String id) {
        return apptRepo.findById(id).map(appt -> {
            appt.setStatus(Appointment.AppointmentStatus.COMPLETED);
            apptRepo.save(appt);
            return ResponseEntity.ok(toMap(appt));
        }).orElse(ResponseEntity.notFound().build());
    }

    // Create slots for a doctor
    @PostMapping("/slots")
    public ResponseEntity<?> createSlots(@RequestBody Map<String, Object> body) {
        try {
            String doctorId = (String) body.get("doctorId");
            String date = (String) body.get("date");
            String startHour = (String) body.get("startHour");
            String endHour = (String) body.get("endHour");
            int durationMinutes = body.containsKey("durationMinutes") ? (int) body.get("durationMinutes") : 30;

            Doctor doctor = doctorRepo.findById(doctorId).orElse(null);
            if (doctor == null) return ResponseEntity.badRequest().body(Map.of("error", "Doctor not found"));

            LocalDate slotDate = LocalDate.parse(date);
            LocalTime start = LocalTime.parse(startHour);
            LocalTime end = LocalTime.parse(endHour);

            List<Map<String, Object>> created = new ArrayList<>();
            while (start.plusMinutes(durationMinutes).compareTo(end) <= 0) {
                DoctorAvailabilitySlot slot = new DoctorAvailabilitySlot();
                slot.setDoctor(doctor);
                slot.setSlotDate(slotDate);
                slot.setStartTime(start);
                slot.setEndTime(start.plusMinutes(durationMinutes));
                slotRepo.save(slot);

                Map<String, Object> m = new LinkedHashMap<>();
                m.put("slotId", slot.getSlotId());
                m.put("startTime", slot.getStartTime().toString());
                m.put("endTime", slot.getEndTime().toString());
                created.add(m);

                start = start.plusMinutes(durationMinutes);
            }

            return ResponseEntity.ok(Map.of("created", created.size(), "slots", created));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }
    // ─── Follow-up / Reschedule — State Diagram: COMPLETED → FOLLOW_UP ──
    /**
     * Marks an appointment as FOLLOW_UP, indicating a new appointment
     * should be booked. Aligns with the Appointment State Diagram which
     * shows COMPLETED → archive → CLOSED, or a follow-up path back to
     * SCHEDULED via a new booking.
     */
    @PutMapping("/{id}/follow-up")
    public ResponseEntity<?> followUp(@PathVariable String id) {
        return apptRepo.findById(id).map(appt -> {
            appt.setStatus(Appointment.AppointmentStatus.FOLLOW_UP);
            apptRepo.save(appt);
            return ResponseEntity.ok(toMap(appt));
        }).orElse(ResponseEntity.notFound().build());
    }

    // ─── Close — State Diagram: CANCELLED / COMPLETED → CLOSED ──────────
    @PutMapping("/{id}/close")
    public ResponseEntity<?> close(@PathVariable String id) {
        return apptRepo.findById(id).map(appt -> {
            appt.setStatus(Appointment.AppointmentStatus.CLOSED);
            apptRepo.save(appt);
            return ResponseEntity.ok(toMap(appt));
        }).orElse(ResponseEntity.notFound().build());
    }

    private Map<String, Object> toMap(Appointment a) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("appointmentId", a.getAppointmentId());
        m.put("patientId", a.getPatient().getPatientId());
        m.put("patientName", a.getPatient().getUser().getFullName());
        m.put("doctorId", a.getDoctor().getDoctorId());
        m.put("doctorName", a.getDoctor().getUser().getFullName());
        m.put("specialization", a.getDoctor().getSpecialization());
        m.put("date", a.getAppointmentDate().toString());
        m.put("time", a.getTimeSlot().toString());
        m.put("status", a.getStatus().name());
        m.put("notes", a.getNotes());
        m.put("bookingTime", a.getBookingTime() != null ? a.getBookingTime().toString() : null);
        return m;
    }
}
