package com.healthsync.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "lab_tests")
public class LabTest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "test_id")
    private Integer testId;

    @ManyToOne
    @JoinColumn(name = "record_id", nullable = false)
    private MedicalRecord record;

    @Column(name = "test_name", nullable = false, length = 150)
    private String testName;

    @ManyToOne
    @JoinColumn(name = "ordered_by", nullable = false)
    private Doctor orderedBy;

    @Column(name = "ordered_date", nullable = false)
    private LocalDateTime orderedDate;

    @Column(name = "result_summary", columnDefinition = "TEXT")
    private String resultSummary;

    @Column(name = "result_date")
    private LocalDateTime resultDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private LabTestStatus status = LabTestStatus.ORDERED;

    @Column(name = "notification_sent", nullable = false)
    private Boolean notificationSent = false;

    public enum LabTestStatus {
        ORDERED, IN_PROGRESS, COMPLETED, CANCELLED
    }

    @PrePersist
    protected void onCreate() {
        if (orderedDate == null) orderedDate = LocalDateTime.now();
    }

    // Getters and Setters
    public Integer getTestId() { return testId; }
    public void setTestId(Integer testId) { this.testId = testId; }
    public MedicalRecord getRecord() { return record; }
    public void setRecord(MedicalRecord record) { this.record = record; }
    public String getTestName() { return testName; }
    public void setTestName(String testName) { this.testName = testName; }
    public Doctor getOrderedBy() { return orderedBy; }
    public void setOrderedBy(Doctor orderedBy) { this.orderedBy = orderedBy; }
    public LocalDateTime getOrderedDate() { return orderedDate; }
    public void setOrderedDate(LocalDateTime orderedDate) { this.orderedDate = orderedDate; }
    public String getResultSummary() { return resultSummary; }
    public void setResultSummary(String resultSummary) { this.resultSummary = resultSummary; }
    public LocalDateTime getResultDate() { return resultDate; }
    public void setResultDate(LocalDateTime resultDate) { this.resultDate = resultDate; }
    public LabTestStatus getStatus() { return status; }
    public void setStatus(LabTestStatus status) { this.status = status; }
    public Boolean getNotificationSent() { return notificationSent; }
    public void setNotificationSent(Boolean notificationSent) { this.notificationSent = notificationSent; }
}
