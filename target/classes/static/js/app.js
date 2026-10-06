/**
 * Controlador de Interface, Autenticacao e Interacoes do Usuario
 * Gerenciamento de Sessao no localStorage e Dashboard "Meus Agendamentos"
 */

import { api, getStoredUser, setStoredUser, clearStoredUser } from './api.js';

// Estado da Aplicacao
let currentServices = [];
let myAppointments = [];
let selectedServiceForBooking = null;
let appointmentToReschedule = null;

// Instancias de Modais e Toasts do Bootstrap
let bookingModal = null;
let loginModal = null;
let rescheduleModal = null;
let toastInstance = null;

// Elementos do DOM - Navbar & Header Auth
const authSection = document.getElementById('authSection');
const btnOpenLoginModal = document.getElementById('btnOpenLoginModal');

// Elementos do DOM - Secao "Meus Agendamentos"
const myAppointmentsSection = document.getElementById('myAppointmentsSection');
const myAppointmentsContainer = document.getElementById('myAppointmentsContainer');

// Elementos do DOM - Catalogo de Servicos
const servicesContainer = document.getElementById('servicesContainer');
const loadingSpinner = document.getElementById('loadingSpinner');
const errorMessageContainer = document.getElementById('errorMessageContainer');

// Elementos do DOM - Modal de Login
const loginModalEl = document.getElementById('loginModal');
const loginForm = document.getElementById('loginForm');
const loginEmailInput = document.getElementById('loginEmail');
const btnSubmitLogin = document.getElementById('btnSubmitLogin');

// Elementos do DOM - Modal de Agendamento
const bookingModalEl = document.getElementById('bookingModal');
const bookingForm = document.getElementById('bookingForm');
const btnSubmitBooking = document.getElementById('btnSubmitBooking');
const inputBookingServiceId = document.getElementById('serviceId');
const summaryServiceName = document.getElementById('summaryServiceName');
const summaryProfessional = document.getElementById('summaryProfessional');
const summaryDuration = document.getElementById('summaryDuration');
const summaryPrice = document.getElementById('summaryPrice');
const bookingClientName = document.getElementById('bookingClientName');
const inputBookingDate = document.getElementById('appointmentDate');
const inputBookingStartTime = document.getElementById('appointmentStartTime');
const inputBookingEndTime = document.getElementById('appointmentEndTime');
const inputBookingNotes = document.getElementById('appointmentNotes');

// Elementos do DOM - Modal de Reagendamento
const rescheduleModalEl = document.getElementById('rescheduleModal');
const rescheduleForm = document.getElementById('rescheduleForm');
const btnSubmitReschedule = document.getElementById('btnSubmitReschedule');
const rescheduleApptId = document.getElementById('rescheduleApptId');
const rescheduleServiceName = document.getElementById('rescheduleServiceName');
const rescheduleProfessional = document.getElementById('rescheduleProfessional');
const inputRescheduleDate = document.getElementById('rescheduleDate');
const inputRescheduleStartTime = document.getElementById('rescheduleStartTime');
const inputRescheduleEndTime = document.getElementById('rescheduleEndTime');

// Elementos do DOM - Toast
const toastEl = document.getElementById('liveToast');
const toastIcon = document.getElementById('toastIcon');
const toastMessage = document.getElementById('toastMessage');

/**
 * Inicializacao ao carregar o DOM
 */
document.addEventListener('DOMContentLoaded', () => {
  // Inicializar componentes do Bootstrap
  if (bookingModalEl) bookingModal = new bootstrap.Modal(bookingModalEl);
  if (loginModalEl) loginModal = new bootstrap.Modal(loginModalEl);
  if (rescheduleModalEl) rescheduleModal = new bootstrap.Modal(rescheduleModalEl);
  if (toastEl) toastInstance = new bootstrap.Toast(toastEl, { delay: 6000 });

  // Configurar datas minimas como hoje
  setMinDates();

  // Escutar eventos de calculo automatico de termino
  if (inputBookingStartTime) {
    inputBookingStartTime.addEventListener('input', updateBookingCalculatedEndTime);
  }
  if (inputRescheduleStartTime) {
    inputRescheduleStartTime.addEventListener('input', updateRescheduleCalculatedEndTime);
  }

  // Escutar submissoes de formularios
  if (loginForm) loginForm.addEventListener('submit', handleLoginSubmit);
  if (bookingForm) bookingForm.addEventListener('submit', handleBookingSubmit);
  if (rescheduleForm) rescheduleForm.addEventListener('submit', handleRescheduleSubmit);

  // Escutar badges de login rapido no modal de login
  document.querySelectorAll('.quick-login-badge').forEach(badge => {
    badge.addEventListener('click', (e) => {
      const email = e.currentTarget.getAttribute('data-email');
      if (loginEmailInput) loginEmailInput.value = email;
    });
  });

  // Atualizar cabecalho conforme sessao ativa
  updateAuthHeader();

  // Carregar dados iniciais do catalogo e da agenda
  loadInitialData();
});

/**
 * Ajusta data minima nos inputs de data
 */
function setMinDates() {
  const today = new Date();
  const year = today.getFullYear();
  const month = String(today.getMonth() + 1).padStart(2, '0');
  const day = String(today.getDate()).padStart(2, '0');
  const minDateStr = `${year}-${month}-${day}`;

  if (inputBookingDate) inputBookingDate.min = minDateStr;
  if (inputRescheduleDate) inputRescheduleDate.min = minDateStr;
}

/**
 * Atualiza o Header da Navbar com base no estado do usuario no localStorage
 */
function updateAuthHeader() {
  const user = getStoredUser();

  if (!authSection) return;

  if (user) {
    authSection.innerHTML = `
      <div class="d-flex align-items-center gap-3">
        <span class="text-white d-flex align-items-center gap-2">
          <i class="bi bi-person-circle fs-5 text-info"></i>
          <span>Olá, <strong>${escapeHtml(user.name.split(' ')[0])}</strong></span>
        </span>
        <button id="btnLogout" class="btn btn-sm btn-outline-light rounded-pill px-3">
          <i class="bi bi-box-arrow-right"></i> Sair
        </button>
      </div>
    `;

    const btnLogout = document.getElementById('btnLogout');
    if (btnLogout) {
      btnLogout.addEventListener('click', handleLogout);
    }

    if (myAppointmentsSection) {
      myAppointmentsSection.style.display = 'block';
    }
  } else {
    authSection.innerHTML = `
      <button id="btnOpenLoginModal" class="btn btn-sm btn-outline-light rounded-pill px-3" data-bs-toggle="modal" data-bs-target="#loginModal">
        <i class="bi bi-box-arrow-in-right"></i> Entrar
      </button>
    `;

    if (myAppointmentsSection) {
      myAppointmentsSection.style.display = 'none';
    }
  }
}

/**
 * Carrega catalogo de servicos e agendamentos pessoais
 */
async function loadInitialData() {
  showLoading(true);
  hideErrorMessage();

  try {
    const services = await api.getServices();
    currentServices = services || [];
    renderServices(currentServices);

    const user = getStoredUser();
    if (user) {
      await loadMyAppointments();
    }
  } catch (error) {
    console.error('Falha ao carregar dados do backend:', error);
    renderErrorMessage(error.message);
  } finally {
    showLoading(false);
  }
}

/**
 * Busca e renderiza os agendamentos do cliente logado
 */
async function loadMyAppointments() {
  const user = getStoredUser();
  if (!user || !myAppointmentsContainer) return;

  try {
    const appointments = await api.getMyAppointments();
    myAppointments = appointments || [];
    renderMyAppointments(myAppointments);
  } catch (err) {
    console.warn('Erro ao consultar agendamentos do cliente:', err);
    myAppointmentsContainer.innerHTML = `
      <div class="col-12">
        <div class="alert alert-light border text-center p-3 text-muted">
          Não foi possível carregar seus agendamentos no momento.
        </div>
      </div>
    `;
  }
}

/**
 * Renderiza os Cards da secao "Meus Agendamentos"
 * @param {Array} appointments
 */
function renderMyAppointments(appointments) {
  if (!myAppointmentsContainer) return;
  myAppointmentsContainer.innerHTML = '';

  if (!appointments || appointments.length === 0) {
    myAppointmentsContainer.innerHTML = `
      <div class="col-12 state-container py-4">
        <div class="state-icon"><i class="bi bi-calendar2-check text-muted"></i></div>
        <h5 class="fw-bold">Você não possui agendamentos futuros</h5>
        <p class="text-muted small">Escolha um dos procedimentos abaixo para reservar seu horário!</p>
      </div>
    `;
    return;
  }

  appointments.forEach(appt => {
    const col = document.createElement('div');
    col.className = 'col-12 col-md-6 col-lg-4';

    const startDate = new Date(appt.startDateTime);
    const dateFormatted = startDate.toLocaleDateString('pt-BR', {
      weekday: 'short', day: '2-digit', month: '2-digit', year: 'numeric'
    });
    const startTimeFormatted = startDate.toLocaleTimeString('pt-BR', { hour: '2-digit', minute: '2-digit' });

    const endDate = new Date(appt.endDateTime);
    const endTimeFormatted = endDate.toLocaleTimeString('pt-BR', { hour: '2-digit', minute: '2-digit' });

    col.innerHTML = `
      <div class="appointment-card">
        <div>
          <div class="d-flex justify-content-between align-items-center mb-2">
            <span class="badge bg-primary-subtle text-primary border border-primary-subtle rounded-pill">
              <i class="bi bi-check2"></i> ${appt.status}
            </span>
            <span class="small text-muted">ID #${appt.id}</span>
          </div>

          <h4 class="appt-title">${escapeHtml(appt.service?.name || 'Serviço')}</h4>
          <div class="appt-meta">
            <i class="bi bi-person-badge text-muted"></i>
            <span>Profissional: <strong>${escapeHtml(appt.professional?.name || 'Não informado')}</strong></span>
          </div>

          <div class="appt-datetime">
            <i class="bi bi-clock-history"></i>
            <span>${dateFormatted} &bull; ${startTimeFormatted} às ${endTimeFormatted}</span>
          </div>

          ${appt.notes ? `<p class="small text-muted fst-italic mb-2">"${escapeHtml(appt.notes)}"</p>` : ''}
        </div>

        <div class="appt-actions">
          <button class="btn btn-sm btn-outline-primary w-50 btn-reschedule" data-appt-id="${appt.id}">
            <i class="bi bi-arrow-repeat"></i> Reagendar
          </button>
          <button class="btn btn-sm btn-outline-danger w-50 btn-cancel-appt" data-appt-id="${appt.id}">
            <i class="bi bi-x-circle"></i> Cancelar
          </button>
        </div>
      </div>
    `;

    myAppointmentsContainer.appendChild(col);
  });

  // Vincular eventos aos botoes de Cancelar e Reagendar
  document.querySelectorAll('.btn-cancel-appt').forEach(btn => {
    btn.addEventListener('click', (e) => {
      const id = parseInt(e.currentTarget.getAttribute('data-appt-id'), 10);
      handleCancelAppointment(id);
    });
  });

  document.querySelectorAll('.btn-reschedule').forEach(btn => {
    btn.addEventListener('click', (e) => {
      const id = parseInt(e.currentTarget.getAttribute('data-appt-id'), 10);
      openRescheduleModal(id);
    });
  });
}

/**
 * Renderiza os Cards de Servicos disponiveis
 * @param {Array} services
 */
function renderServices(services) {
  if (!servicesContainer) return;
  servicesContainer.innerHTML = '';

  if (!services || services.length === 0) {
    servicesContainer.innerHTML = `
      <div class="col-12 state-container">
        <div class="state-icon"><i class="bi bi-calendar-x"></i></div>
        <h4 class="fw-bold">Nenhum serviço disponível no momento</h4>
        <p class="text-muted">Por favor, volte mais tarde ou contate o estabelecimento.</p>
      </div>
    `;
    return;
  }

  services.forEach(service => {
    const col = document.createElement('div');
    col.className = 'col-12 col-md-6 col-lg-4';

    const formattedPrice = new Intl.NumberFormat('pt-BR', {
      style: 'currency',
      currency: 'BRL'
    }).format(service.price);

    col.innerHTML = `
      <div class="service-card">
        <div class="card-body">
          <div class="d-flex justify-content-between align-items-start mb-2">
            <span class="service-duration">
              <i class="bi bi-clock"></i> ${service.durationMinutes} min
            </span>
            <span class="badge bg-success-subtle text-success border border-success-subtle rounded-pill">
              Disponível
            </span>
          </div>

          <h3 class="service-title">${escapeHtml(service.name)}</h3>
          
          <div class="service-professional">
            <i class="bi bi-person-check-fill text-muted"></i>
            <span>${escapeHtml(service.professional?.name || 'Profissional')}</span>
          </div>

          <p class="service-description">
            ${escapeHtml(service.description || 'Atendimento com produtos de alta qualidade e profissionais capacitados.')}
          </p>

          <div class="service-meta">
            <div>
              <span class="d-block text-muted small">Valor do serviço</span>
              <span class="service-price">${formattedPrice}</span>
            </div>
          </div>

          <button 
            type="button" 
            class="btn btn-navy w-100 btn-open-booking" 
            data-service-id="${service.id}"
          >
            <i class="bi bi-calendar-plus"></i> Agendar Horário
          </button>
        </div>
      </div>
    `;

    servicesContainer.appendChild(col);
  });

  document.querySelectorAll('.btn-open-booking').forEach(btn => {
    btn.addEventListener('click', (e) => {
      const serviceId = parseInt(e.currentTarget.getAttribute('data-service-id'), 10);
      openBookingModal(serviceId);
    });
  });
}

/**
 * Abre o modal de Agendamento
 * @param {number} serviceId
 */
function openBookingModal(serviceId) {
  const user = getStoredUser();

  // Se nao estiver logado, abre modal de login primeiro
  if (!user) {
    showToast('Por favor, faça login com seu e-mail para agendar.', 'info');
    if (loginModal) loginModal.show();
    return;
  }

  selectedServiceForBooking = currentServices.find(s => s.id === serviceId);
  if (!selectedServiceForBooking) return;

  inputBookingServiceId.value = selectedServiceForBooking.id;
  summaryServiceName.textContent = selectedServiceForBooking.name;
  summaryProfessional.textContent = selectedServiceForBooking.professional?.name || 'Profissional';
  summaryDuration.textContent = `${selectedServiceForBooking.durationMinutes} minutos`;
  summaryPrice.textContent = new Intl.NumberFormat('pt-BR', {
    style: 'currency', currency: 'BRL'
  }).format(selectedServiceForBooking.price);

  if (bookingClientName) {
    bookingClientName.textContent = `${user.name} (${user.email})`;
  }

  // Sugere amanha 10:00
  const tomorrow = new Date();
  tomorrow.setDate(tomorrow.getDate() + 1);
  const year = tomorrow.getFullYear();
  const month = String(tomorrow.getMonth() + 1).padStart(2, '0');
  const day = String(tomorrow.getDate()).padStart(2, '0');
  inputBookingDate.value = `${year}-${month}-${day}`;
  inputBookingStartTime.value = '10:00';
  updateBookingCalculatedEndTime();

  inputBookingNotes.value = '';

  bookingModal.show();
}

/**
 * Calcula hora de termino no modal de agendamento
 */
function updateBookingCalculatedEndTime() {
  if (!selectedServiceForBooking || !inputBookingStartTime.value) {
    if (inputBookingEndTime) inputBookingEndTime.value = '';
    return;
  }
  inputBookingEndTime.value = calculateEndTime(inputBookingStartTime.value, selectedServiceForBooking.durationMinutes);
}

/**
 * Trata o envio do agendamento
 */
async function handleBookingSubmit(e) {
  e.preventDefault();
  const user = getStoredUser();
  if (!user) {
    showToast('Você precisa estar logado para agendar.', 'danger');
    bookingModal.hide();
    loginModal.show();
    return;
  }

  const serviceId = parseInt(inputBookingServiceId.value, 10);
  const dateVal = inputBookingDate.value;
  const timeVal = inputBookingStartTime.value;
  const notesVal = inputBookingNotes.value.trim();

  if (!dateVal || !timeVal) {
    showToast('Selecione data e horário válidos.', 'danger');
    return;
  }

  const startDateTime = `${dateVal}T${timeVal}:00`;

  if (new Date(`${dateVal}T${timeVal}`) <= new Date()) {
    showToast('A data e hora do agendamento devem ser futuras.', 'danger');
    return;
  }

  setSubmittingState(btnSubmitBooking, true, 'Confirmando...');

  try {
    await api.createAppointment({
      clientId: user.id,
      serviceId,
      startDateTime,
      notes: notesVal || null
    });

    showToast(`Agendamento confirmado com sucesso para ${dateVal.split('-').reverse().join('/')} às ${timeVal}!`, 'success');
    bookingModal.hide();
    bookingForm.reset();

    // Recarrega lista pessoal
    await loadMyAppointments();
  } catch (error) {
    console.warn('Erro ao criar agendamento:', error);
    if (error.status === 409) {
      showToast(error.message || 'Conflito de agenda: o profissional já possui outro agendamento neste horário.', 'danger');
    } else {
      showToast(error.message || 'Não foi possível confirmar o agendamento.', 'danger');
    }
  } finally {
    setSubmittingState(btnSubmitBooking, false, '<i class="bi bi-check2-circle"></i> Confirmar Agendamento');
  }
}

/**
 * Abre o modal de Reagendamento
 * @param {number} appointmentId
 */
function openRescheduleModal(appointmentId) {
  appointmentToReschedule = myAppointments.find(a => a.id === appointmentId);
  if (!appointmentToReschedule) return;

  rescheduleApptId.value = appointmentToReschedule.id;
  rescheduleServiceName.textContent = appointmentToReschedule.service?.name || 'Serviço';
  rescheduleProfessional.textContent = appointmentToReschedule.professional?.name || 'Profissional';

  // Sugere data atual do agendamento
  const currentStart = new Date(appointmentToReschedule.startDateTime);
  const year = currentStart.getFullYear();
  const month = String(currentStart.getMonth() + 1).padStart(2, '0');
  const day = String(currentStart.getDate()).padStart(2, '0');
  const hours = String(currentStart.getHours()).padStart(2, '0');
  const minutes = String(currentStart.getMinutes()).padStart(2, '0');

  inputRescheduleDate.value = `${year}-${month}-${day}`;
  inputRescheduleStartTime.value = `${hours}:${minutes}`;
  updateRescheduleCalculatedEndTime();

  rescheduleModal.show();
}

/**
 * Calcula hora de termino no modal de reagendamento
 */
function updateRescheduleCalculatedEndTime() {
  if (!appointmentToReschedule || !inputRescheduleStartTime.value) {
    if (inputRescheduleEndTime) inputRescheduleEndTime.value = '';
    return;
  }
  const duration = appointmentToReschedule.service?.durationMinutes || 30;
  inputRescheduleEndTime.value = calculateEndTime(inputRescheduleStartTime.value, duration);
}

/**
 * Trata o envio do reagendamento (PUT /reschedule)
 */
async function handleRescheduleSubmit(e) {
  e.preventDefault();
  if (!appointmentToReschedule) return;

  const dateVal = inputRescheduleDate.value;
  const timeVal = inputRescheduleStartTime.value;

  if (!dateVal || !timeVal) {
    showToast('Selecione nova data e horário válidos.', 'danger');
    return;
  }

  const newStartTime = `${dateVal}T${timeVal}:00`;

  if (new Date(`${dateVal}T${timeVal}`) <= new Date()) {
    showToast('A nova data e hora devem ser estritamente futuras.', 'danger');
    return;
  }

  setSubmittingState(btnSubmitReschedule, true, 'Salvando...');

  try {
    await api.rescheduleAppointment(appointmentToReschedule.id, {
      newStartTime
    });

    showToast('Agendamento reagendado com sucesso!', 'success');
    rescheduleModal.hide();
    rescheduleForm.reset();

    await loadMyAppointments();
  } catch (error) {
    console.warn('Erro ao reagendar:', error);
    if (error.status === 409) {
      showToast(error.message || 'Horário indisponível: o profissional já possui outro agendamento neste horário.', 'danger');
    } else {
      showToast(error.message || 'Não foi possível reagendar o atendimento.', 'danger');
    }
  } finally {
    setSubmittingState(btnSubmitReschedule, false, '<i class="bi bi-arrow-repeat"></i> Confirmar Novo Horário');
  }
}

/**
 * Cancelamento de Agendamento (DELETE)
 * @param {number} appointmentId
 */
async function handleCancelAppointment(appointmentId) {
  const confirmed = window.confirm('Deseja realmente cancelar este agendamento? Esta ação não pode ser desfeita.');
  if (!confirmed) return;

  try {
    await api.cancelAppointment(appointmentId);
    showToast('Agendamento cancelado com sucesso.', 'success');
    await loadMyAppointments();
  } catch (err) {
    console.error('Erro ao cancelar:', err);
    showToast(err.message || 'Não foi possível cancelar o agendamento.', 'danger');
  }
}

/**
 * Submissao de Login (POST /api/auth/login)
 */
async function handleLoginSubmit(e) {
  e.preventDefault();
  const email = loginEmailInput.value.trim();

  if (!email) {
    showToast('Por favor, informe seu e-mail.', 'danger');
    return;
  }

  setSubmittingState(btnSubmitLogin, true, 'Entrando...');

  try {
    const userData = await api.login(email);

    // Salva no localStorage
    setStoredUser(userData);

    showToast(`Bem-vindo(a) de volta, ${userData.name}!`, 'success');
    loginModal.hide();
    loginForm.reset();

    // Atualiza interface
    updateAuthHeader();
    await loadMyAppointments();
  } catch (error) {
    console.warn('Falha no login:', error);
    showToast(error.message || 'E-mail não encontrado ou usuário inativo.', 'danger');
  } finally {
    setSubmittingState(btnSubmitLogin, false, '<i class="bi bi-box-arrow-in-right"></i> Entrar');
  }
}

/**
 * Logout do usuario
 */
function handleLogout() {
  clearStoredUser();
  updateAuthHeader();
  if (myAppointmentsContainer) myAppointmentsContainer.innerHTML = '';
  showToast('Você encerrou sua sessão.', 'info');
}

/**
 * Helper para somar minutos ao horario HH:mm
 */
function calculateEndTime(startTimeStr, durationMinutes) {
  const [hours, minutes] = startTimeStr.split(':').map(Number);
  if (isNaN(hours) || isNaN(minutes)) return '';

  const totalStartMinutes = hours * 60 + minutes;
  const totalEndMinutes = totalStartMinutes + durationMinutes;

  const endHours = Math.floor(totalEndMinutes / 60) % 24;
  const endMins = totalEndMinutes % 60;

  return `${String(endHours).padStart(2, '0')}:${String(endMins).padStart(2, '0')}`;
}

/**
 * Helper para estado de carregamento do botao
 */
function setSubmittingState(button, isSubmitting, text) {
  if (!button) return;
  button.disabled = isSubmitting;
  if (isSubmitting) {
    button.innerHTML = `<span class="spinner-border spinner-border-sm" role="status" aria-hidden="true"></span> ${text}`;
  } else {
    button.innerHTML = text;
  }
}

/**
 * Dispara notificacao Toast
 */
function showToast(message, type = 'success') {
  if (!toastEl || !toastInstance) return;

  toastEl.classList.remove('toast-success', 'toast-danger', 'toast-info');

  if (type === 'success') {
    toastEl.classList.add('toast-success');
    if (toastIcon) toastIcon.className = 'bi bi-check-circle-fill fs-5 text-white';
  } else if (type === 'danger') {
    toastEl.classList.add('toast-danger');
    if (toastIcon) toastIcon.className = 'bi bi-exclamation-triangle-fill fs-5 text-white';
  } else {
    toastEl.classList.add('toast-info');
    if (toastIcon) toastIcon.className = 'bi bi-info-circle-fill fs-5 text-white';
  }

  if (toastMessage) toastMessage.textContent = message;
  toastInstance.show();
}

function showLoading(show) {
  if (loadingSpinner) loadingSpinner.style.display = show ? 'block' : 'none';
}

function renderErrorMessage(message) {
  if (errorMessageContainer) {
    errorMessageContainer.innerHTML = `
      <div class="col-12">
        <div class="alert alert-warning border-0 shadow-sm p-4 text-center rounded-4">
          <i class="bi bi-wifi-off fs-1 text-warning d-block mb-2"></i>
          <h5 class="fw-bold">Falha na conexão com o servidor</h5>
          <p class="text-muted mb-3">${escapeHtml(message)}</p>
          <button class="btn btn-navy" onclick="location.reload()">
            <i class="bi bi-arrow-clockwise"></i> Tentar Novamente
          </button>
        </div>
      </div>
    `;
    errorMessageContainer.style.display = 'block';
  }
}

function hideErrorMessage() {
  if (errorMessageContainer) errorMessageContainer.style.display = 'none';
}

function escapeHtml(str) {
  if (!str) return '';
  const div = document.createElement('div');
  div.textContent = str;
  return div.innerHTML;
}
