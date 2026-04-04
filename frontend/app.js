// Mock Database State
let state = {
    currentlyInside: [
        { id: 'KA-05-MM-1212', entryTime: '2026-04-04 08:30', gate: 'Main Gate', type: 'CAMPUS' },
        { id: 'TN-10-XX-4455', entryTime: '2026-04-04 09:15', gate: 'East Gate', type: 'EXTERNAL' }
    ],
    movementHistory: [
        { id: 'KA-05-MM-1212', entryTime: '2026-04-04 08:30', gate: 'Main Gate', exitTime: null, status: 'INSIDE' },
        { id: 'TN-10-XX-4455', entryTime: '2026-04-04 09:15', gate: 'East Gate', exitTime: null, status: 'INSIDE' },
        { id: 'MH-12-AB-9090', entryTime: '2026-04-04 07:00', gate: 'West Gate', exitTime: '2026-04-04 12:00', status: 'LEFT' }
    ],
    incidents: [
        { date: '2026-04-04 10:15', id: 'KA-01-AB-1234', severity: 'HIGH', desc: 'Spotted speeding near main library.' },
        { date: '2026-04-03 14:20', id: 'MH-12-XY-9876', severity: 'MEDIUM', desc: 'Overstayed limit by 2 hours.' }
    ]
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
                <td>${v.id}</td>
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
                <td>${v.id}</td>
                <td>${v.entryTime}</td>
                <td>${v.gate}</td>
                <td>${v.exitTime || '-'}</td>
                <td><span class="tag tag-${v.status === 'INSIDE' ? 'amber' : 'green'}">${v.status}</span></td>
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

// Authentication Mock
loginForm.addEventListener('submit', (e) => {
    e.preventDefault();
    const user = usernameInput.value.trim().toLowerCase();
    
    if (user === 'admin' || user === 'security') {
        loginView.classList.remove('active');
        appView.classList.add('active');
        currentUsernameSpan.textContent = usernameInput.value;
        userRoleBadge.textContent = user;
        
        if (user === 'admin') {
            adminNav.style.display = 'block';
            securityNav.style.display = 'none';
            switchView('admin-dashboard', 'Overview & Stats');
        } else {
            adminNav.style.display = 'none';
            securityNav.style.display = 'block';
            switchView('sec-dashboard', 'Operations');
        }
        showToast(`Welcome back, ${user}!`);
    } else {
        showToast('Invalid credentials. Use "admin" or "security".', 'error');
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

// Mock Forms Submissions
document.querySelectorAll('form:not(#login-form)').forEach(form => {
    form.addEventListener('submit', (e) => {
        e.preventDefault();
        
        if (form.id === 'record-entry-form') {
            const inputs = form.querySelectorAll('input, select');
            const regNumber = inputs[0].value.toUpperCase();
            const gate = inputs[1].value;
            const timeStr = getCurrentTimeStr();
            
            state.currentlyInside.unshift({ id: regNumber, entryTime: timeStr, gate: gate, type: 'GUEST' });
            state.movementHistory.unshift({ id: regNumber, entryTime: timeStr, gate: gate, exitTime: null, status: 'INSIDE' });
            
            showToast('Vehicle entry logged.');
            form.reset();
            renderTables();
            
        } else if (form.id === 'record-exit-form') {
            const inputs = form.querySelectorAll('input');
            const regNumber = inputs[0].value.toUpperCase();
            
            const idx = state.currentlyInside.findIndex(v => v.id === regNumber);
            if (idx !== -1) {
                state.currentlyInside.splice(idx, 1);
                
                // Update history
                const hist = state.movementHistory.find(v => v.id === regNumber && v.status === 'INSIDE');
                if (hist) {
                    hist.exitTime = getCurrentTimeStr();
                    hist.status = 'LEFT';
                }
                showToast(`Vehicle ${regNumber} exited successfully.`);
            } else {
                showToast(`Vehicle ${regNumber} is not currently inside!`, 'error');
            }
            form.reset();
            renderTables();
            
        } else if (form.id === 'record-incident-form') {
            const inputs = form.querySelectorAll('input, select, textarea');
            const regNumber = inputs[0].value.toUpperCase();
            const severity = inputs[1].value;
            const desc = inputs[2].value;
            
            state.incidents.unshift({
                date: getCurrentTimeStr(),
                id: regNumber,
                severity: severity,
                desc: desc
            });
            showToast('Incident recorded.');
            form.reset();
            renderTables();
            
        } else if (form.id === 'quick-search-form') {
            const regNumber = form.querySelector('input').value.toUpperCase();
            const res = document.getElementById('quick-search-result');
            
            // Look in history or currently inside
            const found = state.currentlyInside.find(v => v.id === regNumber);
            res.style.display = 'block';
            if (found) {
                res.innerHTML = `
                    <div style="padding: 10px; background: rgba(37,99,235,0.05); border: 1px solid rgba(37,99,235,0.2); border-radius: 4px;">
                        <strong>Vehicle:</strong> ${found.id} <br>
                        <strong>Type:</strong> ${found.type}<br>
                        <strong>Status:</strong> INSIDE (Gate: ${found.gate})
                    </div>
                `;
            } else {
                res.innerHTML = `<div style="padding: 10px; color: var(--red);">Vehicle not found inside campus.</div>`;
            }
            
        } else if (form.id === 'extended-search-form') {
            const tbody = document.getElementById('extended-search-tbody');
            if (tbody) {
                tbody.innerHTML = state.movementHistory.map(v => `
                    <tr>
                        <td>${v.entryTime}</td>
                        <td>${v.id}</td>
                        <td>${v.gate}</td>
                        <td>${v.exitTime || '-'}</td>
                    </tr>
                `).join('');
                showToast('Search completed.');
            }
            
        } else {
            showToast('Action successful!');
            form.reset();
        }
    });
});
