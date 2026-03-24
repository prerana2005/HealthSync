package com.healthsync.repository;

import com.healthsync.model.LabTest;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface LabTestRepository extends JpaRepository<LabTest, Integer> {
    List<LabTest> findByRecordRecordId(String recordId);
    List<LabTest> findByStatus(LabTest.LabTestStatus status);
}
