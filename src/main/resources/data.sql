-- ============================================================
-- HealthSync Hospital Management System — Seed Data for H2
-- ============================================================

-- Departments
INSERT INTO departments (dept_id, dept_name) VALUES ('DEPT001', 'Cardiology');
INSERT INTO departments (dept_id, dept_name) VALUES ('DEPT002', 'Neurology');
INSERT INTO departments (dept_id, dept_name) VALUES ('DEPT003', 'Orthopedics');
INSERT INTO departments (dept_id, dept_name) VALUES ('DEPT004', 'General Medicine');
INSERT INTO departments (dept_id, dept_name) VALUES ('DEPT005', 'Pharmacy');

-- Users (passwords hashed with SHA-256)
-- admin@123  → 7676aaafb027c825bd9abab78b234070e702752f625b752e55e55b48e607e358
-- doc@123    → 05da31d4724854c42409e55c4e7f8e3eb3de79f08535851012a518aca15e8cc5
-- pat@123    → 573c5fe3730f043b0c85d0c8fa87aacd5c3e4bef144d96a4cb5863e2d879fb33
-- nurse@123  → 989e512027d206532eebf3e42996e53bb7801ae645ff9479dca395ee674dc9df

INSERT INTO users (user_id, full_name, email, phone, password_hash, role_type, is_active, created_at) VALUES
('USR00001', 'Admin User',      'admin@healthsync.in',    '9000000001', '7676aaafb027c825bd9abab78b234070e702752f625b752e55e55b48e607e358', 'ADMIN',   TRUE, CURRENT_TIMESTAMP);
INSERT INTO users (user_id, full_name, email, phone, password_hash, role_type, is_active, created_at) VALUES
('USR00002', 'Dr. Ananya Rao',  'ananya@healthsync.in',   '9000000002', '05da31d4724854c42409e55c4e7f8e3eb3de79f08535851012a518aca15e8cc5', 'DOCTOR',  TRUE, CURRENT_TIMESTAMP);
INSERT INTO users (user_id, full_name, email, phone, password_hash, role_type, is_active, created_at) VALUES
('USR00003', 'Dr. Kiran Mehta', 'kiran@healthsync.in',    '9000000003', '05da31d4724854c42409e55c4e7f8e3eb3de79f08535851012a518aca15e8cc5', 'DOCTOR',  TRUE, CURRENT_TIMESTAMP);
INSERT INTO users (user_id, full_name, email, phone, password_hash, role_type, is_active, created_at) VALUES
('USR00004', 'Priya Sharma',    'priya@healthsync.in',    '9000000004', '573c5fe3730f043b0c85d0c8fa87aacd5c3e4bef144d96a4cb5863e2d879fb33', 'PATIENT', TRUE, CURRENT_TIMESTAMP);
INSERT INTO users (user_id, full_name, email, phone, password_hash, role_type, is_active, created_at) VALUES
('USR00005', 'Nurse Kavitha',   'kavitha@healthsync.in',  '9000000005', '989e512027d206532eebf3e42996e53bb7801ae645ff9479dca395ee674dc9df', 'NURSE',   TRUE, CURRENT_TIMESTAMP);

-- Doctors
INSERT INTO doctors (doctor_id, user_id, dept_id, specialization, consultation_fee, availability_status, qualification, experience_years) VALUES
('DOC00001', 'USR00002', 'DEPT001', 'Cardiologist', 800.00, 'AVAILABLE', 'MD Cardiology, MBBS', 12);
INSERT INTO doctors (doctor_id, user_id, dept_id, specialization, consultation_fee, availability_status, qualification, experience_years) VALUES
('DOC00002', 'USR00003', 'DEPT002', 'Neurologist', 900.00, 'AVAILABLE', 'DM Neurology, MBBS', 8);

-- Update HOD
UPDATE departments SET hod_user_id = 'USR00002' WHERE dept_id = 'DEPT001';
UPDATE departments SET hod_user_id = 'USR00003' WHERE dept_id = 'DEPT002';

-- Patient
INSERT INTO patients (patient_id, user_id, blood_group, date_of_birth, emergency_contact, medical_history, current_ailment) VALUES
('PAT00001', 'USR00004', 'O+', '1995-06-15', '9000000099', 'No significant past medical history', 'Routine Checkup');

-- Pharmacy inventory
INSERT INTO pharmacy_inventory (medicine_id, medicine_name, generic_name, category, stock_quantity, unit_price, expiry_date, reorder_level, supplier, last_updated) VALUES
('MED00001', 'Paracetamol 500mg', 'Acetaminophen', 'Analgesic', 200, 2.50,  '2027-01-01', 20, 'Sun Pharma', CURRENT_TIMESTAMP);
INSERT INTO pharmacy_inventory (medicine_id, medicine_name, generic_name, category, stock_quantity, unit_price, expiry_date, reorder_level, supplier, last_updated) VALUES
('MED00002', 'Amoxicillin 250mg', 'Amoxicillin', 'Antibiotic', 150, 8.00,  '2026-06-01', 15, 'Cipla Ltd', CURRENT_TIMESTAMP);
INSERT INTO pharmacy_inventory (medicine_id, medicine_name, generic_name, category, stock_quantity, unit_price, expiry_date, reorder_level, supplier, last_updated) VALUES
('MED00003', 'Aspirin 75mg', 'Aspirin', 'Antiplatelet', 300, 1.00,  '2027-03-01', 25, 'Dr. Reddys', CURRENT_TIMESTAMP);
INSERT INTO pharmacy_inventory (medicine_id, medicine_name, generic_name, category, stock_quantity, unit_price, expiry_date, reorder_level, supplier, last_updated) VALUES
('MED00004', 'Metformin 500mg', 'Metformin', 'Antidiabetic', 5, 3.50,  '2027-06-01', 20, 'Lupin Ltd', CURRENT_TIMESTAMP);
INSERT INTO pharmacy_inventory (medicine_id, medicine_name, generic_name, category, stock_quantity, unit_price, expiry_date, reorder_level, supplier, last_updated) VALUES
('MED00005', 'Atorvastatin 10mg', 'Atorvastatin', 'Statin', 8, 5.00,  '2027-09-01', 15, 'Torrent Pharma', CURRENT_TIMESTAMP);

-- Doctor availability slots (for next few days)
INSERT INTO doctor_availability_slots (doctor_id, slot_date, start_time, end_time, is_booked) VALUES
('DOC00001', CURRENT_DATE, '09:00:00', '09:30:00', FALSE);
INSERT INTO doctor_availability_slots (doctor_id, slot_date, start_time, end_time, is_booked) VALUES
('DOC00001', CURRENT_DATE, '09:30:00', '10:00:00', FALSE);
INSERT INTO doctor_availability_slots (doctor_id, slot_date, start_time, end_time, is_booked) VALUES
('DOC00001', CURRENT_DATE, '10:00:00', '10:30:00', FALSE);
INSERT INTO doctor_availability_slots (doctor_id, slot_date, start_time, end_time, is_booked) VALUES
('DOC00001', CURRENT_DATE, '10:30:00', '11:00:00', FALSE);
INSERT INTO doctor_availability_slots (doctor_id, slot_date, start_time, end_time, is_booked) VALUES
('DOC00001', CURRENT_DATE, '11:00:00', '11:30:00', FALSE);
INSERT INTO doctor_availability_slots (doctor_id, slot_date, start_time, end_time, is_booked) VALUES
('DOC00002', CURRENT_DATE, '14:00:00', '14:30:00', FALSE);
INSERT INTO doctor_availability_slots (doctor_id, slot_date, start_time, end_time, is_booked) VALUES
('DOC00002', CURRENT_DATE, '14:30:00', '15:00:00', FALSE);
INSERT INTO doctor_availability_slots (doctor_id, slot_date, start_time, end_time, is_booked) VALUES
('DOC00002', CURRENT_DATE, '15:00:00', '15:30:00', FALSE);
INSERT INTO doctor_availability_slots (doctor_id, slot_date, start_time, end_time, is_booked) VALUES
('DOC00002', CURRENT_DATE, '15:30:00', '16:00:00', FALSE);

-- Sample appointment
INSERT INTO appointments (appointment_id, patient_id, doctor_id, slot_id, appointment_date, time_slot, status, booking_time) VALUES
('APT00001', 'PAT00001', 'DOC00001', 1, CURRENT_DATE, '09:00:00', 'SCHEDULED', CURRENT_TIMESTAMP);
UPDATE doctor_availability_slots SET is_booked = TRUE WHERE slot_id = 1;

-- Sample medical record
INSERT INTO medical_records (record_id, patient_id, doctor_id, appointment_id, diagnosis_notes, prescription_details, status, date_created, last_updated) VALUES
('REC00001', 'PAT00001', 'DOC00001', 'APT00001', 'Routine cardiac evaluation. Heart sounds normal. ECG normal sinus rhythm.', 'Tab. Aspirin 75mg once daily', 'OPEN', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- Sample invoice
INSERT INTO invoices (bill_id, patient_id, appointment_id, consultation_fee, medication_cost, lab_charges, room_charges, tax_amount, payment_status, insurance_claim, generated_date, generated_by) VALUES
('BILL0001', 'PAT00001', 'APT00001', 800.00, 75.00, 0.00, 0.00, 131.25, 'PENDING', 0.00, CURRENT_TIMESTAMP, 'USR00001');

-- Sample notifications
INSERT INTO notifications (user_id, notif_type, title, message, is_read, sent_at) VALUES
('USR00004', 'APPOINTMENT', 'Appointment Confirmed', 'Your appointment with Dr. Ananya Rao has been scheduled for today at 09:00 AM.', FALSE, CURRENT_TIMESTAMP);
INSERT INTO notifications (user_id, notif_type, title, message, is_read, sent_at) VALUES
('USR00004', 'GENERAL', 'Welcome to HealthSync', 'Welcome to HealthSync Hospital Management System! Your patient profile has been created.', TRUE, CURRENT_TIMESTAMP);
