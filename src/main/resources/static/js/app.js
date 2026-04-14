// =========================================================
// HealthSync — Frontend Application
// API integration + interactivity
// =========================================================

const API = '';  // Same origin, no prefix needed for Spring Boot

let currentUser = null;

// ==================== AUTH ====================

async function handleLogin(e) {
    e.preventDefault();
    const email = document.getElementById('loginEmail').value;
    const password = document.getElementById('loginPassword').value;

    try {
        const res = await fetch(API + '/api/auth/login', {
            method: 'POST',
            headers: roleHeaders({ 'Content-Type': 'application/json' }),
            body: JSON.stringify({ email, password })
        });
        const data = await res.json();
        if (!res.ok) {
            showAlert('loginAlert', data.error || 'Login failed', 'error');
            return false;
        }
        currentUser = data;
        enterApp();
    } catch (err) {
        showAlert('loginAlert', 'Cannot connect to server. Make sure the backend is running.', 'error');
    }
    return false;
}

async function handleRegister(e) {
    e.preventDefault();
    try {
        const res = await fetch(API + '/api/auth/register', {
            method: 'POST',
            headers: roleHeaders({ 'Content-Type': 'application/json' }),
            body: JSON.stringify({
                fullName: document.getElementById('regName').value,
                email: document.getElementById('regEmail').value,
                phone: document.getElementById('regPhone').value,
                password: document.getElementById('regPassword').value,
                bloodGroup: document.getElementById('regBloodGroup').value,
                emergencyContact: document.getElementById('regEmergency').value
            })
        });
        const data = await res.json();
        if (!res.ok) {
            showAlert('registerAlert', data.error || 'Registration failed', 'error');
            return false;
        }
        showAlert('registerAlert', 'Registration successful! Please login.', 'success');
        setTimeout(() => showLogin(), 1500);
    } catch (err) {
        showAlert('registerAlert', 'Cannot connect to server.', 'error');
    }
    return false;
}

function showLogin() {
    document.getElementById('loginPage').classList.remove('hidden');
    document.getElementById('registerPage').classList.add('hidden');
}

function showRegister() {
    document.getElementById('loginPage').classList.add('hidden');
    document.getElementById('registerPage').classList.remove('hidden');
}

function logout() {
    currentUser = null;
    document.getElementById('appPage').classList.add('hidden');
    document.getElementById('loginPage').classList.remove('hidden');
    document.getElementById('loginEmail').value = '';
    document.getElementById('loginPassword').value = '';
}

function enterApp() {
    document.getElementById('loginPage').classList.add('hidden');
    document.getElementById('registerPage').classList.add('hidden');
    document.getElementById('appPage').classList.remove('hidden');

    document.getElementById('userName').textContent = currentUser.fullName;
    document.getElementById('userRole').textContent = currentUser.role;
    document.getElementById('userAvatar').textContent = currentUser.fullName.charAt(0).toUpperCase();
    document.getElementById('welcomeName').textContent = currentUser.fullName.split(' ')[0];

    applyRoleBasedAccess();
    showPage('dashboard');
}

// ==================== ROLE-BASED ACCESS CONTROL ====================

function applyRoleBasedAccess() {
    const role = currentUser.role; // PATIENT, DOCTOR, NURSE, ADMIN

    // Define which nav items each role can see
    const roleMenuMap = {
        PATIENT:       ['dashboard', 'doctors', 'appointments', 'records', 'billing'],
        DOCTOR:        ['dashboard', 'patients', 'appointments', 'records'],
        STAFF:         ['dashboard', 'patients', 'appointments', 'records', 'pharmacy', 'billing', 'wards'],
        ADMINISTRATOR: ['dashboard', 'patients', 'doctors', 'appointments', 'records', 'pharmacy', 'billing', 'wards'],
    };

    const allowed = roleMenuMap[role] || roleMenuMap['ADMINISTRATOR'];
    const allNavIds = ['dashboard', 'patients', 'doctors', 'appointments', 'records', 'pharmacy', 'billing', 'wards'];

    allNavIds.forEach(id => {
        const navEl = document.getElementById('nav-' + id);
        if (navEl) {
            navEl.style.display = allowed.includes(id) ? '' : 'none';
        }
    });

    // Patients cannot allot beds — hide the allot button and actions column
    const isPatient = role === 'PATIENT';
    const wardActionsBtn = document.getElementById('wardActions');
    if (wardActionsBtn) wardActionsBtn.style.display = isPatient ? 'none' : '';

    // Only DOCTOR and ADMIN can create medical records
    const canCreateRecord = (role === 'DOCTOR' || role === 'ADMINISTRATOR');
    const newRecordBtn = document.getElementById('newRecordBtn');
    if (newRecordBtn) newRecordBtn.style.display = canCreateRecord ? '' : 'none';

    // Only ADMINISTRATOR and STAFF can generate invoices
    const canBill = (role === 'ADMINISTRATOR' || role === 'STAFF');
    const generateInvoiceBtn = document.getElementById('generateInvoiceBtn');
    if (generateInvoiceBtn) generateInvoiceBtn.style.display = canBill ? '' : 'none';

    // Only ADMINISTRATOR and STAFF can add medicines
    const addMedicineBtn = document.getElementById('addMedicineBtn');
    if (addMedicineBtn) addMedicineBtn.style.display = canBill ? '' : 'none';

    // DOCTOR views appointments (their schedule) but does NOT book them — only PATIENT and STAFF/ADMIN book
    const bookApptBtn = document.getElementById('bookApptBtn');
    if (bookApptBtn) bookApptBtn.style.display = (role === 'DOCTOR') ? 'none' : '';
}

// ==================== NAVIGATION ====================

function showPage(page) {
    document.querySelectorAll('.page-section').forEach(s => s.classList.remove('active'));
    document.querySelectorAll('.nav-item').forEach(n => n.classList.remove('active'));

    document.getElementById('page-' + page).classList.add('active');
    const navEl = document.getElementById('nav-' + page);
    if (navEl) navEl.classList.add('active');

    // Load data
    switch (page) {
        case 'dashboard': loadDashboard(); break;
        case 'patients': loadPatients(); break;
        case 'doctors': loadDoctors(); break;
        case 'appointments': loadAppointments(); break;
        case 'records': loadRecords(); break;
        case 'pharmacy': loadPharmacy(); break;
        case 'billing': loadBilling(); break;
        case 'wards': loadWards(); break;
    }
}

// ==================== DASHBOARD ====================

// ==================== DASHBOARD ====================

async function loadDashboard() {
    try {
        const role = currentUser.role;
        if (role === 'PATIENT')       await loadPatientDashboard();
        else if (role === 'DOCTOR')   await loadDoctorDashboard();
        else                          await loadAdminDashboard();
    } catch (err) {
        console.error('Dashboard load error:', err);
    }
}

/** Set a stat card's label, icon, value, visibility and click target */
function setupCard(num, label, icon, value, navTarget, visible = true) {
    document.getElementById('statCard' + num).style.display = visible ? '' : 'none';
    if (!visible) return;
    document.getElementById('labelStat' + num).textContent = label;
    if (document.getElementById('iconStat' + num))
        document.getElementById('iconStat' + num).textContent = icon;
    document.getElementById('statCard' + num).onclick = () => showPage(navTarget);
    animateCounter(
        ['statPatients','statDoctors','statAppointments','statPending','statLowStock'][num-1],
        value
    );
}

async function loadAdminDashboard() {
    const stats = await fetchJson('/api/dashboard/stats');
    setupCard(1, 'Total Patients',    '👥', stats.totalPatients         || 0, 'patients');
    setupCard(2, 'Total Doctors',     '🩺', stats.totalDoctors          || 0, 'doctors');
    setupCard(3, 'Appointments Today','📅', stats.scheduledAppointments || 0, 'appointments');
    setupCard(4, 'Pending Bills',     '💳', stats.pendingInvoices       || 0, 'billing');
    setupCard(5, 'Low Stock Alerts',  '⚠️', stats.lowStockMedicines     || 0, 'pharmacy');

    const appts = await fetchJson('/api/appointments');
    renderDashAppointments(appts.slice(0, 5));
}

async function loadDoctorDashboard() {
    const did = currentUser.doctorId;
    const [appts, records] = await Promise.all([
        fetchJson('/api/appointments/doctor/' + did),
        fetchJson('/api/records/doctor/' + did)
    ]);
    const today = new Date().toISOString().substring(0, 10);
    const todayAppts  = appts.filter(a => a.date === today);
    const myPatients  = [...new Set(appts.map(a => a.patientId))].length;
    const openRecords = records.filter(r => r.status === 'OPEN' || r.status === 'UNDER_DIAGNOSIS').length;

    setupCard(1, 'My Patients',          '👥', myPatients,         'patients');
    setupCard(2, 'My Total Appointments','📅', appts.length,       'appointments');
    setupCard(3, "Today's Appointments", '🗓️', todayAppts.length,  'appointments');
    setupCard(4, 'Open Medical Records', '📋', openRecords,        'records');
    setupCard(5, '', '', 0, '', false); // hide low stock — not relevant to doctor

    renderDashAppointments(todayAppts.length ? todayAppts.slice(0, 5) : appts.slice(0, 5));
}

async function loadPatientDashboard() {
    const pid = currentUser.patientId;
    const [appts, records, bills] = await Promise.all([
        fetchJson('/api/appointments/patient/' + pid),
        fetchJson('/api/records/patient/' + pid),
        fetchJson('/api/billing/patient/' + pid)
    ]);
    const pendingBills = bills.filter(b => b.paymentStatus === 'PENDING').length;

    setupCard(1, 'My Appointments',   '📅', appts.length,   'appointments');
    setupCard(2, 'My Medical Records','📋', records.length, 'records');
    setupCard(3, 'My Pending Bills',  '💳', pendingBills,   'billing');
    setupCard(4, 'Total Bills',       '🧾', bills.length,   'billing');
    setupCard(5, '', '', 0, '', false); // hide low stock

    renderDashAppointments(appts.slice(0, 5));
}

function renderDashAppointments(recent) {
    const tbody = document.getElementById('dashAppointments');
    if (recent.length === 0) {
        tbody.innerHTML = '<tr><td colspan="6" class="empty-state"><div class="empty-icon">📅</div><h3>No appointments yet</h3></td></tr>';
        return;
    }
    tbody.innerHTML = recent.map(a => `
        <tr>
            <td><strong>${a.appointmentId}</strong></td>
            <td>${a.patientName}</td>
            <td>${a.doctorName}</td>
            <td>${a.date}</td>
            <td>${a.time}</td>
            <td>${statusBadge(a.status)}</td>
        </tr>
    `).join('');
}

function animateCounter(id, target) {
    const el = document.getElementById(id);
    let current = 0;
    const step = Math.max(1, Math.ceil(target / 30));
    const interval = setInterval(() => {
        current += step;
        if (current >= target) { current = target; clearInterval(interval); }
        el.textContent = current;
    }, 30);
}

// ==================== PATIENTS ====================

let allPatients = [];

async function loadPatients() {
    // Patients can only view their own profile, not the full list
    if (currentUser.role === 'PATIENT') {
        if (currentUser.patientId) {
            const p = await fetchJson('/api/patients/' + currentUser.patientId);
            allPatients = p && p.patientId ? [p] : [];
        } else {
            allPatients = [];
        }
        renderPatients(allPatients);
        return;
    }
    const patients = await fetchJson('/api/patients');
    allPatients = patients;
    renderPatients(patients);
}

function renderPatients(patients) {
    const tbody = document.getElementById('patientsList');
    if (patients.length === 0) {
        tbody.innerHTML = '<tr><td colspan="6" class="empty-state"><div class="empty-icon">👥</div><h3>No patients found</h3></td></tr>';
        return;
    }
    tbody.innerHTML = patients.map(p => `
        <tr>
            <td><strong>${p.patientId}</strong></td>
            <td>${p.fullName}</td>
            <td>${p.email}</td>
            <td>${p.phone}</td>
            <td>${p.bloodGroup || '-'}</td>
            <td>
                <button class="btn btn-ghost btn-sm" onclick="viewPatient('${p.patientId}')">View</button>
            </td>
        </tr>
    `).join('');
}

function searchPatients(q) {
    if (!q) { renderPatients(allPatients); return; }
    const filtered = allPatients.filter(p =>
        p.fullName.toLowerCase().includes(q.toLowerCase()) ||
        p.patientId.toLowerCase().includes(q.toLowerCase()) ||
        (p.phone && p.phone.includes(q))
    );
    renderPatients(filtered);
}

function viewPatient(id) {
    const p = allPatients.find(x => x.patientId === id);
    if (!p) return;
    showModal('Patient Details — ' + p.fullName, `
        <div class="form-row">
            <div class="form-group"><label>Patient ID</label><input value="${p.patientId}" readonly></div>
            <div class="form-group"><label>Full Name</label><input value="${p.fullName}" readonly></div>
        </div>
        <div class="form-row">
            <div class="form-group"><label>Email</label><input value="${p.email}" readonly></div>
            <div class="form-group"><label>Phone</label><input value="${p.phone}" readonly></div>
        </div>
        <div class="form-row">
            <div class="form-group"><label>Blood Group</label><input value="${p.bloodGroup || 'N/A'}" readonly></div>
            <div class="form-group"><label>Date of Birth</label><input value="${p.dateOfBirth || 'N/A'}" readonly></div>
        </div>
        <div class="form-group"><label>Medical History</label><textarea readonly rows="3">${p.medicalHistory || 'No records'}</textarea></div>
        <div class="form-group"><label>Emergency Contact</label><input value="${p.emergencyContact || 'N/A'}" readonly></div>
    `);
}

// ==================== DOCTORS ====================

let allDoctors = [];

async function loadDoctors() {
    const doctors = await fetchJson('/api/doctors');
    allDoctors = doctors;
    renderDoctors(doctors);
}

function renderDoctors(doctors) {
    const tbody = document.getElementById('doctorsList');
    if (doctors.length === 0) {
        tbody.innerHTML = '<tr><td colspan="6" class="empty-state"><div class="empty-icon">🩺</div><h3>No doctors found</h3></td></tr>';
        return;
    }
    tbody.innerHTML = doctors.map(d => `
        <tr>
            <td><strong>${d.doctorId}</strong></td>
            <td>${d.fullName}</td>
            <td>${d.specialization}</td>
            <td>${d.department}</td>
            <td>₹${d.consultationFee}</td>
            <td>${statusBadge(d.availability)}</td>
        </tr>
    `).join('');
}

function searchDoctors(q) {
    if (!q) { renderDoctors(allDoctors); return; }
    const filtered = allDoctors.filter(d =>
        d.fullName.toLowerCase().includes(q.toLowerCase()) ||
        d.specialization.toLowerCase().includes(q.toLowerCase())
    );
    renderDoctors(filtered);
}

// ==================== APPOINTMENTS ====================

async function loadAppointments() {
    let url = '/api/appointments';
    if (currentUser.role === 'PATIENT' && currentUser.patientId) {
        url = '/api/appointments/patient/' + currentUser.patientId;
    }
    const appts = await fetchJson(url);
    const tbody = document.getElementById('appointmentsList');
    if (appts.length === 0) {
        tbody.innerHTML = '<tr><td colspan="7" class="empty-state"><div class="empty-icon">📅</div><h3>No appointments found</h3></td></tr>';
        return;
    }
    const isPatientRole = currentUser.role === 'PATIENT';
    tbody.innerHTML = appts.map(a => `
        <tr>
            <td><strong>${a.appointmentId}</strong></td>
            <td>${a.patientName}</td>
            <td>${a.doctorName}</td>
            <td>${a.date}</td>
            <td>${a.time}</td>
            <td>${statusBadge(a.status)}</td>
            <td>
                ${a.status === 'SCHEDULED' ? `
                    ${!isPatientRole ? `<button class="btn btn-success btn-sm" onclick="completeAppointment('${a.appointmentId}')">Complete</button>` : ''}
                    <button class="btn btn-danger btn-sm" onclick="cancelAppointment('${a.appointmentId}')">Cancel</button>
                ` : ''}
            </td>
        </tr>
    `).join('');
}

async function showBookAppointment() {
    // Ensure doctors are loaded
    if (allDoctors.length === 0) allDoctors = await fetchJson('/api/doctors');

    const role      = currentUser.role;
    const isPatient = role === 'PATIENT';
    const todayStr  = new Date().toISOString().substring(0, 10);

    // ── Patient field ────────────────────────────────────────────────────
    // PATIENT: auto-fill their own ID (readonly — they can only book for themselves)
    // STAFF / ADMIN: dropdown of all patients (they book on behalf of any patient)
    let patientField;
    if (isPatient) {
        const pid = currentUser.patientId || '';
        patientField = `
            <input type="text" id="apptPatient" value="${pid}"
                readonly style="background:var(--bg-secondary);color:var(--text-muted);">
            ${!pid ? '<small style="color:var(--accent-rose);">⚠️ Patient ID missing — please log out and log in again.</small>' : ''}`;
    } else {
        // Staff / Admin — load patient list for dropdown
        if (allPatients.length === 0) allPatients = await fetchJson('/api/patients');
        patientField = `
            <select id="apptPatient">
                <option value="">— Select Patient —</option>
                ${allPatients.map(p =>
                    `<option value="${p.patientId}">${p.fullName} (${p.patientId})</option>`
                ).join('')}
            </select>`;
    }

    // ── Doctor field ─────────────────────────────────────────────────────
    // All roles that see this modal (PATIENT, STAFF, ADMIN) get a dropdown
    const doctorField = `
        <select id="apptDoctor" onchange="loadSlots()">
            <option value="">— Select Doctor —</option>
            ${allDoctors.map(d =>
                `<option value="${d.doctorId}">${d.fullName} — ${d.specialization}</option>`
            ).join('')}
        </select>`;

    showModal('Book New Appointment', `
        <div class="form-group">
            <label>Patient</label>
            ${patientField}
        </div>
        <div class="form-group">
            <label>Doctor</label>
            ${doctorField}
        </div>
        <div class="form-group">
            <label>Date</label>
            <input type="date" id="apptDate" min="${todayStr}" onchange="loadSlots()">
        </div>
        <div class="form-group">
            <label>Available Slot</label>
            <select id="apptSlot">
                <option value="">Select date &amp; doctor first</option>
            </select>
        </div>
        <div class="form-group">
            <label>Notes</label>
            <textarea id="apptNotes" rows="2" placeholder="Optional notes..."></textarea>
        </div>
    `, async () => {
        const patientId = document.getElementById('apptPatient').value.trim();
        const doctorId  = document.getElementById('apptDoctor').value.trim();
        const slotId    = document.getElementById('apptSlot').value.trim();
        const date      = document.getElementById('apptDate').value.trim();

        if (!patientId) { alert('Please select a Patient.'); return; }
        if (!doctorId)  { alert('Please select a Doctor.'); return; }
        if (!date)      { alert('Please select a Date.'); return; }
        if (!slotId)    { alert('No slot selected. Pick a date and doctor, then choose a slot.'); return; }

        const res = await fetch(API + '/api/appointments', {
            method: 'POST',
            headers: roleHeaders({ 'Content-Type': 'application/json' }),
            body: JSON.stringify({ patientId, doctorId, slotId, date,
                notes: document.getElementById('apptNotes').value })
        });
        if (res.ok) { closeModal(); loadAppointments(); }
        else { const d = await res.json(); alert(d.error || 'Booking failed'); }
    });
}

async function loadSlots() {
    const doctorId = document.getElementById('apptDoctor').value;
    const date = document.getElementById('apptDate').value;
    const select = document.getElementById('apptSlot');
    if (!doctorId || !date) return;

    const slots = await fetchJson(`/api/appointments/slots?doctorId=${doctorId}&date=${date}`);
    if (slots.length === 0) {
        select.innerHTML = '<option value="">No available slots — create slots first</option>';
    } else {
        select.innerHTML = slots.map(s => `<option value="${s.slotId}">${s.startTime} - ${s.endTime}</option>`).join('');
    }
}

async function completeAppointment(id) {
    await fetch(API + `/api/appointments/${id}/complete`, { method: 'PUT', headers: roleHeaders() });
    loadAppointments();
}

async function cancelAppointment(id) {
    if (!confirm('Cancel this appointment?')) return;
    await fetch(API + `/api/appointments/${id}/cancel`, {
        method: 'PUT',
        headers: roleHeaders({ 'Content-Type': 'application/json' }),
        body: JSON.stringify({ reason: 'Cancelled by staff' })
    });
    loadAppointments();
}

// ==================== MEDICAL RECORDS ====================

let allRecords = [];

async function loadRecords() {
    let url = '/api/records';
    if (currentUser.role === 'PATIENT' && currentUser.patientId) {
        url = '/api/records/patient/' + currentUser.patientId;
    }
    const records = await fetchJson(url);
    allRecords = records;
    const tbody = document.getElementById('recordsList');
    if (records.length === 0) {
        tbody.innerHTML = '<tr><td colspan="7" class="empty-state"><div class="empty-icon">📋</div><h3>No records found</h3></td></tr>';
        return;
    }
    tbody.innerHTML = records.map(r => `
        <tr>
            <td><strong>${r.recordId}</strong></td>
            <td>${r.patientName}</td>
            <td>${r.doctorName}</td>
            <td>${(r.diagnosisNotes || '').substring(0, 40)}${(r.diagnosisNotes || '').length > 40 ? '...' : ''}</td>
            <td>${statusBadge(r.status)}</td>
            <td>${r.dateCreated ? r.dateCreated.substring(0, 10) : '-'}</td>
            <td><button class="btn btn-ghost btn-sm" onclick="viewRecord('${r.recordId}')">View</button></td>
        </tr>
    `).join('');
}

function viewRecord(recordId) {
    const r = allRecords.find(x => x.recordId === recordId);
    if (!r) return;
    showModal('Medical Record — ' + r.recordId, `
        <div class="form-row">
            <div class="form-group"><label>Record ID</label><input value="${r.recordId}" readonly></div>
            <div class="form-group"><label>Status</label><input value="${r.status}" readonly></div>
        </div>
        <div class="form-row">
            <div class="form-group"><label>Patient</label><input value="${r.patientName}" readonly></div>
            <div class="form-group"><label>Doctor</label><input value="${r.doctorName}" readonly></div>
        </div>
        <div class="form-group"><label>Date Created</label><input value="${r.dateCreated ? r.dateCreated.substring(0, 10) : 'N/A'}" readonly></div>
        <div class="form-group">
            <label>Diagnosis Notes</label>
            <textarea readonly rows="4" style="width:100%; resize:none;">${r.diagnosisNotes || 'No diagnosis notes'}</textarea>
        </div>
        <div class="form-group">
            <label>Lab Results</label>
            <textarea readonly rows="3" style="width:100%; resize:none;">${r.labResults || 'No lab results'}</textarea>
        </div>
        <div class="form-group">
            <label>Prescription Details</label>
            <textarea readonly rows="3" style="width:100%; resize:none;">${r.prescriptionDetails || 'No prescription'}</textarea>
        </div>
    `);
}

function showCreateRecord() {
    const isDoctor = currentUser.role === 'DOCTOR';
    const doctorVal = isDoctor ? (currentUser.doctorId || '') : '';
    showModal('Create Medical Record', `
        <div class="form-row">
            <div class="form-group">
                <label>Patient ID</label>
                <input type="text" id="recPatient" placeholder="e.g. PAT00001">
            </div>
            <div class="form-group">
                <label>Doctor ID</label>
                <input type="text" id="recDoctor" placeholder="e.g. DOC00001"
                    value="${doctorVal}"
                    ${isDoctor ? 'readonly style="background:var(--bg-secondary);color:var(--text-muted);"' : ''}>
            </div>
        </div>
        <div class="form-group">
            <label>Diagnosis Notes</label>
            <textarea id="recDiagnosis" rows="3" placeholder="Enter diagnosis..."></textarea>
        </div>
        <div class="form-group">
            <label>Lab Results</label>
            <textarea id="recLab" rows="2" placeholder="Lab results..."></textarea>
        </div>
        <div class="form-group">
            <label>Prescription Details</label>
            <textarea id="recPrescription" rows="2" placeholder="Prescription details..."></textarea>
        </div>
    `, async () => {
        const res = await fetch(API + '/api/records', {
            method: 'POST',
            headers: roleHeaders({ 'Content-Type': 'application/json' }),
            body: JSON.stringify({
                patientId: document.getElementById('recPatient').value,
                doctorId: document.getElementById('recDoctor').value,
                diagnosisNotes: document.getElementById('recDiagnosis').value,
                labResults: document.getElementById('recLab').value,
                prescriptionDetails: document.getElementById('recPrescription').value
            })
        });
        if (res.ok) { closeModal(); loadRecords(); }
        else { const d = await res.json(); alert(d.error || 'Failed'); }
    });
}

// ==================== PHARMACY ====================

// Common medicine catalog — name → generic + category auto-fill
const MEDICINE_CATALOG = [
    { name: 'Paracetamol 500mg',    generic: 'Acetaminophen',   category: 'Analgesic' },
    { name: 'Paracetamol 650mg',    generic: 'Acetaminophen',   category: 'Analgesic' },
    { name: 'Ibuprofen 400mg',      generic: 'Ibuprofen',       category: 'NSAID' },
    { name: 'Ibuprofen 200mg',      generic: 'Ibuprofen',       category: 'NSAID' },
    { name: 'Diclofenac 50mg',      generic: 'Diclofenac',      category: 'NSAID' },
    { name: 'Aspirin 75mg',         generic: 'Aspirin',         category: 'Antiplatelet' },
    { name: 'Aspirin 150mg',        generic: 'Aspirin',         category: 'Antiplatelet' },
    { name: 'Clopidogrel 75mg',     generic: 'Clopidogrel',     category: 'Antiplatelet' },
    { name: 'Amoxicillin 250mg',    generic: 'Amoxicillin',     category: 'Antibiotic' },
    { name: 'Amoxicillin 500mg',    generic: 'Amoxicillin',     category: 'Antibiotic' },
    { name: 'Azithromycin 500mg',   generic: 'Azithromycin',    category: 'Antibiotic' },
    { name: 'Ciprofloxacin 500mg',  generic: 'Ciprofloxacin',   category: 'Antibiotic' },
    { name: 'Metronidazole 400mg',  generic: 'Metronidazole',   category: 'Antibiotic' },
    { name: 'Metformin 500mg',      generic: 'Metformin',       category: 'Antidiabetic' },
    { name: 'Metformin 1000mg',     generic: 'Metformin',       category: 'Antidiabetic' },
    { name: 'Glibenclamide 5mg',    generic: 'Glibenclamide',   category: 'Antidiabetic' },
    { name: 'Insulin Regular',      generic: 'Insulin',         category: 'Antidiabetic' },
    { name: 'Insulin Glargine',     generic: 'Insulin Glargine',category: 'Antidiabetic' },
    { name: 'Atorvastatin 10mg',    generic: 'Atorvastatin',    category: 'Statin' },
    { name: 'Atorvastatin 20mg',    generic: 'Atorvastatin',    category: 'Statin' },
    { name: 'Atorvastatin 40mg',    generic: 'Atorvastatin',    category: 'Statin' },
    { name: 'Rosuvastatin 10mg',    generic: 'Rosuvastatin',    category: 'Statin' },
    { name: 'Amlodipine 5mg',       generic: 'Amlodipine',      category: 'Antihypertensive' },
    { name: 'Losartan 50mg',        generic: 'Losartan',        category: 'Antihypertensive' },
    { name: 'Lisinopril 10mg',      generic: 'Lisinopril',      category: 'Antihypertensive' },
    { name: 'Enalapril 5mg',        generic: 'Enalapril',       category: 'Antihypertensive' },
    { name: 'Omeprazole 20mg',      generic: 'Omeprazole',      category: 'Proton Pump Inhibitor' },
    { name: 'Pantoprazole 40mg',    generic: 'Pantoprazole',    category: 'Proton Pump Inhibitor' },
    { name: 'Ranitidine 150mg',     generic: 'Ranitidine',      category: 'H2 Blocker' },
    { name: 'Cetirizine 10mg',      generic: 'Cetirizine',      category: 'Antihistamine' },
    { name: 'Chlorphenamine 4mg',   generic: 'Chlorphenamine',  category: 'Antihistamine' },
    { name: 'Salbutamol Inhaler',   generic: 'Salbutamol',      category: 'Bronchodilator' },
    { name: 'Prednisolone 5mg',     generic: 'Prednisolone',    category: 'Corticosteroid' },
    { name: 'Dexamethasone 4mg',    generic: 'Dexamethasone',   category: 'Corticosteroid' },
    { name: 'Warfarin 5mg',         generic: 'Warfarin',        category: 'Anticoagulant' },
    { name: 'Phenytoin 100mg',      generic: 'Phenytoin',       category: 'Antiepileptic' },
    { name: 'Diazepam 5mg',         generic: 'Diazepam',        category: 'Anxiolytic' },
    { name: 'Ondansetron 4mg',      generic: 'Ondansetron',     category: 'Antiemetic' },
    { name: 'Domperidone 10mg',     generic: 'Domperidone',     category: 'Antiemetic' },
    { name: 'Vitamin D3 60000 IU',  generic: 'Cholecalciferol', category: 'Vitamin' },
    { name: 'Vitamin B12 500mcg',   generic: 'Cyanocobalamin',  category: 'Vitamin' },
    { name: 'Iron 100mg',           generic: 'Ferrous Sulphate',category: 'Supplement' },
    { name: 'Calcium 500mg',        generic: 'Calcium Carbonate',category:'Supplement' },
];

const SUPPLIERS = [
    'Sun Pharma', 'Cipla Ltd', 'Dr. Reddys', 'Lupin Ltd', 'Torrent Pharma',
    'Aurobindo Pharma', 'Zydus Cadila', 'Alkem Laboratories', 'Abbott India',
    'Pfizer India', 'Novartis India', 'GlaxoSmithKline', 'Mankind Pharma',
    'Intas Pharmaceuticals', 'Wockhardt', 'Glenmark Pharmaceuticals'
];

/** Called when medicine dropdown changes — auto-fills generic name & category */
function fillMedicineDetails() {
    const sel = document.getElementById('medNameSel');
    const val = sel ? sel.value : '';
    const customRow = document.getElementById('medCustomNameRow');

    if (val === '__custom__') {
        if (customRow) customRow.style.display = '';
        document.getElementById('medGeneric').value  = '';
        document.getElementById('medCategory').value = '';
    } else {
        if (customRow) customRow.style.display = 'none';
        const med = MEDICINE_CATALOG.find(m => m.name === val);
        if (med) {
            document.getElementById('medGeneric').value  = med.generic;
            document.getElementById('medCategory').value = med.category;
        } else {
            document.getElementById('medGeneric').value  = '';
            document.getElementById('medCategory').value = '';
        }
    }
}

/** Called when supplier dropdown changes — shows custom input if "Other" selected */
function toggleCustomSupplier() {
    const sel = document.getElementById('medSupplierSel');
    const customRow = document.getElementById('medCustomSupplierRow');
    if (!sel || !customRow) return;
    customRow.style.display = sel.value === '__custom__' ? '' : 'none';
}

async function loadPharmacy() {
    const meds = await fetchJson('/api/pharmacy');
    const tbody = document.getElementById('pharmacyList');
    if (meds.length === 0) {
        tbody.innerHTML = '<tr><td colspan="8" class="empty-state"><div class="empty-icon">💊</div><h3>No medicines in inventory</h3></td></tr>';
        return;
    }
    tbody.innerHTML = meds.map(m => {
        const isLow = m.stockQuantity <= m.reorderLevel;
        return `
            <tr>
                <td><strong>${m.medicineId}</strong></td>
                <td>${m.medicineName}</td>
                <td>${m.genericName || '-'}</td>
                <td>${m.stockQuantity}</td>
                <td>₹${m.unitPrice}</td>
                <td>${m.expiryDate}</td>
                <td>${isLow ? '<span class="badge badge-low">Low Stock</span>' : '<span class="badge badge-available">In Stock</span>'}</td>
                <td><button class="btn btn-ghost btn-sm" onclick="restockMedicine('${m.medicineId}')">Restock</button></td>
            </tr>
        `;
    }).join('');
}

function showAddMedicine() {
    const catalogOptions = MEDICINE_CATALOG.map(m =>
        `<option value="${m.name}">${m.name}</option>`
    ).join('');

    const supplierOptions = SUPPLIERS.map(s =>
        `<option value="${s}">${s}</option>`
    ).join('');

    showModal('Add New Medicine', `
        <div class="form-group">
            <label>Medicine Name</label>
            <select id="medNameSel" onchange="fillMedicineDetails()">
                <option value="">— Select Medicine —</option>
                ${catalogOptions}
                <option value="__custom__">+ Other (type below)</option>
            </select>
        </div>
        <div class="form-group" id="medCustomNameRow" style="display:none;">
            <label>Custom Medicine Name</label>
            <input type="text" id="medNameCustom" placeholder="e.g. Doxycycline 100mg">
        </div>
        <div class="form-row">
            <div class="form-group">
                <label>Generic Name</label>
                <input type="text" id="medGeneric" placeholder="Auto-filled on selection">
            </div>
            <div class="form-group">
                <label>Category</label>
                <input type="text" id="medCategory" placeholder="Auto-filled on selection">
            </div>
        </div>
        <div class="form-row">
            <div class="form-group"><label>Stock Quantity</label><input type="number" id="medStock" value="100" min="0"></div>
            <div class="form-group"><label>Unit Price (₹)</label><input type="number" step="0.01" id="medPrice" value="10.00" min="0"></div>
        </div>
        <div class="form-row">
            <div class="form-group"><label>Expiry Date</label><input type="date" id="medExpiry" required></div>
            <div class="form-group"><label>Reorder Level</label><input type="number" id="medReorder" value="10" min="0"></div>
        </div>
        <div class="form-group">
            <label>Supplier</label>
            <select id="medSupplierSel" onchange="toggleCustomSupplier()">
                <option value="">— Select Supplier —</option>
                ${supplierOptions}
                <option value="__custom__">+ Other Supplier</option>
            </select>
        </div>
        <div class="form-group" id="medCustomSupplierRow" style="display:none;">
            <label>Custom Supplier Name</label>
            <input type="text" id="medSupplierCustom" placeholder="Enter supplier name">
        </div>
    `, async () => {
        // Resolve medicine name
        const selVal     = document.getElementById('medNameSel').value;
        const medicineName = selVal === '__custom__'
            ? (document.getElementById('medNameCustom')?.value.trim() || '')
            : selVal;

        // Resolve supplier
        const supSelVal  = document.getElementById('medSupplierSel').value;
        const supplier   = supSelVal === '__custom__'
            ? (document.getElementById('medSupplierCustom')?.value.trim() || '')
            : supSelVal;

        if (!medicineName) { alert('Please select or enter a medicine name.'); return; }
        if (!supplier)     { alert('Please select or enter a supplier.'); return; }

        const res = await fetch(API + '/api/pharmacy', {
            method: 'POST',
            headers: roleHeaders({ 'Content-Type': 'application/json' }),
            body: JSON.stringify({
                medicineName,
                genericName:    document.getElementById('medGeneric').value,
                category:       document.getElementById('medCategory').value,
                stockQuantity:  document.getElementById('medStock').value,
                unitPrice:      document.getElementById('medPrice').value,
                expiryDate:     document.getElementById('medExpiry').value,
                reorderLevel:   document.getElementById('medReorder').value,
                supplier
            })
        });
        if (res.ok) { closeModal(); loadPharmacy(); }
        else { const d = await res.json(); alert(d.error || 'Failed'); }
    });
}

function restockMedicine(id) {
    showModal('Restock Medicine', `
        <div class="form-group">
            <label>Quantity to Add</label>
            <input type="number" id="restockQty" value="50" min="1">
        </div>
    `, async () => {
        const qty = document.getElementById('restockQty').value;
        await fetch(API + `/api/pharmacy/${id}/restock`, {
            method: 'PUT',
            headers: roleHeaders({ 'Content-Type': 'application/json' }),
            body: JSON.stringify({ quantity: qty })
        });
        closeModal();
        loadPharmacy();
    });
}

// ==================== BILLING ====================

async function loadBilling() {
    let url = '/api/billing';
    if (currentUser.role === 'PATIENT' && currentUser.patientId) {
        url = '/api/billing/patient/' + currentUser.patientId;
    }
    const invoices = await fetchJson(url);
    const tbody = document.getElementById('billingList');
    if (invoices.length === 0) {
        tbody.innerHTML = '<tr><td colspan="6" class="empty-state"><div class="empty-icon">💳</div><h3>No invoices yet</h3></td></tr>';
        return;
    }
    tbody.innerHTML = invoices.map(i => `
        <tr>
            <td><strong>${i.billId}</strong></td>
            <td>${i.patientName}</td>
            <td>₹${i.totalAmount || '0.00'}</td>
            <td>${statusBadge(i.paymentStatus)}</td>
            <td>${i.generatedDate ? i.generatedDate.substring(0, 10) : '-'}</td>
            <td>
                ${i.paymentStatus !== 'PAID' ? `<button class="btn btn-success btn-sm" onclick="payInvoice('${i.billId}', ${i.totalAmount || 0})">Pay</button>` : ''}
            </td>
        </tr>
    `).join('');
}

function showCreateInvoice() {
    showModal('Generate Invoice', `
        <div class="form-group"><label>Patient ID</label><input type="text" id="invPatient" placeholder="e.g. PAT00001" required></div>
        <div class="form-row">
            <div class="form-group"><label>Consultation Fee (₹)</label><input type="number" step="0.01" id="invConsult" value="800"></div>
            <div class="form-group"><label>Medication Cost (₹)</label><input type="number" step="0.01" id="invMed" value="0"></div>
        </div>
        <div class="form-row">
            <div class="form-group"><label>Lab Charges (₹)</label><input type="number" step="0.01" id="invLab" value="0"></div>
            <div class="form-group"><label>Room Charges (₹)</label><input type="number" step="0.01" id="invRoom" value="0"></div>
        </div>
        <div class="form-group"><label>Tax Amount (₹)</label><input type="number" step="0.01" id="invTax" value="0"></div>
    `, async () => {
        const res = await fetch(API + '/api/billing', {
            method: 'POST',
            headers: roleHeaders({ 'Content-Type': 'application/json' }),
            body: JSON.stringify({
                patientId: document.getElementById('invPatient').value,
                consultationFee: document.getElementById('invConsult').value,
                medicationCost: document.getElementById('invMed').value,
                labCharges: document.getElementById('invLab').value,
                roomCharges: document.getElementById('invRoom').value,
                taxAmount: document.getElementById('invTax').value
            })
        });
        if (res.ok) { closeModal(); loadBilling(); }
        else { const d = await res.json(); alert(d.error || 'Failed'); }
    });
}

function payInvoice(billId, amount) {
    // Patients cannot change the bill amount — it is set by the hospital
    const amountLocked = currentUser.role === 'PATIENT';
    showModal('Process Payment', `
        <div class="form-group">
            <label>Bill Amount (₹)</label>
            <input type="number" step="0.01" id="payAmount" value="${amount}"
                ${amountLocked ? 'readonly style="background:var(--bg-secondary);color:var(--text-muted);"' : ''}>
        </div>
        <div class="form-group">
            <label>Payment Method</label>
            <select id="payMethod" onchange="updatePaymentFields()">
                <option value="CASH">Cash</option>
                <option value="CARD">Card / Debit / Credit</option>
                <option value="UPI">UPI</option>
                <option value="ONLINE">Online Transfer / NEFT</option>
                <option value="INSURANCE">Insurance Claim</option>
            </select>
        </div>
        <div id="paymentExtraFields"></div>
    `, async () => {
        const method = document.getElementById('payMethod').value;
        let referenceNo = '';

        if (method === 'UPI') {
            referenceNo = document.getElementById('payUpiId')?.value || '';
        } else if (method === 'CARD') {
            const raw = (document.getElementById('payCardNo')?.value || '').replace(/\s/g, '');
            referenceNo = 'Card ending ' + (raw.slice(-4) || 'XXXX');
        } else if (method === 'ONLINE') {
            referenceNo = document.getElementById('payTxnId')?.value || '';
        } else if (method === 'INSURANCE') {
            const provider = document.getElementById('payInsProvider')?.value || '';
            const policy   = document.getElementById('payInsPolicy')?.value || '';
            referenceNo = provider + (policy ? ' | Policy: ' + policy : '');
        }

        const res = await fetch(API + `/api/billing/${billId}/pay`, {
            method: 'POST',
            headers: roleHeaders({ 'Content-Type': 'application/json' }),
            body: JSON.stringify({
                amount: document.getElementById('payAmount').value,
                method: method,
                referenceNo: referenceNo
            })
        });
        if (res.ok) { closeModal(); loadBilling(); }
        else { const d = await res.json(); alert(d.error || 'Payment failed'); }
    });
}

function updatePaymentFields() {
    const method    = document.getElementById('payMethod')?.value;
    const container = document.getElementById('paymentExtraFields');
    if (!container) return;

    const fields = {
        CASH: '',
        CARD: `
            <div class="form-group">
                <label>Card Number</label>
                <input type="text" id="payCardNo" placeholder="1234 5678 9012 3456" maxlength="19"
                    oninput="this.value=this.value.replace(/[^\\d]/g,'').replace(/(\\d{4}(?!$))/g,'$1 ')">
            </div>
            <div class="form-row">
                <div class="form-group">
                    <label>Expiry (MM/YY)</label>
                    <input type="text" id="payExpiry" placeholder="MM/YY" maxlength="5"
                        oninput="this.value=this.value.replace(/[^\\d\\/]/g,'')">
                </div>
                <div class="form-group">
                    <label>CVV</label>
                    <input type="password" id="payCvv" placeholder="•••" maxlength="4">
                </div>
            </div>`,
        UPI: `
            <div class="form-group">
                <label>UPI ID</label>
                <input type="text" id="payUpiId" placeholder="e.g. name@okaxis">
            </div>`,
        ONLINE: `
            <div class="form-group">
                <label>Transaction / Reference ID</label>
                <input type="text" id="payTxnId" placeholder="Bank transaction reference number">
            </div>`,
        INSURANCE: `
            <div class="form-group">
                <label>Insurance Provider</label>
                <input type="text" id="payInsProvider" placeholder="e.g. Star Health Insurance">
            </div>
            <div class="form-group">
                <label>Policy Number / Member ID</label>
                <input type="text" id="payInsPolicy" placeholder="Policy or member number">
            </div>
            <div class="form-group">
                <label>Coverage Amount (₹)</label>
                <input type="number" step="0.01" id="payInsCoverage" placeholder="0.00">
            </div>`
    };

    container.innerHTML = fields[method] || '';
}

// ==================== WARD ALLOTMENT ====================

let allWards = [];

async function loadWards() {
    const [wards, allotments] = await Promise.all([
        fetchJson('/api/wards'),
        currentUser.role === 'PATIENT' && currentUser.patientId
            ? fetchJson('/api/wards/allotments/patient/' + currentUser.patientId)
            : fetchJson('/api/wards/allotments')
    ]);

    allWards = wards;
    renderWardsList(wards);
    renderAllotments(allotments);
}

function renderWardsList(wards) {
    const tbody = document.getElementById('wardsList');
    if (!wards || wards.length === 0) {
        tbody.innerHTML = '<tr><td colspan="7" class="empty-state"><div class="empty-icon">🛏️</div><h3>No wards found</h3></td></tr>';
        return;
    }
    tbody.innerHTML = wards.map(w => `
        <tr>
            <td><strong>${w.wardId}</strong></td>
            <td>${w.wardName}</td>
            <td>${w.wardType}</td>
            <td>${w.floorNumber}</td>
            <td>${w.totalBeds}</td>
            <td>${w.availableBeds}</td>
            <td>${w.available
                ? '<span class="badge badge-available">Available</span>'
                : '<span class="badge badge-unavailable">Full</span>'}</td>
        </tr>
    `).join('');
}

function renderAllotments(allotments) {
    const tbody = document.getElementById('allotmentsList');
    const isPatient = currentUser.role === 'PATIENT';
    const colSpan = isPatient ? 8 : 9;

    if (!allotments || allotments.length === 0) {
        tbody.innerHTML = `<tr><td colspan="${colSpan}" class="empty-state"><div class="empty-icon">🛏️</div><h3>No allotments found</h3></td></tr>`;
        return;
    }
    tbody.innerHTML = allotments.map(a => `
        <tr>
            <td><strong>${a.allotmentId}</strong></td>
            <td>${a.patientName}</td>
            <td>${a.wardName}</td>
            <td>${a.bedNumber}</td>
            <td>${a.isICU ? '<span class="badge badge-scheduled">ICU</span>' : 'No'}</td>
            <td>${a.nurseAssigned || '-'}</td>
            <td>${a.allotmentDate ? a.allotmentDate.substring(0, 10) : '-'}</td>
            <td>${statusBadge(a.status)}</td>
            ${!isPatient ? `<td>
                ${a.status === 'ACTIVE'
                    ? `<button class="btn btn-danger btn-sm" onclick="dischargePatient(${a.allotmentId})">Discharge</button>`
                    : '-'}
            </td>` : ''}
        </tr>
    `).join('');
}

async function showAllotBed() {
    // Ensure wards are loaded before rendering the modal
    if (allWards.length === 0) {
        allWards = await fetchJson('/api/wards');
    }
    const wardOptions = allWards.filter(w => w.available)
        .map(w => `<option value="${w.wardId}">${w.wardName} (${w.wardType}) — ${w.availableBeds} beds free</option>`)
        .join('');

    showModal('Allot Bed to Patient', `
        <div class="form-group">
            <label>Patient ID</label>
            <input type="text" id="allotPatient" placeholder="e.g. PAT00001" required>
        </div>
        <div class="form-group">
            <label>Ward</label>
            <select id="allotWard" required>
                <option value="">Select Ward</option>
                ${wardOptions || '<option disabled>No wards available</option>'}
            </select>
        </div>
        <div class="form-row">
            <div class="form-group">
                <label>Bed Number</label>
                <input type="text" id="allotBed" placeholder="e.g. B-12">
            </div>
            <div class="form-group">
                <label>ICU?</label>
                <select id="allotICU">
                    <option value="false">No</option>
                    <option value="true">Yes</option>
                </select>
            </div>
        </div>
        <div class="form-group">
            <label>Nurse Assigned</label>
            <input type="text" id="allotNurse" placeholder="Nurse name (optional)">
        </div>
    `, async () => {
        const res = await fetch(API + '/api/wards/allotments', {
            method: 'POST',
            headers: roleHeaders({ 'Content-Type': 'application/json' }),
            body: JSON.stringify({
                patientId: document.getElementById('allotPatient').value,
                wardId: document.getElementById('allotWard').value,
                bedNumber: document.getElementById('allotBed').value || 'TBD',
                isICU: document.getElementById('allotICU').value,
                nurseAssigned: document.getElementById('allotNurse').value
            })
        });
        if (res.ok) { closeModal(); loadWards(); }
        else { const d = await res.json(); alert(d.error || 'Allotment failed'); }
    });
}

async function dischargePatient(allotmentId) {
    if (!confirm('Discharge this patient?')) return;
    const res = await fetch(API + `/api/wards/allotments/${allotmentId}/discharge`, { method: 'PUT', headers: roleHeaders() });
    if (res.ok) loadWards();
    else alert('Discharge failed');
}

// ==================== MODAL SYSTEM ====================

function showModal(title, bodyHtml, onSave) {
    const container = document.getElementById('modalContainer');
    container.classList.remove('hidden');
    container.innerHTML = `
        <div class="modal-overlay" onclick="if(event.target===this) closeModal()">
            <div class="modal">
                <div class="modal-header">
                    <h3>${title}</h3>
                    <button class="modal-close" onclick="closeModal()">✕</button>
                </div>
                <div class="modal-body">${bodyHtml}</div>
                ${onSave ? `<div class="modal-footer">
                    <button class="btn btn-ghost" onclick="closeModal()">Cancel</button>
                    <button class="btn btn-primary" id="modalSaveBtn">Save</button>
                </div>` : ''}
            </div>
        </div>
    `;
    if (onSave) {
        document.getElementById('modalSaveBtn').onclick = onSave;
    }
}

function closeModal() {
    document.getElementById('modalContainer').classList.add('hidden');
    document.getElementById('modalContainer').innerHTML = '';
}

// ==================== UTILITIES ====================

function roleHeaders(extra) {
    return { 'X-User-Role': (currentUser && currentUser.role) || '', ...extra };
}

async function fetchJson(url) {
    try {
        const res = await fetch(API + url, { headers: roleHeaders() });
        if (!res.ok) return [];
        return await res.json();
    } catch { return []; }
}

function statusBadge(status) {
    if (!status) return '';
    const cls = status.toLowerCase().replace(/_/g, '-');
    const map = {
        'scheduled': 'badge-scheduled', 'in-consultation': 'badge-scheduled',
        'completed': 'badge-completed', 'cancelled': 'badge-cancelled',
        'pending': 'badge-pending', 'paid': 'badge-paid',
        'partially-paid': 'badge-pending', 'available': 'badge-available',
        'unavailable': 'badge-unavailable', 'on-leave': 'badge-cancelled',
        'open': 'badge-open', 'under-diagnosis': 'badge-scheduled',
        'updated': 'badge-completed', 'closed': 'badge-completed',
    };
    const badge = map[cls] || 'badge-scheduled';
    return `<span class="badge ${badge}">${status.replace(/_/g, ' ')}</span>`;
}

function showAlert(containerId, message, type) {
    const el = document.getElementById(containerId);
    el.className = `alert alert-${type}`;
    el.innerHTML = `<span>${type === 'error' ? '⚠️' : '✅'}</span> ${message}`;
    el.classList.remove('hidden');
    if (type === 'success') setTimeout(() => el.classList.add('hidden'), 3000);
}
