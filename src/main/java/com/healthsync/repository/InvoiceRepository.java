package com.healthsync.repository;

import com.healthsync.model.Invoice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;

public interface InvoiceRepository extends JpaRepository<Invoice, String> {
    List<Invoice> findByPatientPatientId(String patientId);
    List<Invoice> findByPaymentStatus(Invoice.PaymentStatus status);

    @Query("SELECT COALESCE(MAX(CAST(SUBSTRING(i.billId, 5) AS int)), 0) FROM Invoice i")
    int findMaxBillId();
}
