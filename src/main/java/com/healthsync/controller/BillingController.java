package com.healthsync.controller;

import com.healthsync.config.RoleGuard;
import com.healthsync.facade.BillingFacade;
import com.healthsync.model.Invoice;
import com.healthsync.repository.InvoiceRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * BillingController — UPDATED to use BillingFacade (Structural: Facade pattern).
 *
 * This controller is now thin — it only handles HTTP concerns:
 *   - Parse request body
 *   - Call BillingFacade
 *   - Return HTTP response
 *
 * All business logic (charge aggregation, payment processing, notifications)
 * lives in BillingFacade. This satisfies:
 *   SOLID SRP: controller owns HTTP, facade owns billing logic.
 *   GRASP Low Coupling: controller does not depend on PaymentRepository
 *     or NotificationRepository directly.
 */
@RestController
@RequestMapping("/api/billing")
@CrossOrigin(origins = "*")
public class BillingController {

    @Autowired private BillingFacade billingFacade;
    @Autowired private InvoiceRepository invoiceRepo;

    @GetMapping
    public ResponseEntity<?> getAll(@RequestHeader(value = "X-User-Role", required = false) String role) {
        ResponseEntity<?> denied = RoleGuard.requireRole(role, "ADMINISTRATOR", "STAFF");
        if (denied != null) return denied;
        return ResponseEntity.ok(invoiceRepo.findAll().stream().map(this::toMap).toList());
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getById(@PathVariable String id) {
        return invoiceRepo.findById(id)
                .map(i -> ResponseEntity.ok(toMap(i)))
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/patient/{patientId}")
    public List<Map<String, Object>> byPatient(@PathVariable String patientId) {
        return invoiceRepo.findByPatientPatientId(patientId)
                .stream().map(this::toMap).toList();
    }

    @GetMapping("/pending")
    public List<Map<String, Object>> pending() {
        List<Map<String, Object>> result = new ArrayList<>();
        result.addAll(invoiceRepo.findByPaymentStatus(Invoice.PaymentStatus.PENDING)
                .stream().map(this::toMap).toList());
        result.addAll(invoiceRepo.findByPaymentStatus(Invoice.PaymentStatus.PARTIALLY_PAID)
                .stream().map(this::toMap).toList());
        return result;
    }

    /**
     * Generate invoice — delegates to BillingFacade (Facade pattern).
     * Activity Diagram UC4: Staff → Billing System → Calculate → Generate invoice.
     */
    @PostMapping
    public ResponseEntity<?> create(@RequestBody Map<String, String> body) {
        try {
            Invoice inv = billingFacade.generateInvoice(body);
            return ResponseEntity.ok(toMap(inv));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Process payment — delegates to BillingFacade (Facade pattern).
     * Activity Diagram UC4: Patient → Pay → System updates status → Send receipt.
     */
    @PostMapping("/{id}/pay")
    public ResponseEntity<?> pay(@PathVariable String id, @RequestBody Map<String, String> body) {
        try {
            Invoice inv = billingFacade.processPayment(
                    id,
                    body.getOrDefault("amount", "0"),
                    body.getOrDefault("method", "CASH"),
                    body.getOrDefault("referenceNo", "")
            );
            return ResponseEntity.ok(toMap(inv));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
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
        m.put("insuranceProvider", i.getInsuranceProvider());
        m.put("insurancePolicyNo", i.getInsurancePolicyNo());
        m.put("insuranceCoverage", i.getInsuranceCoverage());
        m.put("claimStatus", i.getClaimStatus().name());
        m.put("generatedDate", i.getGeneratedDate() != null ? i.getGeneratedDate().toString() : null);
        m.put("paidDate", i.getPaidDate() != null ? i.getPaidDate().toString() : null);
        return m;
    }
}
