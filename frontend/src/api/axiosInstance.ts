import axios from 'axios';
import { useAuthStore } from '../store/authStore';

// Create a centralized Axios instance
export const api = axios.create({
  baseURL: 'http://localhost:8080/api/v1',
  headers: {
    'Content-Type': 'application/json',
  },
});

// Request Interceptor: Attach JWT token to every request
api.interceptors.request.use(
  (config) => {
    const token = useAuthStore.getState().token;
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => Promise.reject(error)
);

// Response Interceptor: Handle 401 and 403 globally
api.interceptors.response.use(
  (response) => response,
  (error) => {
    // If the server explicitly says we are unauthorized or forbidden
    if (error.response) {
      if (error.response.status === 401) {
        console.warn('Unauthorized access, logging out.');
        useAuthStore.getState().logout();
        // Redirect will happen automatically because state clears
      } else if (error.response.status === 403) {
        console.warn('Forbidden access, you do not have permissions for this resource.');
        // Could trigger a toast notification here
      }
    }
    return Promise.reject(error);
  }
);
