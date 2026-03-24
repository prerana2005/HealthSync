package com.healthsync.repository;

import com.healthsync.model.PharmacyInventory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;

public interface PharmacyRepository extends JpaRepository<PharmacyInventory, String> {
    @Query("SELECT p FROM PharmacyInventory p WHERE p.stockQuantity <= p.reorderLevel ORDER BY p.stockQuantity ASC")
    List<PharmacyInventory> findLowStock();

    List<PharmacyInventory> findByMedicineNameContainingIgnoreCase(String name);

    @Query("SELECT COALESCE(MAX(CAST(SUBSTRING(p.medicineId, 4) AS int)), 0) FROM PharmacyInventory p")
    int findMaxMedicineId();
}
