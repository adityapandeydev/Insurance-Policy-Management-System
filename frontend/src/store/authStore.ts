import { create } from 'zustand';
import { persist } from 'zustand/middleware';
import { jwtDecode } from 'jwt-decode';

export type Role = 'ROLE_CUSTOMER' | 'ROLE_AGENT' | 'ROLE_ADMIN';

export interface JwtPayload {
  sub: string; // username/email
  exp: number;
  iat: number;
  role?: Role;
  userId?: number;
}

interface AuthState {
  token: string | null;
  role: Role | null;
  userId: number | null;
  username: string | null;
  setToken: (token: string) => void;
  logout: () => void;
  isAuthenticated: () => boolean;
}

export const useAuthStore = create<AuthState>()(
  persist(
    (set, get) => ({
      token: null,
      role: null,
      userId: null,
      username: null,
      
      setToken: (token: string) => {
        try {
          const decoded = jwtDecode<JwtPayload>(token);
          set({
            token,
            role: decoded.role || null,
            userId: decoded.userId || null,
            username: decoded.sub || null,
          });
        } catch (error) {
          console.error('Invalid token format');
        }
      },
      
      logout: () => {
        set({ token: null, role: null, userId: null, username: null });
      },
      
      isAuthenticated: () => {
        const { token } = get();
        if (!token) return false;
        
        try {
          const decoded = jwtDecode<JwtPayload>(token);
          // Check if token is expired
          if (decoded.exp && decoded.exp * 1000 < Date.now()) {
            get().logout();
            return false;
          }
          return true;
        } catch {
          return false;
        }
      },
    }),
    {
      name: 'insurance-auth-storage', // name of the item in the storage (must be unique)
    }
  )
);
