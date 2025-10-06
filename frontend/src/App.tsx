import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { ProtectedRoute } from './routes/ProtectedRoute';
import { useAuthStore } from './store/authStore';

// We will create these pages next
import { SidebarLayout } from './components/layout/SidebarLayout';
import { LoginPage } from './pages/auth/LoginPage';
import { RegisterPage } from './pages/auth/RegisterPage';
import { UnauthorizedPage } from './pages/auth/UnauthorizedPage';

// Placeholders for dashboard
const CustomerDashboard = () => <div className="p-8">Customer Dashboard</div>;
const AgentDashboard = () => <div className="p-8">Agent Dashboard</div>;

function App() {
  const { isAuthenticated, role } = useAuthStore();

  return (
    <BrowserRouter>
      <Routes>
        {/* Public Routes */}
        <Route path="/login" element={!isAuthenticated() ? <LoginPage /> : <Navigate to="/" replace />} />
        <Route path="/register" element={!isAuthenticated() ? <RegisterPage /> : <Navigate to="/" replace />} />
        <Route path="/unauthorized" element={<UnauthorizedPage />} />

        {/* Protected Routes (Must be logged in) */}
        <Route element={<ProtectedRoute />}>
          <Route element={<SidebarLayout />}>
            
            {/* Dynamic Root Route based on Role */}
            <Route path="/" element={
              role === 'ROLE_CUSTOMER' ? <Navigate to="/dashboard/customer" replace /> :
              role === 'ROLE_AGENT' ? <Navigate to="/dashboard/agent" replace /> :
              role === 'ROLE_ADMIN' ? <Navigate to="/dashboard/admin" replace /> :
              <Navigate to="/login" replace />
            } />

            {/* Customer Only Routes */}
            <Route element={<ProtectedRoute allowedRoles={['ROLE_CUSTOMER']} />}>
              <Route path="/dashboard/customer" element={<CustomerDashboard />} />
              {/* <Route path="/policies" element={<CustomerPolicies />} /> */}
            </Route>

            {/* Agent / Admin Routes */}
            <Route element={<ProtectedRoute allowedRoles={['ROLE_AGENT', 'ROLE_ADMIN']} />}>
              <Route path="/dashboard/agent" element={<AgentDashboard />} />
              {/* <Route path="/customers" element={<CustomerManagement />} /> */}
            </Route>
            
          </Route>
        </Route>

        {/* Catch all */}
        <Route path="*" element={<Navigate to="/" replace />} />
      </Routes>
    </BrowserRouter>
  );
}

export default App;
