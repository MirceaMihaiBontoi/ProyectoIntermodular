import { API_BASE } from './config.js';

/**
 * API Service for backend communication
 */
export const ApiService = {
    async getReservas() {
        const response = await fetch(`${API_BASE}/reservas`);
        return response.json();
    },

    async getRecursos() {
        const response = await fetch(`${API_BASE}/recursos`);
        return response.json();
    },

    async getUsuarios() {
        const response = await fetch(`${API_BASE}/usuarios`);
        return response.json();
    },

    async createReserva(data) {
        return fetch(`${API_BASE}/reservas`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(data)
        });
    },

    async deleteReserva(idRecurso, idReservaLocal) {
        return fetch(`${API_BASE}/reservas/${idRecurso}/${idReservaLocal}`, {
            method: 'DELETE'
        });
    }
};
