/**
 * Camada de Comunicacao HTTP com o Backend REST (Fetch API)
 * Suporte a autenticacao e dashboard do cliente via localStorage
 */

export const API_BASE_URL = window.location.origin.includes(':8080')
  ? ''
  : 'http://localhost:8080';

const STORAGE_KEY = 'scheduling_user_session';

/**
 * Obtem a sessao do usuario logado no localStorage
 * @returns {{ id: number, name: string, email: string, role: string, token: string } | null}
 */
export function getStoredUser() {
  try {
    const raw = localStorage.getItem(STORAGE_KEY);
    return raw ? JSON.parse(raw) : null;
  } catch (e) {
    console.error('Erro ao ler usuario do localStorage:', e);
    return null;
  }
}

/**
 * Salva a sessao do usuario no localStorage
 * @param {object} user
 */
export function setStoredUser(user) {
  try {
    localStorage.setItem(STORAGE_KEY, JSON.stringify(user));
  } catch (e) {
    console.error('Erro ao salvar usuario no localStorage:', e);
  }
}

/**
 * Remove a sessao do usuario do localStorage (Logout)
 */
export function clearStoredUser() {
  try {
    localStorage.removeItem(STORAGE_KEY);
  } catch (e) {
    console.error('Erro ao limpar usuario do localStorage:', e);
  }
}

/**
 * Helper generico para requisicoes HTTP com tratamento de erros padronizado
 * @param {string} endpoint - Caminho do recurso (ex: '/api/services')
 * @param {RequestInit} options - Opcoes do Fetch API
 * @returns {Promise<any>} Dados desserializados do JSON
 */
async function request(endpoint, options = {}) {
  const defaultHeaders = {
    'Content-Type': 'application/json',
    'Accept': 'application/json'
  };

  // Anexa o usuario autenticado caso exista na sessao
  const user = getStoredUser();
  if (user && user.id) {
    defaultHeaders['X-User-Id'] = String(user.id);
    if (user.token) {
      defaultHeaders['Authorization'] = `Bearer ${user.token}`;
    }
  }

  const config = {
    ...options,
    headers: {
      ...defaultHeaders,
      ...options.headers
    }
  };

  try {
    const response = await fetch(`${API_BASE_URL}${endpoint}`, config);

    // Se resposta nao for sucesso (ex: 400, 401, 403, 404, 409, 500)
    if (!response.ok) {
      let errorBody = {};
      try {
        errorBody = await response.json();
      } catch (parseError) {
        errorBody = { message: `Erro HTTP: ${response.status} ${response.statusText}` };
      }

      const errorMessage = errorBody.message || `Erro no servidor (Status: ${response.status})`;
      const error = new Error(errorMessage);
      error.status = response.status;
      error.details = errorBody.details || [];
      error.raw = errorBody;
      throw error;
    }

    // Se for 204 No Content
    if (response.status === 204) {
      return null;
    }

    return await response.json();
  } catch (err) {
    if (err.name === 'TypeError' && err.message.includes('fetch')) {
      const networkError = new Error('Não foi possível conectar ao servidor backend (http://localhost:8080). Verifique se a aplicação Spring Boot está rodando.');
      networkError.status = 0;
      throw networkError;
    }
    throw err;
  }
}

/**
 * Objeto de operacoes da API de agendamentos e autenticacao
 */
export const api = {
  // Autenticacao
  login: (email) => request('/api/auth/login', {
    method: 'POST',
    body: JSON.stringify({ email })
  }),

  // Servicos
  getServices: (professionalId) => {
    const query = professionalId ? `?professionalId=${professionalId}` : '';
    return request(`/api/services${query}`);
  },

  getServiceById: (id) => request(`/api/services/${id}`),

  // Usuarios / Clientes
  getUsers: (role) => {
    const query = role ? `?role=${role}` : '';
    return request(`/api/users${query}`);
  },

  // Agendamentos Gerais
  createAppointment: (appointmentData) => request('/api/appointments', {
    method: 'POST',
    body: JSON.stringify(appointmentData)
  }),

  // Dashboard do Cliente Autenticado
  getMyAppointments: () => request('/api/appointments/me'),

  // Cancelamento (DELETE)
  cancelAppointment: (id) => request(`/api/appointments/${id}`, {
    method: 'DELETE'
  }),

  // Reagendamento (PUT)
  rescheduleAppointment: (id, rescheduleData) => request(`/api/appointments/${id}/reschedule`, {
    method: 'PUT',
    body: JSON.stringify(rescheduleData)
  })
};

// Compatibilidade com escopo global se carregado de forma tradicional
if (typeof window !== 'undefined') {
  window.api = api;
  window.getStoredUser = getStoredUser;
  window.setStoredUser = setStoredUser;
  window.clearStoredUser = clearStoredUser;
}
