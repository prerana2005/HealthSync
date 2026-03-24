package com.healthsync.repository;

import com.healthsync.model.Prescription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;

public interface PrescriptionRepository extends JpaRepository<Prescription, String> {
    List<Prescription> findByPatientPatientId(String patientId);
    List<Prescription> findByStatus(Prescription.PrescriptionStatus status);

    @Query("SELECT COALESCE(MAX(CAST(SUBSTRING(p.prescriptionId, 4) AS int)), 0) FROM Prescription p")
    int findMaxPrescriptionId();
}
