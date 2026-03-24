package com.healthsync.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "prescriptions")
public class Prescription {

    @Id
    @Column(name = "prescription_id", length = 10)
    private String prescriptionId;

    @ManyToOne
    @JoinColumn(name = "record_id", nullable = false)
    private MedicalRecord record;

    @ManyToOne
    @JoinColumn(name = "doctor_id", nullable = false)
    private Doctor doctor;

    @ManyToOne
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;

    @Column(name = "issued_date", nullable = false)
    private LocalDateTime issuedDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private PrescriptionStatus status = PrescriptionStatus.PENDING;

    public enum PrescriptionStatus { PENDING, DISPENSED, CANCELLED }

    @PrePersist
    protected void onCreate() { if (issuedDate == null) issuedDate = LocalDateTime.now(); }

    public String getPrescriptionId() { return prescriptionId; }
    public void setPrescriptionId(String prescriptionId) { this.prescriptionId = prescriptionId; }
    public MedicalRecord getRecord() { return record; }
    public void setRecord(MedicalRecord record) { this.record = record; }
    public Doctor getDoctor() { return doctor; }
    public void setDoctor(Doctor doctor) { this.doctor = doctor; }
    public Patient getPatient() { return patient; }
    public void setPatient(Patient patient) { this.patient = patient; }
    public LocalDateTime getIssuedDate() { return issuedDate; }
    public void setIssuedDate(LocalDateTime issuedDate) { this.issuedDate = issuedDate; }
    public PrescriptionStatus getStatus() { return status; }
    public void setStatus(PrescriptionStatus status) { this.status = status; }
}
