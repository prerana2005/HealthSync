package com.healthsync.repository;

import com.healthsync.model.Prescription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;

public interface PrescriptionRepository extends JpaRepository<Prescription, String> {
    List<Prescription> findByPatientPatientId(String patientId);
    List<Prescription> findByStatus(Prescription.PrescriptionStatus status);

    /** NEW — doctor can view all prescriptions they generated (Doctor's Prescriptions page view) */
    List<Prescription> findByDoctorDoctorId(String doctorId);

    @Query("SELECT COALESCE(MAX(CAST(SUBSTRING(p.prescriptionId, 3) AS int)), 0) FROM Prescription p")
    int findMaxPrescriptionId();
}
