import { ApiService } from "./api.js";
import { UI } from "./ui.js";
import { WebSocketService } from "./ws.js";

/**
 * Main Application Controller
 */
const App = {
  state: {
    reservas: [],
    usuarios: [],
    recursos: [],
    activeSection: "reservas",
  },

  async init() {
    this.ws = new WebSocketService(() => this.refreshActiveSection());
    this.ws.init();

    this.bindEvents();
    this.showSection("reservas");
  },

  bindEvents() {
    // Navigation
    document.getElementById("mainNav").addEventListener("click", (e) => {
      const btn = e.target.closest(".nav-btn");
      if (btn) this.showSection(btn.dataset.section);
    });

    // Search
    document
      .getElementById("searchReservas")
      .addEventListener("input", () => this.handleFilter("reservas"));
    document
      .getElementById("searchUsuarios")
      .addEventListener("input", () => this.handleFilter("usuarios"));
    document
      .getElementById("searchRecursos")
      .addEventListener("input", () => this.handleFilter("recursos"));

    // Form
    document
      .getElementById("reservaForm")
      .addEventListener("submit", (e) => this.handleReservaSubmit(e));

    // Table Delegation
    document.getElementById("reservasBody").addEventListener("click", (e) => {
      const cancelBtn = e.target.closest(".btn-cancel-reserva");
      if (cancelBtn) {
        const { recurso, local } = cancelBtn.dataset;
        this.handleCancelReserva(recurso, local);
      }
    });
  },

  async showSection(sectionId) {
    this.state.activeSection = sectionId;

    document
      .querySelectorAll(".section")
      .forEach((s) => s.classList.toggle("active", s.id === sectionId));
    document
      .querySelectorAll(".nav-btn")
      .forEach((b) =>
        b.classList.toggle("active", b.dataset.section === sectionId),
      );

    await this.loadSectionData(sectionId);
  },

  async loadSectionData(sectionId) {
    UI.setLoadingState(sectionId, true);
    try {
      switch (sectionId) {
        case "reservas":
          this.state.reservas = await ApiService.getReservas();
          UI.renderReservas(this.state.reservas);
          break;
        case "nueva-reserva": {
          const [recursos, usuarios] = await Promise.all([
            ApiService.getRecursos(),
            ApiService.getUsuarios(),
          ]);
          UI.populateSelect(
            "recurso",
            recursos,
            "id_recurso",
            "nombre",
            "tipo",
          );
          UI.populateSelect(
            "usuario",
            usuarios,
            "id_usuario",
            "nombre",
            "correo_electronico",
          );
          break;
        }
        case "usuarios":
          this.state.usuarios = await ApiService.getUsuarios();
          UI.renderUsuarios(this.state.usuarios);
          break;
        case "recursos":
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
    const search = document
      .getElementById(`search${type.charAt(0).toUpperCase() + type.slice(1)}`)
      .value.toLowerCase();
    let filtered = [];

    if (type === "reservas") {
      filtered = this.state.reservas.filter(
        (r) =>
          r.motivo?.toLowerCase().includes(search) ||
          r.fecha?.includes(search) ||
          String(r.id_usuario).includes(search),
      );
      UI.renderReservas(filtered);
    } else if (type === "usuarios") {
      filtered = this.state.usuarios.filter(
        (u) =>
          u.nombre.toLowerCase().includes(search) ||
          u.correo_electronico.toLowerCase().includes(search),
      );
      UI.renderUsuarios(filtered);
    } else if (type === "recursos") {
      filtered = this.state.recursos.filter(
        (r) =>
          r.nombre.toLowerCase().includes(search) ||
          r.tipo.toLowerCase().includes(search),
      );
      UI.renderRecursos(filtered);
    }
  },

  async handleReservaSubmit(e) {
    e.preventDefault();
    // Staff UI uses inline feedback blocks instead of browser alerts.
    const data = this.getReservaFormData();

    const errors = this.validateReservaForm(data);
    if (errors.length > 0) {
      UI.showError("nueva-reserva", errors.join("\n"));
      return;
    }

    await this.submitReserva(data);
  },

  validateReservaForm(data) {
    const errors = [];
    if (Number.isNaN(data.id_recurso)) errors.push("Resource is mandatory");
    if (Number.isNaN(data.id_usuario)) errors.push("User is mandatory");

    this.validateDate(data.fecha, errors);
    this.validateTimes(data.hora_inicio, data.hora_fin, errors);

    return errors;
  },

  validateDate(fecha, errors) {
    if (fecha) {
      const today = new Date().toISOString().split("T")[0];
      if (fecha < today) {
        errors.push("Cannot create reservation for a past date");
      }
    } else {
      errors.push("Date is mandatory");
    }
  },

  validateTimes(inicio, fin, errors) {
    if (!inicio || inicio === ":00") {
      errors.push("Start time is mandatory");
    }
    if (!fin || fin === ":00") {
      errors.push("End time is mandatory");
    }
    if (inicio && fin && inicio !== ":00" && fin !== ":00" && inicio >= fin) {
      errors.push("Start time must be before end time");
    }
  },

  getReservaFormData() {
    // HTML ids follow frontend naming; the payload keys follow database column names.
    const numPlazas = document.getElementById("numeroPlazas").value;
    const horaInicio = document.getElementById("horaInicio").value;
    const horaFin = document.getElementById("horaFin").value;

    return {
      id_recurso: Number.parseInt(document.getElementById("recurso").value, 10),
      id_usuario: Number.parseInt(document.getElementById("usuario").value, 10),
      fecha: document.getElementById("fecha").value,
      hora_inicio: horaInicio ? horaInicio + ":00" : "",
      hora_fin: horaFin ? horaFin + ":00" : "",
      numero_plazas: numPlazas ? Number.parseInt(numPlazas, 10) : null,
      motivo: document.getElementById("motivo").value || null,
    };
  },

  async submitReserva(data) {
    try {
      // The backend performs the authoritative availability/overlap validation.
      const response = await ApiService.createReserva(data);
      if (response.ok) {
        UI.showFeedback(
          "nueva-reserva",
          "Reservation created successfully!",
          "success",
        );
        document.getElementById("reservaForm").reset();
        await this.showSection("reservas");
      } else {
        const err = await response.json();
        UI.showError("nueva-reserva", err.error || "Creation failed");
      }
    } catch (e) {
      UI.showError("nueva-reserva", "Network error: " + e.message);
    }
  },

  async handleCancelReserva(idRecurso, idReservaLocal) {
    if (!confirm("Cancel this reservation?")) return;
    try {
      const response = await ApiService.deleteReserva(
        idRecurso,
        idReservaLocal,
      );
      if (response.ok) {
        UI.showFeedback("reservas", "Cancelled!", "success");
        this.loadSectionData("reservas");
      } else {
        UI.showError("reservas", "Delete failed");
      }
    } catch (e) {
      UI.showError("reservas", e.message);
    }
  },
};

// Application Boot
document.addEventListener("DOMContentLoaded", () => App.init());
