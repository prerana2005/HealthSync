<div align="center">

# 🏥 HealthSync
### Hospital Management System

**A full-stack, role-based hospital management platform built with Java Spring Boot & Vanilla JavaScript**

![Java](https://img.shields.io/badge/Java-17-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.2.4-6DB33F?style=for-the-badge&logo=spring-boot&logoColor=white)
![H2](https://img.shields.io/badge/H2_Database-In--Memory-blue?style=for-the-badge&logo=databricks&logoColor=white)
![JavaScript](https://img.shields.io/badge/JavaScript-Vanilla_SPA-F7DF1E?style=for-the-badge&logo=javascript&logoColor=black)
![Maven](https://img.shields.io/badge/Maven-Build-C71A36?style=for-the-badge&logo=apache-maven&logoColor=white)

---

*UE23CS352B · Object Oriented Analysis and Design*
**Yuti Naha · Yuvika T · Vaishnavi JP · Prerana M P**

</div>

---

## 📖 About

HealthSync is an integrated Hospital Management System that automates the clinical and administrative workflows of a modern hospital. Built as part of an **OOAD course project**, it demonstrates real-world application of design patterns, SOLID & GRASP principles, and a clean MVC architecture.

> In a bustling healthcare environment, manual record-keeping leads to data silos, scheduling conflicts, and delayed patient care. HealthSync solves this by providing a **single unified platform** for every hospital stakeholder.

---

## ✨ Features

### 🔑 Core Modules

| Module | Description |
|--------|-------------|
| 🔐 **User Authentication & RBAC** | Secure SHA-256 login with 4 distinct roles — Admin, Doctor, Patient, Staff |
| 📅 **Appointment Scheduling** | Real-time doctor availability, slot booking, conflict prevention |
| 🗂️ **Electronic Medical Records** | Create, update, and close patient EMRs with diagnosis notes |
| 💊 **E-Prescription Management** | Issue prescriptions, send to pharmacy, track dispensing |
| 🧪 **Lab Tests & Notifications** | Order tests, update results, auto-notify patients via Observer pattern |
| 💰 **Billing & Insurance** | Auto-invoice generation, multi-method payments, insurance claims |
| 💊 **Pharmacy Inventory** | 44-item medicine catalog, auto-fill, low-stock alerts, 16 suppliers |
| 🛏️ **Ward & Bed Management** | 4 ward types, bed allotment/discharge with live occupancy tracking |
| 🔔 **Real-time Notifications** | Bell indicator with 30-second polling, mark read/unread |
| 📋 **Audit Logging** | Singleton-based audit trail for all critical system actions |

---

## 🏗️ Architecture & Design Patterns

```
┌─────────────────────────────────────────────────────────┐
│                    VIEW LAYER                           │
│         index.html  +  app.js  (Vanilla SPA)            │
│   Role-based nav · Dynamic modals · Fetch API calls     │
└────────────────────────┬────────────────────────────────┘
                         │ HTTP / JSON
┌────────────────────────▼────────────────────────────────┐
│                 CONTROLLER LAYER                        │
│         Spring @RestController  +  RoleGuard            │
│   AuthController · AppointmentController · Billing...   │
└────────────────────────┬────────────────────────────────┘
                         │
┌────────────────────────▼────────────────────────────────┐
│              SERVICE / FACADE LAYER                     │
│    AppointmentService  ·  BillingFacade (Facade DP)     │
│    HealthSyncFactory (Factory)  ·  AuditLogService      │
│    NotificationObserver (Observer DP)                   │
└────────────────────────┬────────────────────────────────┘
                         │
┌────────────────────────▼────────────────────────────────┐
│               MODEL / PERSISTENCE LAYER                 │
│     18 @Entity classes  ·  Spring Data JPA              │
│         H2 In-Memory Database (18 tables)               │
└─────────────────────────────────────────────────────────┘
```

### 🎨 Design Patterns Implemented

| Pattern | File | How It's Used |
|---------|------|---------------|
| 🏭 **Factory Method** | `factory/HealthSyncFactory.java` | Creates all domain objects (User, Patient, Invoice, Medicine, MedicalRecord) with formatted IDs like `PAT00001`, `BILL0001` |
| 🔒 **Singleton** | `singleton/AuditLogService.java` | Single audit logger instance across the entire application — logs every critical action |
| 🏛️ **Facade** | `facade/BillingFacade.java` | Hides billing complexity — `BillingController` only calls `generateInvoice()` and `processPayment()` |
| 👁️ **Observer** | `observer/NotificationObserver.java` | Fires notifications automatically on appointment booking, lab results, and payment events |

---

## 👥 Roles & Demo Credentials

| Role | Email | Password | Access |
|------|-------|----------|--------|
| 👑 **Administrator** | `admin@healthsync.in` | `admin@123` | Full system access |
| 🩺 **Doctor** | `ananya@healthsync.in` | `doc@123` | Patients, Records, Lab Tests, Prescriptions |
| 🧑‍⚕️ **Doctor 2** | `kiran@healthsync.in` | `doc@123` | Neurology — secondary demo account |
| 🤒 **Patient** | `priya@healthsync.in` | `pat@123` | Appointments, Records (read), Bills, Notifications |
| 👩‍⚕️ **Staff / Nurse** | `kavitha@healthsync.in` | `nurse@123` | Appointments, Wards, Pharmacy, Prescriptions |

---

## 🚀 Getting Started

### Prerequisites

- **Java 17+** — [Download](https://adoptium.net/)
- **Apache Maven 3.8+** — [Download](https://maven.apache.org/)
- A modern web browser (Chrome / Firefox / Edge)

### Run the Application

```bash
# 1. Clone the repository
git clone https://github.com/your-username/HealthSync.git
cd HealthSync

# 2. Build and run
mvn spring-boot:run

# 3. Open your browser
open http://localhost:8080
```

> The H2 in-memory database auto-populates with seed data on every startup — no database setup required.

### Access the H2 Database Console

```
URL:      http://localhost:8080/h2-console
JDBC URL: jdbc:h2:mem:healthsync
Username: sa
Password: (leave blank)
```

---

## 📁 Project Structure

```
src/
├── main/
│   ├── java/com/healthsync/
│   │   ├── config/
│   │   │   ├── RoleGuard.java          # Role-based access interceptor
│   │   │   └── WebConfig.java          # CORS & MVC config
│   │   ├── controller/                 # REST API endpoints (10 controllers)
│   │   │   ├── AuthController.java
│   │   │   ├── AppointmentController.java
│   │   │   ├── MedicalRecordController.java
│   │   │   ├── LabTestController.java
│   │   │   ├── PrescriptionController.java
│   │   │   ├── BillingController.java
│   │   │   ├── PharmacyController.java
│   │   │   ├── WardController.java
│   │   │   ├── PatientController.java
│   │   │   └── DoctorController.java
│   │   ├── facade/
│   │   │   └── BillingFacade.java      # Facade Pattern
│   │   ├── factory/
│   │   │   └── HealthSyncFactory.java  # Factory Method Pattern
│   │   ├── model/                      # 18 JPA @Entity classes
│   │   │   ├── User.java · Doctor.java · Patient.java · Staff.java
│   │   │   ├── Appointment.java · DoctorAvailabilitySlot.java
│   │   │   ├── MedicalRecord.java · LabTest.java · Prescription.java
│   │   │   ├── Invoice.java · Payment.java · PharmacyInventory.java
│   │   │   ├── Ward.java · WardAllotment.java · Notification.java
│   │   │   └── AuditLog.java · Department.java · PrescriptionItem.java
│   │   ├── observer/
│   │   │   ├── NotificationObserver.java           # Observer interface
│   │   │   └── PersistentNotificationObserver.java # Concrete observer
│   │   ├── repository/                 # Spring Data JPA repositories
│   │   ├── service/
│   │   │   └── AppointmentService.java
│   │   └── singleton/
│   │       └── AuditLogService.java    # Singleton Pattern
│   └── resources/
│       ├── application.properties
│       ├── data.sql                    # Seed data (users, slots, medicines)
│       └── static/
│           ├── index.html              # Single-page application shell
│           └── js/
│               └── app.js             # All frontend logic (~1500 lines)
```

---

## 🔌 API Reference

<details>
<summary><b>Authentication</b></summary>

| Method | Endpoint | Description |
|--------|----------|-------------|
| `POST` | `/api/auth/login` | Login with email + password |
| `POST` | `/api/auth/register` | Register new user |

</details>

<details>
<summary><b>Appointments</b></summary>

| Method | Endpoint | Description |
|--------|----------|-------------|
| `GET` | `/api/appointments` | List all appointments (role-filtered) |
| `GET` | `/api/appointments/slots` | Available slots for a doctor on a date |
| `POST` | `/api/appointments` | Book new appointment |
| `PUT` | `/api/appointments/{id}/cancel` | Cancel appointment |
| `PUT` | `/api/appointments/{id}/complete` | Mark as completed |

</details>

<details>
<summary><b>Medical Records</b></summary>

| Method | Endpoint | Description |
|--------|----------|-------------|
| `GET` | `/api/records` | All records |
| `POST` | `/api/records` | Create new EMR |
| `PUT` | `/api/records/{id}` | Update / close record |
| `GET` | `/api/records/patient/{id}` | Patient's records |

</details>

<details>
<summary><b>Billing & Insurance</b></summary>

| Method | Endpoint | Description |
|--------|----------|-------------|
| `GET` | `/api/billing` | All invoices |
| `POST` | `/api/billing` | Generate invoice |
| `POST` | `/api/billing/{id}/pay` | Process payment (Cash/Card/UPI/Insurance) |
| `GET` | `/api/billing/pending` | Unpaid invoices |

</details>

<details>
<summary><b>Pharmacy, Lab Tests, Prescriptions, Wards</b></summary>

| Method | Endpoint | Description |
|--------|----------|-------------|
| `GET` | `/api/pharmacy` | Inventory list |
| `GET` | `/api/pharmacy/low-stock` | Below reorder level |
| `POST` | `/api/pharmacy` | Add medicine |
| `POST` | `/api/labtests` | Order lab test |
| `PUT` | `/api/labtests/{id}/complete` | Update result + notify patient |
| `POST` | `/api/prescriptions` | Issue prescription |
| `PUT` | `/api/prescriptions/{id}/dispense` | Dispense + decrement stock |
| `GET` | `/api/wards` | All wards with occupancy |
| `POST` | `/api/wards/allotments` | Admit patient to ward |
| `PUT` | `/api/wards/allotments/{id}/discharge` | Discharge patient |

</details>

---

## 🧩 SOLID & GRASP Principles

<details>
<summary><b>SOLID Principles</b></summary>

| Principle | Implementation |
|-----------|----------------|
| **S**ingle Responsibility | Each controller handles exactly one domain (BillingController → billing only) |
| **O**pen/Closed | `NotificationObserver` interface — add Email/SMS observer without modifying existing code |
| **L**iskov Substitution | `PersistentNotificationObserver` is swappable with any `NotificationObserver` implementation |
| **I**nterface Segregation | `NotificationObserver` has a single focused method `onEvent()` |
| **D**ependency Inversion | `BillingFacade` depends on `NotificationObserver` interface, not concrete class |

</details>

<details>
<summary><b>GRASP Principles</b></summary>

| Principle | Implementation |
|-----------|----------------|
| Information Expert | `Invoice` calculates its own tax; `Ward` knows its own bed availability |
| Creator | `HealthSyncFactory` creates all domain objects (has all required data) |
| Low Coupling | `BillingFacade` shields `BillingController` from Notification/Audit internals |
| High Cohesion | `PharmacyController` does pharmacy only; `WardController` does wards only |
| Controller | All 10 `@RestController` classes act as GRASP controllers |
| Polymorphism | `NotificationObserver` interface enables polymorphic notification strategies |
| Indirection | `BillingFacade` is the indirection layer between controller and subsystem |
| Pure Fabrication | `AuditLogService` and `HealthSyncFactory` — technical classes with no real-world counterpart |

</details>

---

## 🗃️ Database Schema (18 Tables)

```
users ──┬──► doctors ──► doctor_availability_slots
        ├──► patients ──► appointments ──► medical_records ──► lab_tests
        │                                                  └──► prescriptions ──► prescription_items
        ├──► staff ──► ward_allotments ──► wards
        └──► notifications
             audit_logs
             pharmacy_inventory
             invoices ──► payments
             departments
```

---

## 📊 State Machines

| Object | States |
|--------|--------|
| **Appointment** | `SCHEDULED` → `COMPLETED` \| `CANCELLED` \| `NO_SHOW` |
| **Invoice** | `PENDING` → `PAID` \| `CANCELLED` \| `INSURANCE_CLAIMED` \| `PARTIALLY_PAID` |
| **MedicalRecord** | `OPEN` → `CLOSED` *(one-way)* |
| **LabTest** | `ORDERED` → `IN_PROGRESS` → `COMPLETED` \| `CANCELLED` |
| **Prescription** | `PENDING` → `DISPENSED` \| `CANCELLED` |
| **WardAllotment** | `ADMITTED` → `DISCHARGED` |

---

## 🛠️ Tech Stack

| Layer | Technology |
|-------|------------|
| Language | Java 17 |
| Framework | Spring Boot 3.2.4 |
| ORM | Spring Data JPA + Hibernate |
| Database | H2 In-Memory (auto-seeded) |
| Frontend | Vanilla JavaScript SPA, HTML5, Bootstrap 5 |
| Build | Apache Maven |
| Security | SHA-256 password hashing, X-User-Role header guard |

---

## 📸 Screenshots

> Run the app and log in with any of the demo credentials to explore:
> - **Admin** — full dashboard, billing, ward management, user management
<img width="2874" height="1622" alt="image" src="https://github.com/user-attachments/assets/dbb389b9-5a76-429b-b161-7290e4ff8e0a" />

> - **Doctor** — patient list, EMR, lab tests, prescriptions
> - **Patient** — book appointments, view records, pay bills, notification bell
> - **Staff** — appointment booking, ward allotment, pharmacy, prescription dispensing

---

<div align="center">

**UE23CS352B: Object Oriented Analysis and Design**

*PES University · 2024–25*

</div>
