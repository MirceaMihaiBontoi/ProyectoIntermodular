import { utils } from './utils.js';

/**
 * UI Rendering and DOM management
 */
export const UI = {
    renderReservas(reservas) {
        const tbody = document.getElementById('reservasBody');
        tbody.innerHTML = reservas.map(r => `
            <tr>
                <td>${utils.escapeHTML(String(r.id_recurso))}</td>
                <td>${utils.escapeHTML(String(r.id_usuario))}</td>
                <td>${utils.escapeHTML(r.fecha)}</td>
                <td>${utils.escapeHTML(r.hora_inicio)} - ${utils.escapeHTML(r.hora_fin)}</td>
                <td>${utils.escapeHTML(r.motivo || '-')}</td>
                <td>
                    <button class="btn btn-danger btn-cancel-reserva" 
                            data-recurso="${r.id_recurso}" 
                            data-local="${r.id_reserva_local}">Cancel</button>
                </td>
            </tr>
        `).join('');
    },

    renderUsuarios(usuarios) {
        const grid = document.getElementById('usuariosGrid');
        grid.innerHTML = usuarios.map(u => `
            <div class="user-card">
                <h4>${utils.escapeHTML(u.nombre)}</h4>
                <p><strong>Email:</strong> ${utils.escapeHTML(u.correo_electronico)}</p>
                <p><strong>Type:</strong> <span class="badge ${u.tipo_usuario === 'Administrador' ? 'badge-admin' : 'badge-normal'}">${utils.escapeHTML(u.tipo_usuario)}</span></p>
                <p><strong>Birth Date:</strong> ${utils.escapeHTML(u.fecha_nacimiento || '-')}</p>
            </div>
        `).join('');
    },

    renderRecursos(recursos) {
        const grid = document.getElementById('recursosGrid');
        grid.innerHTML = recursos.map(r => `
            <div class="resource-card">
                <h4>${utils.escapeHTML(r.nombre)}</h4>
                <p><strong>Type:</strong> ${utils.escapeHTML(r.tipo)}</p>
                <p><strong>Description:</strong> ${utils.escapeHTML(r.descripcion || '-')}</p>
                <p><strong>Location:</strong> ${utils.escapeHTML(r.ubicacion || '-')}</p>
                <p><strong>Capacity:</strong> ${utils.escapeHTML(String(r.capacidad || '-'))}</p>
            </div>
        `).join('');
    },

    populateSelect(elementId, items, valueField, textField, extraField = '') {
        const select = document.getElementById(elementId);
        select.innerHTML = `<option value="">Select ${elementId}...</option>` +
            items.map(item => `
                <option value="${item[valueField]}">
                    ${utils.escapeHTML(item[textField])} ${extraField ? `(${utils.escapeHTML(item[extraField])})` : ''}
                </option>
            `).join('');
    },

    getSectionUI(section) {
        return {
            loading: document.getElementById(`${section}Loading`),
            error: document.getElementById(`${section}Error`),
            success: document.getElementById(`${section}Success`),
            display: document.getElementById(`${section}Table`) || document.getElementById(`${section}Grid`)
        };
    },

    setLoadingState(section, isLoading) {
        const ui = this.getSectionUI(section);
        if (ui.loading) ui.loading.style.display = isLoading ? 'block' : 'none';
        if (ui.display) ui.display.style.display = isLoading ? 'none' : (ui.display.tagName === 'TABLE' ? 'table' : 'grid');
        if (ui.error) ui.error.style.display = 'none';
    },

    showFeedback(section, message, type) {
        const div = document.getElementById(`${section}${type.charAt(0).toUpperCase() + type.slice(1)}`);
        if (div) {
            div.textContent = message;
            div.style.display = 'block';
            setTimeout(() => div.style.display = 'none', type === 'success' ? 3000 : 5000);
        }
    },

    showError(section, message) {
        const ui = this.getSectionUI(section);
        if (ui.error) {
            ui.error.textContent = message;
            ui.error.style.display = 'block';
        }
        if (ui.loading) ui.loading.style.display = 'none';
    }
};
