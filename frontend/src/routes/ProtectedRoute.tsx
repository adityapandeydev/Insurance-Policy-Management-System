import { Navigate, Outlet, useLocation } from 'react-router-dom';
import { useAuthStore, type Role } from '../store/authStore';

interface ProtectedRouteProps {
  allowedRoles?: Role[];
}

export const ProtectedRoute = ({ allowedRoles }: ProtectedRouteProps) => {
  const { isAuthenticated, role } = useAuthStore();
  const location = useLocation();

  // 1. Check if user is authenticated at all
  if (!isAuthenticated()) {
    return <Navigate to="/login" state={{ from: location }} replace />;
  }

  // 2. Check if user has required role (if specific roles are required)
  if (allowedRoles && role && !allowedRoles.includes(role)) {
    return <Navigate to="/unauthorized" replace />;
  }

  // 3. User is authenticated and authorized, render child routes
  return <Outlet />;
};
