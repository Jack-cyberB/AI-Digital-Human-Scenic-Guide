import { create } from 'zustand';
import { persist } from 'zustand/middleware';
import type { LoginResponse, DashboardData } from '../types';

interface AdminState {
  token: string | null;
  user: LoginResponse | null;
  isAuthenticated: boolean;
  dashboardData: DashboardData | null;
  setToken: (token: string | null) => void;
  setUser: (user: LoginResponse | null) => void;
  setAuthenticated: (isAuthenticated: boolean) => void;
  setDashboardData: (data: DashboardData | null) => void;
  logout: () => void;
}

export const useAdminStore = create<AdminState>()(
  persist(
    (set) => ({
      token: null,
      user: null,
      isAuthenticated: false,
      dashboardData: null,

      setToken: (token) => set({ token }),

      setUser: (user) => set({ user }),

      setAuthenticated: (isAuthenticated) => set({ isAuthenticated }),

      setDashboardData: (dashboardData) => set({ dashboardData }),

      logout: () =>
        set({
          token: null,
          user: null,
          isAuthenticated: false,
          dashboardData: null,
        }),
    }),
    {
      name: 'admin-storage',
      partialize: (state) => ({
        token: state.token,
        user: state.user,
        isAuthenticated: state.isAuthenticated,
      }),
    }
  )
);
