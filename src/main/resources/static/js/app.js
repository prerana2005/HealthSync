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
            headers: { 'Content-Type': 'application/json' },
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
            headers: { 'Content-Type': 'application/json' },
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

    showPage('dashboard');
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
    }
}

// ==================== DASHBOARD ====================

async function loadDashboard() {
    try {
        const stats = await fetchJson('/api/dashboard/stats');
        animateCounter('statPatients', stats.totalPatients || 0);
        animateCounter('statDoctors', stats.totalDoctors || 0);
        animateCounter('statAppointments', stats.scheduledAppointments || 0);
        animateCounter('statPending', stats.pendingInvoices || 0);
        animateCounter('statLowStock', stats.lowStockMedicines || 0);

        const appts = await fetchJson('/api/appointments');
        const recent = appts.slice(0, 5);
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
    } catch (err) {
        console.error('Dashboard load error:', err);
    }
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
    const appts = await fetchJson('/api/appointments');
    const tbody = document.getElementById('appointmentsList');
    if (appts.length === 0) {
        tbody.innerHTML = '<tr><td colspan="7" class="empty-state"><div class="empty-icon">📅</div><h3>No appointments found</h3></td></tr>';
        return;
    }
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
                    <button class="btn btn-success btn-sm" onclick="completeAppointment('${a.appointmentId}')">Complete</button>
                    <button class="btn btn-danger btn-sm" onclick="cancelAppointment('${a.appointmentId}')">Cancel</button>
                ` : ''}
            </td>
        </tr>
    `).join('');
}

function showBookAppointment() {
    showModal('Book New Appointment', `
        <div class="form-group">
            <label>Patient ID</label>
            <input type="text" id="apptPatient" placeholder="e.g. PAT00001">
        </div>
        <div class="form-group">
            <label>Doctor</label>
            <select id="apptDoctor" onchange="loadSlots()">
                <option value="">Select Doctor</option>
                ${allDoctors.map(d => `<option value="${d.doctorId}">${d.fullName} — ${d.specialization}</option>`).join('')}
            </select>
        </div>
        <div class="form-group">
            <label>Date</label>
            <input type="date" id="apptDate" onchange="loadSlots()">
        </div>
        <div class="form-group">
            <label>Available Slot</label>
            <select id="apptSlot"><option value="">Select date & doctor first</option></select>
        </div>
        <div class="form-group">
            <label>Notes</label>
            <textarea id="apptNotes" rows="2" placeholder="Optional notes..."></textarea>
        </div>
    `, async () => {
        const res = await fetch(API + '/api/appointments', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
                patientId: document.getElementById('apptPatient').value,
                doctorId: document.getElementById('apptDoctor').value,
                slotId: document.getElementById('apptSlot').value,
                date: document.getElementById('apptDate').value,
                notes: document.getElementById('apptNotes').value
            })
        });
        if (res.ok) { closeModal(); loadAppointments(); }
        else { const d = await res.json(); alert(d.error || 'Failed'); }
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
    await fetch(API + `/api/appointments/${id}/complete`, { method: 'PUT' });
    loadAppointments();
}

async function cancelAppointment(id) {
    if (!confirm('Cancel this appointment?')) return;
    await fetch(API + `/api/appointments/${id}/cancel`, {
        method: 'PUT',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ reason: 'Cancelled by staff' })
    });
    loadAppointments();
}

// ==================== MEDICAL RECORDS ====================

async function loadRecords() {
    const records = await fetchJson('/api/records');
    const tbody = document.getElementById('recordsList');
    if (records.length === 0) {
        tbody.innerHTML = '<tr><td colspan="6" class="empty-state"><div class="empty-icon">📋</div><h3>No records found</h3></td></tr>';
        return;
    }
    tbody.innerHTML = records.map(r => `
        <tr>
            <td><strong>${r.recordId}</strong></td>
            <td>${r.patientName}</td>
            <td>${r.doctorName}</td>
            <td>${(r.diagnosisNotes || '').substring(0, 50)}${(r.diagnosisNotes || '').length > 50 ? '...' : ''}</td>
            <td>${statusBadge(r.status)}</td>
            <td>${r.dateCreated ? r.dateCreated.substring(0, 10) : '-'}</td>
        </tr>
    `).join('');
}

function showCreateRecord() {
    showModal('Create Medical Record', `
        <div class="form-row">
            <div class="form-group">
                <label>Patient ID</label>
                <input type="text" id="recPatient" placeholder="e.g. PAT00001">
            </div>
            <div class="form-group">
                <label>Doctor ID</label>
                <input type="text" id="recDoctor" placeholder="e.g. DOC00001">
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
            headers: { 'Content-Type': 'application/json' },
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
    showModal('Add New Medicine', `
        <div class="form-group"><label>Medicine Name</label><input type="text" id="medName" placeholder="e.g. Paracetamol 500mg" required></div>
        <div class="form-row">
            <div class="form-group"><label>Generic Name</label><input type="text" id="medGeneric" placeholder="e.g. Acetaminophen"></div>
            <div class="form-group"><label>Category</label><input type="text" id="medCategory" placeholder="e.g. Analgesic"></div>
        </div>
        <div class="form-row">
            <div class="form-group"><label>Stock Quantity</label><input type="number" id="medStock" value="100"></div>
            <div class="form-group"><label>Unit Price (₹)</label><input type="number" step="0.01" id="medPrice" value="10.00"></div>
        </div>
        <div class="form-row">
            <div class="form-group"><label>Expiry Date</label><input type="date" id="medExpiry" required></div>
            <div class="form-group"><label>Reorder Level</label><input type="number" id="medReorder" value="10"></div>
        </div>
        <div class="form-group"><label>Supplier</label><input type="text" id="medSupplier" placeholder="Supplier name"></div>
    `, async () => {
        const res = await fetch(API + '/api/pharmacy', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
                medicineName: document.getElementById('medName').value,
                genericName: document.getElementById('medGeneric').value,
                category: document.getElementById('medCategory').value,
                stockQuantity: document.getElementById('medStock').value,
                unitPrice: document.getElementById('medPrice').value,
                expiryDate: document.getElementById('medExpiry').value,
                reorderLevel: document.getElementById('medReorder').value,
                supplier: document.getElementById('medSupplier').value
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
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ quantity: qty })
        });
        closeModal();
        loadPharmacy();
    });
}

// ==================== BILLING ====================

async function loadBilling() {
    const invoices = await fetchJson('/api/billing');
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
            headers: { 'Content-Type': 'application/json' },
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
    showModal('Process Payment', `
        <div class="form-group"><label>Amount (₹)</label><input type="number" step="0.01" id="payAmount" value="${amount}"></div>
        <div class="form-group">
            <label>Payment Method</label>
            <select id="payMethod">
                <option value="CASH">Cash</option>
                <option value="CARD">Card</option>
                <option value="UPI">UPI</option>
                <option value="ONLINE">Online</option>
                <option value="INSURANCE">Insurance</option>
            </select>
        </div>
        <div class="form-group"><label>Reference No</label><input type="text" id="payRef" placeholder="Optional reference"></div>
    `, async () => {
        await fetch(API + `/api/billing/${billId}/pay`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
                amount: document.getElementById('payAmount').value,
                method: document.getElementById('payMethod').value,
                referenceNo: document.getElementById('payRef').value
            })
        });
        closeModal();
        loadBilling();
    });
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

async function fetchJson(url) {
    try {
        const res = await fetch(API + url);
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
