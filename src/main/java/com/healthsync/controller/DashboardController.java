package com.healthsync.controller;

import com.healthsync.model.*;
import com.healthsync.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/dashboard")
@CrossOrigin(origins = "*")
public class DashboardController {

    @Autowired private PatientRepository patientRepo;
    @Autowired private DoctorRepository doctorRepo;
    @Autowired private AppointmentRepository apptRepo;
    @Autowired private InvoiceRepository invoiceRepo;
    @Autowired private PharmacyRepository pharmacyRepo;
    @Autowired private NotificationRepository notifRepo;

    @GetMapping("/stats")
    public Map<String, Object> getStats() {
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("totalPatients", patientRepo.count());
        stats.put("totalDoctors", doctorRepo.count());
        stats.put("totalAppointments", apptRepo.count());
        stats.put("scheduledAppointments", apptRepo.countByStatus(Appointment.AppointmentStatus.SCHEDULED));
        stats.put("completedAppointments", apptRepo.countByStatus(Appointment.AppointmentStatus.COMPLETED));
        stats.put("totalInvoices", invoiceRepo.count());
        stats.put("pendingInvoices", invoiceRepo.findByPaymentStatus(Invoice.PaymentStatus.PENDING).size());
        stats.put("lowStockMedicines", pharmacyRepo.findLowStock().size());
        stats.put("totalMedicines", pharmacyRepo.count());
        return stats;
    }

    @GetMapping("/notifications/{userId}")
    public List<Notification> getNotifications(@PathVariable String userId) {
        return notifRepo.findByUserUserIdAndIsReadFalseOrderBySentAtDesc(userId);
    }

    @PutMapping("/notifications/{id}/read")
    public ResponseEntity<?> markRead(@PathVariable Integer id) {
        return notifRepo.findById(id).map(n -> {
            n.setIsRead(true);
            notifRepo.save(n);
            return ResponseEntity.ok(Map.of("status", "read"));
        }).orElse(ResponseEntity.notFound().build());
    }
}
