package com.healthsync.controller;

import com.healthsync.model.*;
import com.healthsync.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.*;

@RestController
@RequestMapping("/api/billing")
@CrossOrigin(origins = "*")
public class BillingController {

    @Autowired private InvoiceRepository invoiceRepo;
    @Autowired private PatientRepository patientRepo;
    @Autowired private PaymentRepository paymentRepo;

    @GetMapping
    public List<Map<String, Object>> getAll() {
        return invoiceRepo.findAll().stream().map(this::toMap).toList();
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getById(@PathVariable String id) {
        return invoiceRepo.findById(id).map(i -> ResponseEntity.ok(toMap(i)))
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/patient/{patientId}")
    public List<Map<String, Object>> byPatient(@PathVariable String patientId) {
        return invoiceRepo.findByPatientPatientId(patientId).stream().map(this::toMap).toList();
    }

    @GetMapping("/pending")
    public List<Map<String, Object>> pending() {
        List<Map<String, Object>> result = new ArrayList<>();
        result.addAll(invoiceRepo.findByPaymentStatus(Invoice.PaymentStatus.PENDING).stream().map(this::toMap).toList());
        result.addAll(invoiceRepo.findByPaymentStatus(Invoice.PaymentStatus.PARTIALLY_PAID).stream().map(this::toMap).toList());
        return result;
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody Map<String, String> body) {
        try {
            Patient patient = patientRepo.findById(body.get("patientId")).orElse(null);
            if (patient == null) return ResponseEntity.badRequest().body(Map.of("error", "Patient not found"));

            int nextId = invoiceRepo.findMaxBillId() + 1;
            Invoice inv = new Invoice();
            inv.setBillId(String.format("BILL%04d", nextId));
            inv.setPatient(patient);
            inv.setConsultationFee(new BigDecimal(body.getOrDefault("consultationFee", "0")));
            inv.setMedicationCost(new BigDecimal(body.getOrDefault("medicationCost", "0")));
            inv.setLabCharges(new BigDecimal(body.getOrDefault("labCharges", "0")));
            inv.setRoomCharges(new BigDecimal(body.getOrDefault("roomCharges", "0")));
            inv.setTaxAmount(new BigDecimal(body.getOrDefault("taxAmount", "0")));
            inv.setPaymentStatus(Invoice.PaymentStatus.PENDING);
            inv.setGeneratedBy(body.getOrDefault("generatedBy", ""));
            invoiceRepo.save(inv);

            return ResponseEntity.ok(toMap(inv));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/{id}/pay")
    public ResponseEntity<?> pay(@PathVariable String id, @RequestBody Map<String, String> body) {
        return invoiceRepo.findById(id).map(inv -> {
            Payment payment = new Payment();
            payment.setInvoice(inv);
            payment.setAmountPaid(new BigDecimal(body.getOrDefault("amount", "0")));
            payment.setPaymentMethod(Payment.PaymentMethod.valueOf(body.getOrDefault("method", "CASH")));
            payment.setReferenceNo(body.getOrDefault("referenceNo", ""));
            paymentRepo.save(payment);

            // Update invoice status
            inv.setPaymentStatus(Invoice.PaymentStatus.PAID);
            invoiceRepo.save(inv);

            return ResponseEntity.ok(toMap(inv));
        }).orElse(ResponseEntity.notFound().build());
    }

    private Map<String, Object> toMap(Invoice i) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("billId", i.getBillId());
        m.put("patientId", i.getPatient().getPatientId());
        m.put("patientName", i.getPatient().getUser().getFullName());
        m.put("consultationFee", i.getConsultationFee());
        m.put("medicationCost", i.getMedicationCost());
        m.put("labCharges", i.getLabCharges());
        m.put("roomCharges", i.getRoomCharges());
        m.put("subtotal", i.getSubtotal());
        m.put("taxAmount", i.getTaxAmount());
        m.put("totalAmount", i.getTotalAmount());
        m.put("paymentStatus", i.getPaymentStatus().name());
        m.put("generatedDate", i.getGeneratedDate() != null ? i.getGeneratedDate().toString() : null);
        m.put("paidDate", i.getPaidDate() != null ? i.getPaidDate().toString() : null);
        return m;
    }
}
