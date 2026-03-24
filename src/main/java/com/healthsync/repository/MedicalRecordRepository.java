package com.healthsync.repository;

import com.healthsync.model.MedicalRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;

public interface MedicalRecordRepository extends JpaRepository<MedicalRecord, String> {
    List<MedicalRecord> findByPatientPatientId(String patientId);
    List<MedicalRecord> findByDoctorDoctorId(String doctorId);

    @Query("SELECT COALESCE(MAX(CAST(SUBSTRING(m.recordId, 4) AS int)), 0) FROM MedicalRecord m")
    int findMaxRecordId();
}
