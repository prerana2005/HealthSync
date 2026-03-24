package com.healthsync.model;

import jakarta.persistence.*;

@Entity
@Table(name = "departments")
public class Department {

    @Id
    @Column(name = "dept_id", length = 10)
    private String deptId;

    @Column(name = "dept_name", nullable = false, unique = true, length = 100)
    private String deptName;

    @ManyToOne
    @JoinColumn(name = "hod_user_id")
    private User hod;

    // Getters and Setters
    public String getDeptId() { return deptId; }
    public void setDeptId(String deptId) { this.deptId = deptId; }
    public String getDeptName() { return deptName; }
    public void setDeptName(String deptName) { this.deptName = deptName; }
    public User getHod() { return hod; }
    public void setHod(User hod) { this.hod = hod; }
}
