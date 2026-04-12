// API Base URL — points to the Java backend HttpServer
const API_BASE = 'http://localhost:8080/api';

// UI state mirrors API responses
let state = {
    currentlyInside: [],
    movementHistory: [],
    incidents: [],
    users: []
};

// DOM Elements
const loginView = document.getElementById('login-view');
const appView = document.getElementById('app-view');
const loginForm = document.getElementById('login-form');
const usernameInput = document.getElementById('username');
const passwordInput = document.getElementById('password');

const currentUsernameSpan = document.getElementById('current-username');
const userRoleBadge = document.getElementById('user-role-badge');
const logoutBtn = document.getElementById('logout-btn');

const adminNav = document.getElementById('admin-nav');
const securityNav = document.getElementById('security-nav');
const pageTitle = document.getElementById('page-title');
const sections = document.querySelectorAll('.page-section');

// Clock Update
function updateClock() {
    const now = new Date();
    document.getElementById('clock').textContent = now.toLocaleTimeString();
}
setInterval(updateClock, 1000);
updateClock();

// Toast Notification System
function showToast(message, type = 'success') {
    const container = document.getElementById('toast-container');
    const toast = document.createElement('div');
    toast.className = `toast ${type}`;
    toast.innerHTML = `
        <svg viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor" stroke-width="2">
            ${type === 'success' 
                ? '<path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"></path><polyline points="22 4 12 14.01 9 11.01"></polyline>'
                : '<circle cx="12" cy="12" r="10"></circle><line x1="15" y1="9" x2="9" y2="15"></line><line x1="9" y1="9" x2="15" y2="15"></line>'}
        </svg>
        <span>${message}</span>
    `;
    container.appendChild(toast);
    setTimeout(() => {
        if(container.contains(toast)) container.removeChild(toast);
    }, 3000);
}

// Rendering Logic
function renderTables() {
    // 1. Incidents Table
    const incidentsTbody = document.getElementById('incidents-tbody');
    if (incidentsTbody) {
        incidentsTbody.innerHTML = state.incidents.map(inc => `
            <tr>
                <td>${inc.date}</td>
                <td>${inc.id}</td>
                <td><span class="tag tag-${inc.severity === 'HIGH' ? 'red' : (inc.severity === 'MEDIUM' ? 'amber' : 'green')}">${inc.severity}</span></td>
                <td>${inc.desc}</td>
            </tr>
        `).join('');
    }

    // 2. Currently Inside Table
    const currentlyInsideTbody = document.getElementById('currently-inside-tbody');
    if (currentlyInsideTbody) {
        currentlyInsideTbody.innerHTML = state.currentlyInside.length === 0 ? `<tr><td colspan="4">No vehicles inside.</td></tr>` : 
        state.currentlyInside.map(v => `
            <tr>
                <td>${v.regNumber}</td>
                <td>${v.entryTime}</td>
                <td>${v.gate}</td>
                <td><span class="tag tag-${v.type === 'CAMPUS' ? 'blue' : 'gray'}">${v.type}</span></td>
            </tr>
        `).join('');
    }

    // 3. Movement History Table
    const historyTbody = document.getElementById('movement-history-tbody');
    if (historyTbody) {
        historyTbody.innerHTML = state.movementHistory.length === 0 ? `<tr><td colspan="5">No history found.</td></tr>` : 
        state.movementHistory.map(v => `
            <tr>
                <td>${v.regNumber}</td>
                <td>${v.entryTime}</td>
                <td>${v.gate}</td>
                <td>${v.exitTime || '-'}</td>
                <td><span class="tag tag-${v.status === 'INSIDE' ? 'amber' : 'green'}">${v.status}</span></td>
            </tr>
        `).join('');
    }

    // 4. Admin Users Table
    const usersTbody = document.getElementById('users-tbody');
    if (usersTbody) {
        usersTbody.innerHTML = state.users.length === 0
            ? `<tr><td colspan="2">No users found.</td></tr>`
            : state.users.map(user => `
                <tr>
                    <td>${user.username}</td>
                    <td><span class="tag tag-${user.role === 'ADMIN' ? 'blue' : 'gray'}">${user.role}</span></td>
                </tr>
            `).join('');
    }
}

// Navigation Logic
function switchView(targetSectionId, title) {
    // Hide all
    sections.forEach(sec => sec.classList.add('hidden'));
    document.querySelectorAll('.nav-list a').forEach(a => a.classList.remove('active'));
    
    // Show target
    const targetEl = document.getElementById(targetSectionId);
    if(targetEl) targetEl.classList.remove('hidden');
    
    // Update active nav
    const activeNav = document.querySelector(`.nav-list a[data-target="${targetSectionId}"]`);
    if(activeNav) activeNav.classList.add('active');
    
    // Set title
    pageTitle.textContent = title || activeNav?.textContent || 'Dashboard';
    
    // Rerender tables when navigating
    renderTables();

    // Load live data from API for relevant sections
    if (targetSectionId === 'sec-currently-inside') {
        loadVehiclesInside();
    } else if (targetSectionId === 'admin-dashboard') {
        loadVehiclesInside();
        loadDashboardSummary();
    } else if (targetSectionId === 'admin-incidents') {
        loadIncidents();
    } else if (targetSectionId === 'admin-users') {
        loadUsers();
    }
}

// =========================================================================
// API Data Loaders
// =========================================================================

/** Fetch vehicles currently inside from API and update state + UI */
async function loadVehiclesInside() {
    try {
        const response = await fetch(`${API_BASE}/vehicles/inside`);
        const result = await response.json();
        if (result.success) {
            state.currentlyInside = Array.isArray(result.data) ? result.data : [];
            // Update admin dashboard stat card (count from same query = consistent)
            const countEl = document.getElementById('stat-currently-inside');
            if (countEl) countEl.textContent = result.count ?? state.currentlyInside.length;
            renderTables();
        }
    } catch (err) {
        console.error('Failed to load vehicles inside:', err);
    }
}

/** Fetch dashboard summary metrics from API. */
async function loadDashboardSummary() {
    try {
        const response = await fetch(`${API_BASE}/dashboard/summary`);
        const result = await response.json();
        if (!result.success) return;

        const entriesEl = document.getElementById('stat-entries-today');
        const exitsEl = document.getElementById('stat-exits-today');
        const overstayEl = document.getElementById('stat-overstay-count');

        if (entriesEl) entriesEl.textContent = result.entriesToday ?? 0;
        if (exitsEl) exitsEl.textContent = result.exitsToday ?? 0;
        if (overstayEl) overstayEl.textContent = result.overstayCount ?? 0;

        const distribution = result.distribution || {};
        const distCampus = document.getElementById('dist-campus');
        const distCab = document.getElementById('dist-cab');
        const distDelivery = document.getElementById('dist-delivery');
        const distWork = document.getElementById('dist-work');
        const distExternal = document.getElementById('dist-external');

        if (distCampus) distCampus.textContent = distribution.CAMPUS ?? 0;
        if (distCab) distCab.textContent = distribution.CAB ?? 0;
        if (distDelivery) distDelivery.textContent = distribution.DELIVERY ?? 0;
        if (distWork) distWork.textContent = distribution.WORK ?? 0;
        if (distExternal) distExternal.textContent = distribution.EXTERNAL ?? 0;
    } catch (err) {
        console.error('Failed to load dashboard summary:', err);
    }
}

/** Fetch incidents list from API and update state + table. */
async function loadIncidents() {
    try {
        const response = await fetch(`${API_BASE}/incidents`);
        const result = await response.json();
        if (result.success) {
            state.incidents = Array.isArray(result.data) ? result.data : [];
            renderTables();
        }
    } catch (err) {
        console.error('Failed to load incidents:', err);
    }
}

/** Fetch all users from API and update admin users table. */
async function loadUsers() {
    try {
        const response = await fetch(`${API_BASE}/users`);
        const result = await response.json();
        if (result.success) {
            state.users = Array.isArray(result.data) ? result.data : [];
            renderTables();
        }
    } catch (err) {
        console.error('Failed to load users:', err);
    }
}

// Initial render
renderTables();

// Bind Navigation
document.querySelectorAll('.nav-list a').forEach(link => {
    link.addEventListener('click', (e) => {
        e.preventDefault();
        switchView(e.target.dataset.target, e.target.textContent);
    });
});

// Authentication — calls backend API
loginForm.addEventListener('submit', async (e) => {
    e.preventDefault();
    const user = usernameInput.value.trim();
    const pass = passwordInput.value.trim();

    if (!user || !pass) {
        showToast('Please enter username and password.', 'error');
        return;
    }

    try {
        const response = await fetch(`${API_BASE}/login`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ username: user, password: pass })
        });

        const data = await response.json();

        if (data.success) {
            loginView.classList.remove('active');
            appView.classList.add('active');
            currentUsernameSpan.textContent = data.username;
            userRoleBadge.textContent = data.role;

            if (data.role === 'ADMIN') {
                adminNav.style.display = 'block';
                securityNav.style.display = 'none';
                switchView('admin-dashboard', 'Overview & Stats');
            } else {
                adminNav.style.display = 'none';
                securityNav.style.display = 'block';
                switchView('sec-dashboard', 'Operations');
            }
            showToast(`Welcome back, ${data.username}!`);
        } else {
            showToast(data.message || 'Login failed.', 'error');
        }
    } catch (err) {
        showToast('Cannot connect to server. Is the backend running?', 'error');
    }
});

logoutBtn.addEventListener('click', () => {
    appView.classList.remove('active');
    loginView.classList.add('active');
    loginForm.reset();
    showToast('Logged out successfully.');
});

// Helper for formatted time
function getCurrentTimeStr() {
    const now = new Date();
    return now.getFullYear() + '-' + String(now.getMonth() + 1).padStart(2, '0') + '-' + 
           String(now.getDate()).padStart(2, '0') + ' ' + String(now.getHours()).padStart(2, '0') + 
           ':' + String(now.getMinutes()).padStart(2, '0');
}

// Form submissions
document.querySelectorAll('form:not(#login-form)').forEach(form => {
    form.addEventListener('submit', (e) => {
        e.preventDefault();
        
        if (form.id === 'record-entry-form') {
            const inputs = form.querySelectorAll('input, select');

            const regNumber = inputs[0].value.toUpperCase().trim();
            const gate = inputs[1].value;
            const purpose = (inputs[2] && inputs[2].value) ? inputs[2].value : '';

            fetch(`${API_BASE}/entry`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ regNumber, gate, purpose })
            })
            .then(res => res.json())
            .then(data => {
                if (data.success) {
                    showToast(data.message || 'Vehicle entry logged.');
                    form.reset();
                    loadVehiclesInside(); // refresh data
                    loadDashboardSummary();
                } else {
                    const message = data.message || 'Entry failed.';
                    if (message.toLowerCase().includes('not registered')) {
                        showToast(`${message} Use the Register Vehicle card in Operations.`, 'error');
                    } else {
                        showToast(message, 'error');
                    }
                }
            })
            .catch(() => showToast('Cannot connect to server.', 'error'));
            
        } else if (form.id === 'record-exit-form') {
            const inputs = form.querySelectorAll('input');
            const regNumber = inputs[0].value.toUpperCase().trim();
            const justification = (inputs[1] && inputs[1].value) ? inputs[1].value.trim() : '';

            fetch(`${API_BASE}/exit`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ regNumber, justification })
            })
            .then(res => res.json())
            .then(data => {
                if (data.success) {
                    showToast(data.message || 'Vehicle exit recorded.');
                    form.reset();
                    loadVehiclesInside(); // refresh list/count from DB-backed endpoint
                    loadDashboardSummary();
                } else {
                    showToast(data.message || 'Exit failed.', 'error');
                }
            })
            .catch(() => showToast('Cannot connect to server.', 'error'));
            
        } else if (form.id === 'record-incident-form') {
            const inputs = form.querySelectorAll('input, select, textarea');
            const regNumber = inputs[0].value.toUpperCase().trim();
            const severity = inputs[1].value;
            const description = inputs[2].value.trim();

            fetch(`${API_BASE}/incidents`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ regNumber, severity, description })
            })
            .then(res => res.json())
            .then(data => {
                if (data.success) {
                    showToast(data.message || 'Incident recorded.');
                    form.reset();
                    loadIncidents();
                } else {
                    showToast(data.message || 'Failed to record incident.', 'error');
                }
            })
            .catch(() => showToast('Cannot connect to server.', 'error'));
            
            
            
        } else if (form.id === 'quick-search-form') {
            const regNumber = form.querySelector('input').value.toUpperCase().trim();
            const res = document.getElementById('quick-search-result');
            res.style.display = 'block';

            fetch(`${API_BASE}/vehicle/search?regNumber=${encodeURIComponent(regNumber)}`)
            .then(async (resp) => {
                const data = await resp.json();
                return { ok: resp.ok, data };
            })
            .then(({ ok, data }) => {
                if (!ok || !data.success) {
                    res.innerHTML = `<div style="padding: 10px; color: var(--red);">${data.message || 'Vehicle not found.'}</div>`;
                    return;
                }

                const gateText = data.gate ? ` (Gate: ${data.gate})` : '';
                const exitText = data.exitTime ? `<br><strong>Last Exit:</strong> ${data.exitTime}` : '';
                res.innerHTML = `
                    <div style="padding: 10px; background: rgba(37,99,235,0.05); border: 1px solid rgba(37,99,235,0.2); border-radius: 4px;">
                        <strong>Vehicle:</strong> ${data.regNumber} <br>
                        <strong>Type:</strong> ${data.type}<br>
                        <strong>Status:</strong> ${data.status}${gateText}
                        ${data.entryTime ? `<br><strong>Last Entry:</strong> ${data.entryTime}` : ''}
                        ${exitText}
                    </div>
                `;
            })
            .catch(() => {
                res.innerHTML = `<div style="padding: 10px; color: var(--red);">Cannot connect to server.</div>`;
            });

        } else if (form.id === 'movement-history-form') {
            const regNumberInput = document.getElementById('movement-history-reg');
            const regNumber = regNumberInput ? regNumberInput.value.toUpperCase().trim() : '';

            fetch(`${API_BASE}/vehicle/movement?regNumber=${encodeURIComponent(regNumber)}`)
            .then(async (resp) => {
                const data = await resp.json();
                return { ok: resp.ok, data };
            })
            .then(({ ok, data }) => {
                if (ok && data.success) {
                    state.movementHistory = Array.isArray(data.data) ? data.data : [];
                    renderTables();
                    showToast('Movement history loaded.');
                } else {
                    state.movementHistory = [];
                    renderTables();
                    showToast(data.message || 'No movement history found.', 'error');
                }
            })
            .catch(() => {
                state.movementHistory = [];
                renderTables();
                showToast('Cannot connect to server.', 'error');
            });
            
        } else if (form.id === 'extended-search-form') {
            const inputs = form.querySelectorAll('input');
            const regNumber = (inputs[0] && inputs[0].value) ? inputs[0].value.toUpperCase().trim() : '';
            const dateFrom = (inputs[1] && inputs[1].value) ? inputs[1].value.trim() : '';
            const dateTo = (inputs[2] && inputs[2].value) ? inputs[2].value.trim() : '';

            const params = new URLSearchParams();
            if (regNumber) params.set('regNumber', regNumber);
            if (dateFrom) params.set('dateFrom', dateFrom);
            if (dateTo) params.set('dateTo', dateTo);

            const url = `${API_BASE}/logs/search${params.toString() ? `?${params.toString()}` : ''}`;
            const tbody = document.getElementById('extended-search-tbody');
            fetch(url)
            .then(res => res.json())
            .then(data => {
                if (!tbody) return;
                if (data.success) {
                    const rows = Array.isArray(data.data) ? data.data : [];
                    tbody.innerHTML = rows.length === 0
                        ? `<tr><td colspan="4" class="text-center">No matching logs found.</td></tr>`
                        : rows.map(v => `
                            <tr>
                                <td>${v.entryTime}</td>
                                <td>${v.regNumber}</td>
                                <td>${v.gate}</td>
                                <td>${v.exitTime || '-'}</td>
                            </tr>
                        `).join('');
                    showToast('Search completed.');
                } else {
                    tbody.innerHTML = `<tr><td colspan="4" class="text-center">No matching logs found.</td></tr>`;
                    showToast(data.message || 'Search failed.', 'error');
                }
            })
            .catch(() => {
                if (tbody) {
                    tbody.innerHTML = `<tr><td colspan="4" class="text-center">Unable to load logs.</td></tr>`;
                }
                showToast('Cannot connect to server.', 'error');
            });
            
        } else if (form.id === 'register-vehicle-form' || form.id === 'register-vehicle-ops-form') {
            const regNumberField = form.elements.namedItem('regNumber');
            const typeField = form.elements.namedItem('type');
            const ownerNameField = form.elements.namedItem('ownerName');
            const ownerContactField = form.elements.namedItem('ownerContact');

            const regNumber = regNumberField ? regNumberField.value.toUpperCase().trim() : '';
            const type = typeField ? typeField.value : '';
            const ownerName = ownerNameField ? ownerNameField.value.trim() : '';
            const ownerContact = ownerContactField ? ownerContactField.value.trim() : '';

            fetch(`${API_BASE}/vehicle/register`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ regNumber, type, ownerName, ownerContact })
            })
            .then(res => res.json())
            .then(data => {
                if (data.success) {
                    showToast(data.message || 'Vehicle registered.');
                    form.reset();
                } else {
                    showToast(data.message || 'Registration failed.', 'error');
                }
            })
            .catch(() => showToast('Cannot connect to server.', 'error'));

        } else if (form.id === 'register-security-form') {
            const newUser = document.getElementById('new-sec-user').value.trim();
            const newPass = document.getElementById('new-sec-pass').value.trim();

            if (!newUser || !newPass) {
                showToast('Please enter both username and password.', 'error');
                return;
            }

            fetch(`${API_BASE}/register`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ username: newUser, password: newPass })
            })
            .then(res => res.json())
            .then(data => {
                if (data.success) {
                    showToast(data.message || 'Security user registered.');
                    form.reset();
                    loadUsers();
                } else {
                    showToast(data.message || 'Registration failed.', 'error');
                }
            })
            .catch(() => showToast('Cannot connect to server.', 'error'));

        } else {
            showToast('Action successful!');
            form.reset();
        }
    });
});

const refreshInsideBtn = document.getElementById('refresh-inside-btn');
if (refreshInsideBtn) {
    refreshInsideBtn.addEventListener('click', () => {
        loadVehiclesInside();
        loadDashboardSummary();
    });
}

const refreshUsersBtn = document.getElementById('refresh-users-btn');
if (refreshUsersBtn) {
    refreshUsersBtn.addEventListener('click', () => {
        loadUsers();
    });
}
