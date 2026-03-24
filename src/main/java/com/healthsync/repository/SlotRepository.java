package com.healthsync.repository;

import com.healthsync.model.DoctorAvailabilitySlot;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.util.List;

public interface SlotRepository extends JpaRepository<DoctorAvailabilitySlot, Integer> {
    List<DoctorAvailabilitySlot> findByDoctorDoctorIdAndSlotDateAndIsBookedFalse(String doctorId, LocalDate date);
    List<DoctorAvailabilitySlot> findByDoctorDoctorIdAndSlotDate(String doctorId, LocalDate date);
    List<DoctorAvailabilitySlot> findByDoctorDoctorIdAndSlotDateGreaterThanEqualAndIsBookedFalse(String doctorId, LocalDate date);
}
