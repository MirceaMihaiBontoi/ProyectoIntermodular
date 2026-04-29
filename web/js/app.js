import { ApiService } from './api.js';
import { UI } from './ui.js';
import { utils } from './utils.js';
import { WebSocketService } from './ws.js';

/**
 * Main Application Controller
 */
const App = {
    state: {
        reservas: [],
        usuarios: [],
        recursos: [],
        activeSection: 'reservas'
    },

    async init() {
        this.ws = new WebSocketService(() => this.refreshActiveSection());
        this.ws.init();
        
        this.bindEvents();
        this.showSection('reservas');
    },

    bindEvents() {
        // Navigation
        document.getElementById('mainNav').addEventListener('click', (e) => {
            const btn = e.target.closest('.nav-btn');
            if (btn) this.showSection(btn.dataset.section);
        });

        // Search
        document.getElementById('searchReservas').addEventListener('input', () => this.handleFilter('reservas'));
        document.getElementById('searchUsuarios').addEventListener('input', () => this.handleFilter('usuarios'));
        document.getElementById('searchRecursos').addEventListener('input', () => this.handleFilter('recursos'));

        // Form
        document.getElementById('reservaForm').addEventListener('submit', (e) => this.handleReservaSubmit(e));

        // Table Delegation
        document.getElementById('reservasBody').addEventListener('click', (e) => {
            const cancelBtn = e.target.closest('.btn-cancel-reserva');
            if (cancelBtn) {
                const { recurso, local } = cancelBtn.dataset;
                this.handleCancelReserva(recurso, local);
            }
        });
    },

    async showSection(sectionId) {
        this.state.activeSection = sectionId;
        
        document.querySelectorAll('.section').forEach(s => s.classList.toggle('active', s.id === sectionId));
        document.querySelectorAll('.nav-btn').forEach(b => b.classList.toggle('active', b.dataset.section === sectionId));
        
        await this.loadSectionData(sectionId);
    },

    async loadSectionData(sectionId) {
        UI.setLoadingState(sectionId, true);
        try {
            switch (sectionId) {
                case 'reservas':
                    this.state.reservas = await ApiService.getReservas();
                    UI.renderReservas(this.state.reservas);
                    break;
                case 'nueva-reserva':
                    const [recursos, usuarios] = await Promise.all([
                        ApiService.getRecursos(),
                        ApiService.getUsuarios()
                    ]);
                    UI.populateSelect('recurso', recursos, 'id_recurso', 'nombre', 'tipo');
                    UI.populateSelect('usuario', usuarios, 'id_usuario', 'nombre', 'correo_electronico');
                    break;
                case 'usuarios':
                    this.state.usuarios = await ApiService.getUsuarios();
                    UI.renderUsuarios(this.state.usuarios);
                    break;
                case 'recursos':
                    this.state.recursos = await ApiService.getRecursos();
                    UI.renderRecursos(this.state.recursos);
                    break;
            }
            UI.setLoadingState(sectionId, false);
        } catch (e) {
            UI.showError(sectionId, `Failed to load ${sectionId}: ${e.message}`);
        }
    },

    refreshActiveSection() {
        this.loadSectionData(this.state.activeSection);
    },

    handleFilter(type) {
        const search = document.getElementById(`search${type.charAt(0).toUpperCase() + type.slice(1)}`).value.toLowerCase();
        let filtered = [];
        
        if (type === 'reservas') {
            filtered = this.state.reservas.filter(r => 
                r.motivo?.toLowerCase().includes(search) || 
                r.fecha?.includes(search) || 
                String(r.id_usuario).includes(search)
            );
            UI.renderReservas(filtered);
        } else if (type === 'usuarios') {
            filtered = this.state.usuarios.filter(u => 
                u.nombre.toLowerCase().includes(search) || 
                u.correo_electronico.toLowerCase().includes(search)
            );
            UI.renderUsuarios(filtered);
        } else if (type === 'recursos') {
            filtered = this.state.recursos.filter(r => 
                r.nombre.toLowerCase().includes(search) || 
                r.tipo.toLowerCase().includes(search)
            );
            UI.renderRecursos(filtered);
        }
    },

    async handleReservaSubmit(e) {
        e.preventDefault();
        const data = {
            id_recurso: Number.parseInt(document.getElementById('recurso').value, 10),
            id_usuario: Number.parseInt(document.getElementById('usuario').value, 10),
            fecha: document.getElementById('fecha').value,
            hora_inicio: document.getElementById('horaInicio').value + ':00',
            hora_fin: document.getElementById('horaFin').value + ':00',
            motivo: document.getElementById('motivo').value
        };

        try {
            const response = await ApiService.createReserva(data);
            if (response.ok) {
                UI.showFeedback('reserva', 'Reservation created!', 'success');
                e.target.reset();
                if (this.state.activeSection === 'reservas') this.loadSectionData('reservas');
            } else {
                const err = await response.json();
                UI.showError('reserva', err.error || 'Creation failed');
            }
        } catch (e) {
            UI.showError('reserva', e.message);
        }
    },

    async handleCancelReserva(idRecurso, idReservaLocal) {
        if (!confirm('Cancel this reservation?')) return;
        try {
            const response = await ApiService.deleteReserva(idRecurso, idReservaLocal);
            if (response.ok) {
                UI.showFeedback('reservas', 'Cancelled!', 'success');
                this.loadSectionData('reservas');
            } else {
                UI.showError('reservas', 'Delete failed');
            }
        } catch (e) {
            UI.showError('reservas', e.message);
        }
    }
};

// Application Boot
document.addEventListener('DOMContentLoaded', () => App.init());
