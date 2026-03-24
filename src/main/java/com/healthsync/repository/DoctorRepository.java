package com.healthsync.repository;

import com.healthsync.model.Doctor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.Optional;
import java.util.List;

public interface DoctorRepository extends JpaRepository<Doctor, String> {
    Optional<Doctor> findByUserUserId(String userId);
    List<Doctor> findByDepartmentDeptId(String deptId);
    List<Doctor> findBySpecializationContainingIgnoreCase(String specialization);
    List<Doctor> findByAvailabilityStatus(Doctor.AvailabilityStatus status);

    @Query("SELECT d FROM Doctor d JOIN d.user u WHERE LOWER(u.fullName) LIKE LOWER(CONCAT('%',:q,'%')) OR LOWER(d.specialization) LIKE LOWER(CONCAT('%',:q,'%'))")
    List<Doctor> search(String q);

    @Query("SELECT COALESCE(MAX(CAST(SUBSTRING(d.doctorId, 4) AS int)), 0) FROM Doctor d")
    int findMaxDoctorId();
}
