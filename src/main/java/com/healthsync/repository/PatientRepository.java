package com.healthsync.repository;

import com.healthsync.model.Patient;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.Optional;
import java.util.List;

public interface PatientRepository extends JpaRepository<Patient, String> {
    Optional<Patient> findByUserUserId(String userId);

    @Query("SELECT p FROM Patient p JOIN p.user u WHERE LOWER(u.fullName) LIKE LOWER(CONCAT('%',:q,'%')) OR LOWER(u.phone) LIKE LOWER(CONCAT('%',:q,'%')) OR LOWER(p.patientId) LIKE LOWER(CONCAT('%',:q,'%'))")
    List<Patient> search(String q);

    @Query("SELECT COALESCE(MAX(CAST(SUBSTRING(p.patientId, 4) AS int)), 0) FROM Patient p")
    int findMaxPatientId();
}
