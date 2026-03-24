package com.healthsync.controller;

import com.healthsync.model.*;
import com.healthsync.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

@RestController
@RequestMapping("/api/pharmacy")
@CrossOrigin(origins = "*")
public class PharmacyController {

    @Autowired private PharmacyRepository pharmacyRepo;

    @GetMapping
    public List<PharmacyInventory> getAll() {
        return pharmacyRepo.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getById(@PathVariable String id) {
        return pharmacyRepo.findById(id)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/low-stock")
    public List<PharmacyInventory> lowStock() {
        return pharmacyRepo.findLowStock();
    }

    @GetMapping("/search")
    public List<PharmacyInventory> search(@RequestParam String q) {
        return pharmacyRepo.findByMedicineNameContainingIgnoreCase(q);
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody Map<String, String> body) {
        try {
            int nextId = pharmacyRepo.findMaxMedicineId() + 1;
            PharmacyInventory med = new PharmacyInventory();
            med.setMedicineId(String.format("MED%05d", nextId));
            med.setMedicineName(body.get("medicineName"));
            med.setGenericName(body.getOrDefault("genericName", ""));
            med.setCategory(body.getOrDefault("category", ""));
            med.setStockQuantity(Integer.parseInt(body.getOrDefault("stockQuantity", "0")));
            med.setUnitPrice(new BigDecimal(body.getOrDefault("unitPrice", "0")));
            med.setExpiryDate(LocalDate.parse(body.get("expiryDate")));
            med.setReorderLevel(Integer.parseInt(body.getOrDefault("reorderLevel", "10")));
            med.setSupplier(body.getOrDefault("supplier", ""));
            pharmacyRepo.save(med);
            return ResponseEntity.ok(med);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable String id, @RequestBody Map<String, String> body) {
        return pharmacyRepo.findById(id).map(med -> {
            if (body.containsKey("medicineName")) med.setMedicineName(body.get("medicineName"));
            if (body.containsKey("stockQuantity")) med.setStockQuantity(Integer.parseInt(body.get("stockQuantity")));
            if (body.containsKey("unitPrice")) med.setUnitPrice(new BigDecimal(body.get("unitPrice")));
            if (body.containsKey("reorderLevel")) med.setReorderLevel(Integer.parseInt(body.get("reorderLevel")));
            if (body.containsKey("supplier")) med.setSupplier(body.get("supplier"));
            pharmacyRepo.save(med);
            return ResponseEntity.ok(med);
        }).orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}/restock")
    public ResponseEntity<?> restock(@PathVariable String id, @RequestBody Map<String, String> body) {
        return pharmacyRepo.findById(id).map(med -> {
            int qty = Integer.parseInt(body.getOrDefault("quantity", "0"));
            med.setStockQuantity(med.getStockQuantity() + qty);
            pharmacyRepo.save(med);
            return ResponseEntity.ok(med);
        }).orElse(ResponseEntity.notFound().build());
    }
}
