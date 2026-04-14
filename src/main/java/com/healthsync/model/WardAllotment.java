package com.healthsync.model;

import jakarta.persistence.*;
import java.time.LocalDate;

/**
 * WardAllotment — tracks which patient is assigned to which ward/bed.
 * From the OOAD class diagram: allotmentId, allotmentDate, dischargeDate,
 * bedNumber, isICU, nurseAssigned.
 *
 * GRASP: Creator — WardAllotment is created by Ward/Staff when a patient is admitted.
 */
@Entity
@Table(name = "ward_allotments")
public class WardAllotment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "allotment_id")
    private Integer allotmentId;

    @ManyToOne
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;

    @ManyToOne
    @JoinColumn(name = "ward_id", nullable = false)
    private Ward ward;

    @Column(name = "allotment_date", nullable = false)
    private LocalDate allotmentDate;

    @Column(name = "discharge_date")
    private LocalDate dischargeDate;

    @Column(name = "bed_number", nullable = false, length = 10)
    private String bedNumber;

    @Column(name = "is_icu", nullable = false)
    private Boolean isICU = false;

    @Column(name = "nurse_assigned", length = 10)
    private String nurseAssigned;  // staffId of assigned nurse

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private AllotmentStatus status = AllotmentStatus.ACTIVE;

    public enum AllotmentStatus { ACTIVE, DISCHARGED, TRANSFERRED }

    @PrePersist
    protected void onCreate() {
        if (allotmentDate == null) allotmentDate = LocalDate.now();
    }

    // Getters and Setters
    public Integer getAllotmentId() { return allotmentId; }
    public void setAllotmentId(Integer allotmentId) { this.allotmentId = allotmentId; }
    public Patient getPatient() { return patient; }
    public void setPatient(Patient patient) { this.patient = patient; }
    public Ward getWard() { return ward; }
    public void setWard(Ward ward) { this.ward = ward; }
    public LocalDate getAllotmentDate() { return allotmentDate; }
    public void setAllotmentDate(LocalDate allotmentDate) { this.allotmentDate = allotmentDate; }
    public LocalDate getDischargeDate() { return dischargeDate; }
    public void setDischargeDate(LocalDate dischargeDate) { this.dischargeDate = dischargeDate; }
    public String getBedNumber() { return bedNumber; }
    public void setBedNumber(String bedNumber) { this.bedNumber = bedNumber; }
    public Boolean getIsICU() { return isICU; }
    public void setIsICU(Boolean isICU) { this.isICU = isICU; }
    public String getNurseAssigned() { return nurseAssigned; }
    public void setNurseAssigned(String nurseAssigned) { this.nurseAssigned = nurseAssigned; }
    public AllotmentStatus getStatus() { return status; }
    public void setStatus(AllotmentStatus status) { this.status = status; }

    /** Discharge patient — updates status and date, frees the bed. */
    public void discharge() {
        this.status = AllotmentStatus.DISCHARGED;
        this.dischargeDate = LocalDate.now();
        if (this.ward != null) this.ward.freeBed();
    }
}
