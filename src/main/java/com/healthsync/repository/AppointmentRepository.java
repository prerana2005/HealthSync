package com.healthsync.repository;

import com.healthsync.model.Appointment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.time.LocalDate;
import java.util.List;

public interface AppointmentRepository extends JpaRepository<Appointment, String> {
    List<Appointment> findByPatientPatientId(String patientId);
    List<Appointment> findByDoctorDoctorId(String doctorId);
    List<Appointment> findByAppointmentDate(LocalDate date);
    List<Appointment> findByStatus(Appointment.AppointmentStatus status);
    List<Appointment> findByDoctorDoctorIdAndAppointmentDate(String doctorId, LocalDate date);
    long countByStatus(Appointment.AppointmentStatus status);

    @Query("SELECT COALESCE(MAX(CAST(SUBSTRING(a.appointmentId, 4) AS int)), 0) FROM Appointment a")
    int findMaxAppointmentId();
}
