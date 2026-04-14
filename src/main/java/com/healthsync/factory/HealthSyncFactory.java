package com.healthsync.factory;

import com.healthsync.model.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

/**
 * HealthSyncFactory — Creational Design Pattern: FACTORY METHOD
 * =====================================================================
 *
 * PATTERN: Factory Method (Creational)
 *
 * INTENT: Define an interface for creating objects, but let the factory
 * decide which class to instantiate. Removes object creation logic from
 * controllers and centralises it here.
 *
 * WHY IT IS NEEDED HERE:
 * Controllers (AuthController, BillingController, etc.) were directly
 * instantiating domain objects with scattered "new Entity()" calls and
 * String.format() ID generation. This:
 *   1. Violates the Creator GRASP principle — only one class should know
 *      how to build each entity.
 *   2. Makes ID-generation logic hard to maintain (duplicated across
 *      AuthController, BillingController, PharmacyController, etc.)
 *   3. Makes unit testing harder — cannot mock object creation.
 *
 * HOW IT SOLVES THE PROBLEM:
 * The factory centralises all "new Entity + ID" logic. Controllers call
 * factory methods and receive a ready-to-persist object. Adding a new
 * entity type only requires a new factory method — existing controllers
 * are not changed (Open/Closed Principle).
 *
 * SOLID: SRP — factory is the single place responsible for entity creation.
 * SOLID: OCP — extend by adding methods, never by modifying existing ones.
 * GRASP: Creator — factory has the initialisation data; it creates objects.
 * =====================================================================
 */
public class HealthSyncFactory {

    private HealthSyncFactory() { /* utility class, no instances */ }

    // ─── Patient ────────────────────────────────────────────────────────

    /**
     * Creates a Patient linked to an existing User.
     *
     * @param nextPatientNumber sequence number from patientRepo.findMaxPatientId()
     * @param user              pre-persisted User entity
     * @param bloodGroup        e.g. "O+"
     * @param emergencyContact  phone number string
     */
    public static Patient createPatient(int nextPatientNumber, User user,
                                        String bloodGroup, String emergencyContact) {
        Patient patient = new Patient();
        patient.setPatientId(String.format("PAT%05d", nextPatientNumber));
        patient.setUser(user);
        patient.setBloodGroup(bloodGroup != null ? bloodGroup : "");
        patient.setEmergencyContact(emergencyContact != null ? emergencyContact : "");
        return patient;
    }

    // ─── User ────────────────────────────────────────────────────────────

    /**
     * Creates a User entity (not yet persisted).
     *
     * @param nextUserNumber sequence from userRepo.findMaxUserId()
     * @param fullName       display name
     * @param email          unique email
     * @param phone          contact number
     * @param passwordHash   SHA-256 hex string
     * @param role           RoleType enum value
     */
    public static User createUser(int nextUserNumber, String fullName, String email,
                                   String phone, String passwordHash, User.RoleType role) {
        User user = new User();
        user.setUserId(String.format("USR%05d", nextUserNumber));
        user.setFullName(fullName);
        user.setEmail(email);
        user.setPhone(phone);
        user.setPasswordHash(passwordHash);
        user.setRoleType(role);
        user.setIsActive(true);
        return user;
    }

    // ─── Invoice ─────────────────────────────────────────────────────────

    /**
     * Creates an Invoice from a map of billing fields.
     * Replicates the logic that was scattered in BillingController.create().
     *
     * @param nextBillNumber sequence from invoiceRepo.findMaxBillId()
     * @param patient        linked Patient
     * @param fields         map with keys: consultationFee, medicationCost,
     *                       labCharges, roomCharges, taxAmount, generatedBy
     */
    public static Invoice createInvoice(int nextBillNumber, Patient patient,
                                         Map<String, String> fields) {
        Invoice inv = new Invoice();
        inv.setBillId(String.format("BILL%04d", nextBillNumber));
        inv.setPatient(patient);
        inv.setConsultationFee(decimal(fields, "consultationFee"));
        inv.setMedicationCost(decimal(fields, "medicationCost"));
        inv.setLabCharges(decimal(fields, "labCharges"));
        inv.setRoomCharges(decimal(fields, "roomCharges"));
        inv.setTaxAmount(decimal(fields, "taxAmount"));
        inv.setPaymentStatus(Invoice.PaymentStatus.PENDING);
        inv.setGeneratedBy(fields.getOrDefault("generatedBy", ""));
        return inv;
    }

    // ─── PharmacyInventory ───────────────────────────────────────────────

    /**
     * Creates a PharmacyInventory item.
     *
     * @param nextMedNumber sequence from pharmacyRepo.findMaxMedicineId()
     * @param fields        map with keys: medicineName, genericName, category,
     *                      stockQuantity, unitPrice, expiryDate, reorderLevel, supplier
     */
    public static PharmacyInventory createMedicine(int nextMedNumber,
                                                    Map<String, String> fields) {
        PharmacyInventory med = new PharmacyInventory();
        med.setMedicineId(String.format("MED%05d", nextMedNumber));
        med.setMedicineName(fields.get("medicineName"));
        med.setGenericName(fields.getOrDefault("genericName", ""));
        med.setCategory(fields.getOrDefault("category", ""));
        med.setStockQuantity(intVal(fields, "stockQuantity", 0));
        med.setUnitPrice(decimal(fields, "unitPrice"));
        med.setExpiryDate(LocalDate.parse(fields.get("expiryDate")));
        med.setReorderLevel(intVal(fields, "reorderLevel", 10));
        med.setSupplier(fields.getOrDefault("supplier", ""));
        return med;
    }

    // ─── MedicalRecord ───────────────────────────────────────────────────

    /**
     * Creates a MedicalRecord.
     *
     * @param nextRecordNumber sequence from recordRepo.findMaxRecordId()
     * @param patient          linked Patient
     * @param doctor           linked Doctor
     * @param fields           map with keys: diagnosisNotes, labResults, prescriptionDetails
     */
    public static MedicalRecord createMedicalRecord(int nextRecordNumber,
                                                     Patient patient, Doctor doctor,
                                                     Map<String, String> fields) {
        MedicalRecord rec = new MedicalRecord();
        rec.setRecordId(String.format("REC%05d", nextRecordNumber));
        rec.setPatient(patient);
        rec.setDoctor(doctor);
        rec.setDiagnosisNotes(fields.getOrDefault("diagnosisNotes", ""));
        rec.setLabResults(fields.getOrDefault("labResults", ""));
        rec.setPrescriptionDetails(fields.getOrDefault("prescriptionDetails", ""));
        rec.setStatus(MedicalRecord.RecordStatus.OPEN);
        return rec;
    }

    // ─── Helpers ─────────────────────────────────────────────────────────

    private static BigDecimal decimal(Map<String, String> m, String key) {
        String v = m.getOrDefault(key, "0");
        try { return new BigDecimal(v); } catch (NumberFormatException e) { return BigDecimal.ZERO; }
    }

    private static int intVal(Map<String, String> m, String key, int def) {
        try { return Integer.parseInt(m.getOrDefault(key, String.valueOf(def))); }
        catch (NumberFormatException e) { return def; }
    }
}
