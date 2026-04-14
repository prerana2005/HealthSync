package com.healthsync.repository;

import com.healthsync.model.WardAllotment;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface WardAllotmentRepository extends JpaRepository<WardAllotment, Integer> {
    List<WardAllotment> findByPatientPatientId(String patientId);
    List<WardAllotment> findByWardWardId(String wardId);
    List<WardAllotment> findByStatus(WardAllotment.AllotmentStatus status);
}
