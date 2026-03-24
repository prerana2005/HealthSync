package com.healthsync.repository;

import com.healthsync.model.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface PaymentRepository extends JpaRepository<Payment, Integer> {
    List<Payment> findByInvoiceBillId(String billId);
}
