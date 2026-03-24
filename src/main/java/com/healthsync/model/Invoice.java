package com.healthsync.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "invoices")
public class Invoice {

    @Id
    @Column(name = "bill_id", length = 10)
    private String billId;

    @ManyToOne
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;

    @ManyToOne
    @JoinColumn(name = "appointment_id")
    private Appointment appointment;

    @Column(name = "consultation_fee", nullable = false, precision = 10, scale = 2)
    private BigDecimal consultationFee = BigDecimal.ZERO;

    @Column(name = "medication_cost", nullable = false, precision = 10, scale = 2)
    private BigDecimal medicationCost = BigDecimal.ZERO;

    @Column(name = "lab_charges", nullable = false, precision = 10, scale = 2)
    private BigDecimal labCharges = BigDecimal.ZERO;

    @Column(name = "room_charges", nullable = false, precision = 10, scale = 2)
    private BigDecimal roomCharges = BigDecimal.ZERO;

    @Column(name = "tax_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal taxAmount = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_status", nullable = false)
    private PaymentStatus paymentStatus = PaymentStatus.PENDING;

    @Column(name = "generated_date", nullable = false)
    private LocalDateTime generatedDate;

    @Column(name = "paid_date")
    private LocalDateTime paidDate;

    @Column(name = "insurance_claim", nullable = false, precision = 10, scale = 2)
    private BigDecimal insuranceClaim = BigDecimal.ZERO;

    @Column(name = "generated_by", length = 10)
    private String generatedBy;

    public enum PaymentStatus { PAID, PENDING, PARTIALLY_PAID }

    @PrePersist
    protected void onCreate() { if (generatedDate == null) generatedDate = LocalDateTime.now(); }

    // Computed getters (no DB-level generated columns needed)
    public BigDecimal getSubtotal() {
        return consultationFee.add(medicationCost).add(labCharges).add(roomCharges);
    }

    public BigDecimal getTotalAmount() {
        return getSubtotal().add(taxAmount);
    }

    public String getBillId() { return billId; }
    public void setBillId(String billId) { this.billId = billId; }
    public Patient getPatient() { return patient; }
    public void setPatient(Patient patient) { this.patient = patient; }
    public Appointment getAppointment() { return appointment; }
    public void setAppointment(Appointment appointment) { this.appointment = appointment; }
    public BigDecimal getConsultationFee() { return consultationFee; }
    public void setConsultationFee(BigDecimal consultationFee) { this.consultationFee = consultationFee; }
    public BigDecimal getMedicationCost() { return medicationCost; }
    public void setMedicationCost(BigDecimal medicationCost) { this.medicationCost = medicationCost; }
    public BigDecimal getLabCharges() { return labCharges; }
    public void setLabCharges(BigDecimal labCharges) { this.labCharges = labCharges; }
    public BigDecimal getRoomCharges() { return roomCharges; }
    public void setRoomCharges(BigDecimal roomCharges) { this.roomCharges = roomCharges; }
    public BigDecimal getTaxAmount() { return taxAmount; }
    public void setTaxAmount(BigDecimal taxAmount) { this.taxAmount = taxAmount; }
    public PaymentStatus getPaymentStatus() { return paymentStatus; }
    public void setPaymentStatus(PaymentStatus paymentStatus) { this.paymentStatus = paymentStatus; }
    public LocalDateTime getGeneratedDate() { return generatedDate; }
    public void setGeneratedDate(LocalDateTime generatedDate) { this.generatedDate = generatedDate; }
    public LocalDateTime getPaidDate() { return paidDate; }
    public void setPaidDate(LocalDateTime paidDate) { this.paidDate = paidDate; }
    public BigDecimal getInsuranceClaim() { return insuranceClaim; }
    public void setInsuranceClaim(BigDecimal insuranceClaim) { this.insuranceClaim = insuranceClaim; }
    public String getGeneratedBy() { return generatedBy; }
    public void setGeneratedBy(String generatedBy) { this.generatedBy = generatedBy; }
}
