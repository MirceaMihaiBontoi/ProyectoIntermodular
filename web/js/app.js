const API_BASE = 'http://localhost:3000/api';
let reservasData = [];
let usuariosData = [];
let recursosData = [];

// Navigation
function showSection(sectionId) {
    document.querySelectorAll('.section').forEach(s => s.classList.remove('active'));
    document.querySelectorAll('.nav button').forEach(b => b.classList.remove('active'));
    
    document.getElementById(sectionId).classList.add('active');
    event.target.classList.add('active');

    if (sectionId === 'reservas') loadReservas();
    if (sectionId === 'nueva-reserva') loadFormData();
    if (sectionId === 'usuarios') loadUsuarios();
    if (sectionId === 'recursos') loadRecursos();
}

// Load Reservas
async function loadReservas() {
    const loading = document.getElementById('reservasLoading');
    const error = document.getElementById('reservasError');
    const table = document.getElementById('reservasTable');

    loading.style.display = 'block';
    error.style.display = 'none';
    table.style.display = 'none';

    try {
        const response = await fetch(`${API_BASE}/reservas`);
        reservasData = await response.json();
        
        renderReservas(reservasData);
        loading.style.display = 'none';
        table.style.display = 'table';
    } catch (e) {
        error.textContent = 'Error loading reservations: ' + e.message;
        error.style.display = 'block';
        loading.style.display = 'none';
    }
}

function renderReservas(reservas) {
    const tbody = document.getElementById('reservasBody');
    tbody.innerHTML = reservas.map(r => `
        <tr>
            <td>${r.id_recurso}</td>
            <td>${r.id_usuario}</td>
            <td>${r.fecha}</td>
            <td>${r.hora_inicio} - ${r.hora_fin}</td>
            <td>${r.motivo || '-'}</td>
            <td>
                <button class="btn btn-danger" onclick="cancelReserva('${r.id_recurso}', '${r.id_reserva_local}')">Cancel</button>
            </td>
        </tr>
    `).join('');
}

function filterReservas() {
    const search = document.getElementById('searchReservas').value.toLowerCase();
    const filtered = reservasData.filter(r => 
        r.motivo?.toLowerCase().includes(search) ||
        r.fecha?.includes(search) ||
        r.id_usuario.toString().includes(search)
    );
    renderReservas(filtered);
}

async function cancelReserva(idRecurso, idReservaLocal) {
    if (!confirm('Are you sure you want to cancel this reservation?')) return;

    try {
        const response = await fetch(`${API_BASE}/reservas/${idRecurso}/${idReservaLocal}`, {
            method: 'DELETE'
        });
        
        if (response.ok) {
            showSuccess('reservas', 'Reservation cancelled successfully');
            loadReservas();
        } else {
            showError('reservas', 'Error cancelling reservation');
        }
    } catch (e) {
        showError('reservas', 'Error: ' + e.message);
    }
}

// Load Form Data
async function loadFormData() {
    try {
        const [recursos, usuarios] = await Promise.all([
            fetch(`${API_BASE}/recursos`).then(r => r.json()),
            fetch(`${API_BASE}/usuarios`).then(r => r.json())
        ]);

        const recursoSelect = document.getElementById('recurso');
        recursoSelect.innerHTML = '<option value="">Select resource...</option>' +
            recursos.map(r => `<option value="${r.id_recurso}">${r.nombre} (${r.tipo})</option>`).join('');

        const usuarioSelect = document.getElementById('usuario');
        usuarioSelect.innerHTML = '<option value="">Select user...</option>' +
            usuarios.map(u => `<option value="${u.id_usuario}">${u.nombre} (${u.correo_electronico})</option>`).join('');
    } catch (e) {
        showError('reserva', 'Error loading form data');
    }
}

// Create Reservation
document.getElementById('reservaForm').addEventListener('submit', async (e) => {
    e.preventDefault();

    const data = {
        id_recurso: parseInt(document.getElementById('recurso').value),
        id_reserva_local: Date.now(), // Simple ID generation
        id_usuario: parseInt(document.getElementById('usuario').value),
        fecha: document.getElementById('fecha').value,
        hora_inicio: document.getElementById('horaInicio').value + ':00',
        hora_fin: document.getElementById('horaFin').value + ':00',
        motivo: document.getElementById('motivo').value
    };

    try {
        const response = await fetch(`${API_BASE}/reservas`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(data)
        });

        if (response.ok) {
            showSuccess('reserva', 'Reservation created successfully');
            document.getElementById('reservaForm').reset();
        } else {
            showError('reserva', 'Error creating reservation');
        }
    } catch (e) {
        showError('reserva', 'Error: ' + e.message);
    }
});

// Load Users
async function loadUsuarios() {
    const loading = document.getElementById('usuariosLoading');
    const error = document.getElementById('usuariosError');
    const grid = document.getElementById('usuariosGrid');

    loading.style.display = 'block';
    error.style.display = 'none';
    grid.style.display = 'none';

    try {
        const response = await fetch(`${API_BASE}/usuarios`);
        usuariosData = await response.json();
        
        renderUsuarios(usuariosData);
        loading.style.display = 'none';
        grid.style.display = 'grid';
    } catch (e) {
        error.textContent = 'Error loading users: ' + e.message;
        error.style.display = 'block';
        loading.style.display = 'none';
    }
}

function renderUsuarios(usuarios) {
    const grid = document.getElementById('usuariosGrid');
    grid.innerHTML = usuarios.map(u => `
        <div class="user-card">
            <h4>${u.nombre}</h4>
            <p><strong>Email:</strong> ${u.correo_electronico}</p>
            <p><strong>Type:</strong> <span class="badge ${u.tipo_usuario === 'Administrador' ? 'badge-admin' : 'badge-normal'}">${u.tipo_usuario}</span></p>
            <p><strong>Birth Date:</strong> ${u.fecha_nacimiento || '-'}</p>
        </div>
    `).join('');
}

function filterUsuarios() {
    const search = document.getElementById('searchUsuarios').value.toLowerCase();
    const filtered = usuariosData.filter(u => 
        u.nombre.toLowerCase().includes(search) ||
        u.correo_electronico.toLowerCase().includes(search)
    );
    renderUsuarios(filtered);
}

// Load Resources
async function loadRecursos() {
    const loading = document.getElementById('recursosLoading');
    const error = document.getElementById('recursosError');
    const grid = document.getElementById('recursosGrid');

    loading.style.display = 'block';
    error.style.display = 'none';
    grid.style.display = 'none';

    try {
        const response = await fetch(`${API_BASE}/recursos`);
        recursosData = await response.json();
        
        renderRecursos(recursosData);
        loading.style.display = 'none';
        grid.style.display = 'grid';
    } catch (e) {
        error.textContent = 'Error loading resources: ' + e.message;
        error.style.display = 'block';
        loading.style.display = 'none';
    }
}

function renderRecursos(recursos) {
    const grid = document.getElementById('recursosGrid');
    grid.innerHTML = recursos.map(r => `
        <div class="resource-card">
            <h4>${r.nombre}</h4>
            <p><strong>Type:</strong> ${r.tipo}</p>
            <p><strong>Description:</strong> ${r.descripcion || '-'}</p>
            <p><strong>Location:</strong> ${r.ubicacion || '-'}</p>
            <p><strong>Capacity:</strong> ${r.capacidad || '-'}</p>
        </div>
    `).join('');
}

function filterRecursos() {
    const search = document.getElementById('searchRecursos').value.toLowerCase();
    const filtered = recursosData.filter(r => 
        r.nombre.toLowerCase().includes(search) ||
        r.tipo.toLowerCase().includes(search)
    );
    renderRecursos(filtered);
}

// Utility functions
function showError(section, message) {
    const error = document.getElementById(section + 'Error');
    error.textContent = message;
    error.style.display = 'block';
    setTimeout(() => error.style.display = 'none', 5000);
}

function showSuccess(section, message) {
    const success = document.getElementById(section + 'Success');
    success.textContent = message;
    success.style.display = 'block';
    setTimeout(() => success.style.display = 'none', 5000);
}

// Initial load
loadReservas();
