package com.healthsync.facade;

import com.healthsync.factory.HealthSyncFactory;
import com.healthsync.model.*;
import com.healthsync.observer.NotificationObserver;
import com.healthsync.repository.*;
import com.healthsync.singleton.AuditLogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Map;

/**
 * BillingFacade — Structural Pattern: FACADE — UC4: Billing & Insurance
 * =====================================================================
 *
 * PATTERN: Facade (Structural)
 * Provides a simplified, unified interface to the billing subsystem:
 * InvoiceRepository + PaymentRepository + Notifications + AuditLog.
 *
 * PATTERNS USED INSIDE:
 *   - Factory Method (Creational #1): HealthSyncFactory.createInvoice()
 *     — entity construction centralised, not duplicated in controllers.
 *   - Singleton (Creational #2): AuditLogService — same single instance
 *     logs UC4 actions across all billing operations.
 *   - Observer (Behavioral): NotificationObserver.onEvent() — decouples
 *     billing state transitions from notification delivery.
 *
 * USE CASE: UC4 — Billing & Insurance
 *   Activity Diagram flow:
 *     Staff selects services → calculate charges → generate invoice
 *     → patient pays → update status → send receipt.
 *   State Diagram (BillingInsurance object):
 *     BillingInProgress → InvoiceGenerated → AwaitingPayment
 *     → PaymentProcessing → Paid → ReceiptSent.
 *
 * SOLID:
 *   S — BillingFacade owns billing workflow; BillingController owns HTTP.
 *   I — Controllers see only generateInvoice() and processPayment().
 *   D — Depends on repository interfaces and observer interface.
 * GRASP:
 *   Low Coupling — BillingController not coupled to 4 repositories.
 *   Indirection — Facade stands between controller and subsystem.
 *   High Cohesion — all methods relate to billing lifecycle.
 * =====================================================================
 */
@Component
public class BillingFacade {

    @Autowired private InvoiceRepository invoiceRepo;
    @Autowired private PaymentRepository paymentRepo;
    @Autowired private PatientRepository patientRepo;

    /** Singleton (Creational #2) — one instance, all billing actions audited */
    @Autowired private AuditLogService auditLogService;

    /** Observer (Behavioral) — notification delivery decoupled */
    @Autowired private NotificationObserver notificationObserver;

    // ─── UC4 Step 1: Generate Invoice ────────────────────────────────────

    /**
     * Aggregates charges and persists an Invoice.
     * State transition: BillingInProgress → InvoiceGenerated → AwaitingPayment.
     *
     * @param fields map with: patientId, consultationFee, medicationCost,
     *               labCharges, roomCharges, taxAmount, generatedBy
     */
    public Invoice generateInvoice(Map<String, String> fields) {
        String patientId = fields.get("patientId");
        Patient patient = patientRepo.findById(patientId)
                .orElseThrow(() -> new IllegalArgumentException("Patient not found: " + patientId));

        // Factory Method (Creational #1): centralised entity construction
        int nextId = invoiceRepo.findMaxBillId() + 1;
        Invoice inv = HealthSyncFactory.createInvoice(nextId, patient, fields);
        invoiceRepo.save(inv);

        // Observer: notify patient — state InvoiceGenerated → AwaitingPayment
        notificationObserver.onEvent(
                patient.getUser(),
                Notification.NotifType.BILLING,
                "Invoice Generated",
                "Invoice " + inv.getBillId() + " for \u20b9" + inv.getTotalAmount()
                        + " is ready. Please proceed with payment."
        );

        // Singleton: audit invoice creation
        auditLogService.logAction(
                fields.getOrDefault("generatedBy", "SYSTEM"),
                "GENERATE_INVOICE",
                "invoices",
                inv.getBillId(),
                "Invoice generated for patient " + patient.getUser().getFullName()
                        + " — total \u20b9" + inv.getTotalAmount()
        );

        return inv;
    }

    // ─── UC4 Step 2: Process Payment ─────────────────────────────────────

    /**
     * Records payment and updates invoice to PAID.
     * State transition: AwaitingPayment → PaymentProcessing → Paid → ReceiptSent.
     *
     * @param billId  invoice ID
     * @param amount  amount paid
     * @param method  CASH / CARD / UPI / INSURANCE / ONLINE
     * @param refNo   optional reference number
     */
    public Invoice processPayment(String billId, String amount, String method, String refNo) {
        Invoice inv = invoiceRepo.findById(billId)
                .orElseThrow(() -> new IllegalArgumentException("Invoice not found: " + billId));

        Payment payment = new Payment();
        payment.setInvoice(inv);
        payment.setAmountPaid(new BigDecimal(amount));
        payment.setPaymentMethod(Payment.PaymentMethod.valueOf(method));
        payment.setReferenceNo(refNo != null ? refNo : "");
        paymentRepo.save(payment);

        // State: PaymentProcessing → Paid
        inv.setPaymentStatus(Invoice.PaymentStatus.PAID);
        invoiceRepo.save(inv);

        // Observer: send receipt — state Paid → ReceiptSent
        notificationObserver.onEvent(
                inv.getPatient().getUser(),
                Notification.NotifType.BILLING,
                "Payment Received — Receipt",
                "Payment of \u20b9" + amount + " received for bill "
                        + billId + " via " + method + ". Thank you!"
        );

        // Singleton: audit payment
        auditLogService.logAction(
                inv.getPatient().getUser().getUserId(),
                "PROCESS_PAYMENT",
                "payments",
                billId,
                "Payment \u20b9" + amount + " processed via " + method
                        + " for invoice " + billId
        );

        return inv;
    }
}
