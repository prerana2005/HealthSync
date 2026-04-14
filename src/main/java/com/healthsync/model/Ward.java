package com.healthsync.model;

import jakarta.persistence.*;

/**
 * Ward — represents a physical hospital ward.
 * Present in the OOAD class diagram; previously missing from implementation.
 *
 * GRASP: Information Expert — Ward knows its own bed counts and type.
 */
@Entity
@Table(name = "wards")
public class Ward {

    @Id
    @Column(name = "ward_id", length = 10)
    private String wardId;

    @Column(name = "ward_name", nullable = false, length = 100)
    private String wardName;

    @Enumerated(EnumType.STRING)
    @Column(name = "ward_type", nullable = false)
    private WardType wardType;

    @Column(name = "total_beds", nullable = false)
    private Integer totalBeds = 0;

    @Column(name = "available_beds", nullable = false)
    private Integer availableBeds = 0;

    @Column(name = "floor_number")
    private Integer floorNumber;

    public enum WardType {
        GENERAL, ICU, PRIVATE, EMERGENCY
    }

    // Getters and Setters
    public String getWardId() { return wardId; }
    public void setWardId(String wardId) { this.wardId = wardId; }
    public String getWardName() { return wardName; }
    public void setWardName(String wardName) { this.wardName = wardName; }
    public WardType getWardType() { return wardType; }
    public void setWardType(WardType wardType) { this.wardType = wardType; }
    public Integer getTotalBeds() { return totalBeds; }
    public void setTotalBeds(Integer totalBeds) { this.totalBeds = totalBeds; }
    public Integer getAvailableBeds() { return availableBeds; }
    public void setAvailableBeds(Integer availableBeds) { this.availableBeds = availableBeds; }
    public Integer getFloorNumber() { return floorNumber; }
    public void setFloorNumber(Integer floorNumber) { this.floorNumber = floorNumber; }

    /**
     * Information Expert: Ward itself knows if a bed is available.
     */
    public boolean checkAvailability() {
        return availableBeds > 0;
    }

    /**
     * Called when a patient is allotted — decrements available beds.
     */
    public void allotBed() {
        if (availableBeds > 0) availableBeds--;
    }

    /**
     * Called when a patient is discharged — increments available beds.
     */
    public void freeBed() {
        if (availableBeds < totalBeds) availableBeds++;
    }
}
