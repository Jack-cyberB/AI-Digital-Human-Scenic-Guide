import axios, { AxiosInstance, AxiosError } from 'axios';
import type {
  ApiResponse,
  LoginRequest,
  LoginResponse,
  DashboardData,
  KnowledgeItem,
  Notification,
} from '../types';

const BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080';

const api: AxiosInstance = axios.create({
  baseURL: BASE_URL,
  timeout: 30000,
  headers: {
    'Content-Type': 'application/json',
  },
});

api.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('admin_token');
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => {
    return Promise.reject(error);
  }
);

api.interceptors.response.use(
  (response) => response,
  (error: AxiosError) => {
    if (error.response?.status === 401) {
      localStorage.removeItem('admin_token');
      window.location.href = '/login';
    }
    return Promise.reject(error);
  }
);

// 管理员认证
export const adminApi = {
  login: (data: LoginRequest) =>
    api.post<ApiResponse<LoginResponse>>('/api/admin/login', data),

  logout: () => api.post('/api/admin/logout'),

  getProfile: () => api.get<ApiResponse<any>>('/api/admin/profile'),

  validateToken: () => api.get<ApiResponse<boolean>>('/api/admin/validate'),
};

// 知识库管理
export const knowledgeApi = {
  getList: (params: { pageNum?: number; pageSize?: number; category?: string; status?: number }) =>
    api.get<ApiResponse<any>>('/api/knowledge', { params }),

  getAll: () => api.get<ApiResponse<KnowledgeItem[]>>('/api/knowledge/all'),

  getById: (id: number) =>
    api.get<ApiResponse<KnowledgeItem>>(`/api/knowledge/${id}`),

  create: (data: KnowledgeItem) =>
    api.post<ApiResponse<KnowledgeItem>>('/api/knowledge', data),

  update: (id: number, data: KnowledgeItem) =>
    api.put<ApiResponse<KnowledgeItem>>(`/api/knowledge/${id}`, data),

  delete: (id: number) =>
    api.delete<ApiResponse<null>>(`/api/knowledge/${id}`),

  test: (question: string) =>
    api.post<ApiResponse<{ question: string; answer: string }>>('/api/knowledge/test', { question }),
};

// 紧急通知
export const notificationApi = {
  getList: (params: { pageNum?: number; pageSize?: number; status?: string }) =>
    api.get<ApiResponse<any>>('/api/notifications', { params }),

  getActive: () =>
    api.get<ApiResponse<Notification[]>>('/api/notifications/active'),

  getById: (id: number) =>
    api.get<ApiResponse<Notification>>(`/api/notifications/${id}`),

  create: (data: Notification) =>
    api.post<ApiResponse<Notification>>('/api/notifications', data),

  update: (id: number, data: Notification) =>
    api.put<ApiResponse<Notification>>(`/api/notifications/${id}`, data),

  delete: (id: number) =>
    api.delete<ApiResponse<null>>(`/api/notifications/${id}`),
};

// 统计数据
export const statisticsApi = {
  getToday: () => api.get<ApiResponse<any>>('/api/statistics/today'),

  getRealtime: () => api.get<ApiResponse<DashboardData>>('/api/statistics/realtime'),

  getHistory: (startDate: string, endDate: string) =>
    api.get<ApiResponse<any>>('/api/statistics/history', {
      params: { startDate, endDate },
    }),

  getInteractions: (params: {
    page?: number;
    size?: number;
    visitorId?: string;
    startDate?: string;
    endDate?: string;
  }) => api.get<ApiResponse<any>>('/api/statistics/interactions', { params }),
};

export default api;
