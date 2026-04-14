package com.healthsync.model;

import jakarta.persistence.*;

/**
 * Staff — represents a hospital staff member (nurse, admin staff, etc.).
 * From the OOAD class diagram: staffId, designation, shiftTiming.
 *
 * Inherits identity from User (1:1 relationship).
 * SOLID SRP: Staff only holds staff-specific attributes.
 * GRASP Information Expert: Staff knows its own designation and shift.
 */
@Entity
@Table(name = "staff")
public class Staff {

    @Id
    @Column(name = "staff_id", length = 10)
    private String staffId;

    @OneToOne
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(name = "designation", nullable = false, length = 100)
    private String designation;

    @Column(name = "shift_timing", length = 50)
    private String shiftTiming;

    // Getters and Setters
    public String getStaffId() { return staffId; }
    public void setStaffId(String staffId) { this.staffId = staffId; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public String getDesignation() { return designation; }
    public void setDesignation(String designation) { this.designation = designation; }
    public String getShiftTiming() { return shiftTiming; }
    public void setShiftTiming(String shiftTiming) { this.shiftTiming = shiftTiming; }
}
