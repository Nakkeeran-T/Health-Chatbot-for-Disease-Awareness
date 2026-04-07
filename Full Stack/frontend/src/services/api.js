import axios from 'axios';

const BASE_URL = 'http://localhost:8080/api';

const api = axios.create({
  baseURL: BASE_URL,
  headers: { 'Content-Type': 'application/json' },
});

// Attach JWT token to all requests
api.interceptors.request.use((config) => {
  const token = localStorage.getItem('token');
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

// Handle 401 / 403 - clear token and redirect to login
api.interceptors.response.use(
  (response) => response,
  (error) => {
    const status = error.response?.status;
    if (status === 401 || status === 403) {
      console.warn(`[API] ${status} on ${error.config?.url} – redirecting to login`);
      localStorage.removeItem('token');
      localStorage.removeItem('user');
      window.location.href = '/login';
    }
    return Promise.reject(error);
  }
);

// Auth APIs
export const authAPI = {
  register: (data) => api.post('/auth/register', data),
  login: (data) => api.post('/auth/login', data),
  health: () => api.get('/auth/health'),
};

// Admin APIs
export const adminAPI = {
  getUsers: () => api.get('/admin/users'),
  toggleUserStatus: (id) => api.put(`/admin/users/${id}/toggle`),
  updateRole: (id, role) => api.put(`/admin/users/${id}/role?role=${role}`),
  // Analytics
  getAnalytics: () => api.get('/admin/analytics'),
  // Broadcasting
  broadcast: (data) => api.post('/admin/broadcast', data),
  testVaccineReminder: () => api.post('/admin/broadcast/test-vaccine-reminder'),
  testHealthTip: () => api.post('/admin/broadcast/test-health-tip'),
  // Alert scanner
  runScan: () => api.post('/alerts/admin/run-scan'),
  runWeeklyBulletin: () => api.post('/alerts/admin/run-weekly-bulletin'),
  // WhatsApp test
  testNotify: (phone, message) => api.post(`/alerts/test-notify?phone=${encodeURIComponent(phone)}${message ? `&message=${encodeURIComponent(message)}` : ''}`),
  // Surveillance
  getSurveillance: (district) => api.get(`/alerts/surveillance/${encodeURIComponent(district)}`),
  getDistricts: () => api.get('/alerts/districts'),
  // Webhook status
  getWebhookStatus: () => api.get('/webhook/status'),
};

// Chat APIs
export const ML_BASE_URL = 'http://localhost:5002';

// Normalize Java backend chat response → same shape ChatPage.jsx expects
const normalizeChatResponse = (res) => {
  const d = res.data;
  return {
    ...res,
    data: {
      response:    d.botResponse   ?? d.response   ?? '',
      intent:      d.intent        ?? 'general_query',
      confidence:  d.confidenceScore !== undefined ? d.confidenceScore : (d.confidence ?? 0),
      language:    d.language      ?? 'en',
      suggestions: d.suggestions   ?? [],
    },
  };
};

export const chatAPI = {
  // Route through Java backend — it queries the DB for disease data
  // then falls back to NLPService+DB if the ML API is unavailable
  sendMessage: (data) =>
    api.post('/chat/message', data).then(normalizeChatResponse),

  sendImage: (data) =>
    axios.post(`${ML_BASE_URL}/predict_image`, data, {
      headers: { 'Content-Type': 'multipart/form-data' },
    }),

  getHistory: (sessionId) => api.get(`/chat/history/${sessionId}`),
  getSessions: () => api.get('/chat/sessions'),
};

// Disease APIs
export const diseaseAPI = {
  getAll: () => api.get('/diseases'),
  getById: (id) => api.get(`/diseases/${id}`),
  search: (q) => api.get(`/diseases/search?q=${encodeURIComponent(q)}`),
  getByCategory: (cat) => api.get(`/diseases/category/${cat}`),
  create: (data) => api.post('/diseases', data),
  update: (id, data) => api.put(`/diseases/${id}`, data),
  delete: (id) => api.delete(`/diseases/${id}`),
};

// Vaccine APIs
export const vaccineAPI = {
  getAll: () => api.get('/vaccines'),
  getMandatory: () => api.get('/vaccines/mandatory'),
  create: (data) => api.post('/vaccines', data),
  update: (id, data) => api.put(`/vaccines/${id}`, data),
  delete: (id) => api.delete(`/vaccines/${id}`),
};

// Alert APIs
export const alertAPI = {
  getActive: () => api.get('/alerts'),
  getAll: () => api.get('/alerts/all'),
  getByRegion: (region) => api.get(`/alerts/region/${region}`),
  create: (data) => api.post('/alerts', data),
  update: (id, data) => api.put(`/alerts/${id}`, data),
  delete: (id) => api.delete(`/alerts/${id}`),
};

export default api;
