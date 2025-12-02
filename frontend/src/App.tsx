import { BrowserRouter as Router, Routes, Route, Navigate } from 'react-router-dom';
import { LoginPage } from './pages/auth/LoginPage';
import { RegisterPage } from './pages/auth/RegisterPage';
import { UnauthorizedPage } from './pages/auth/UnauthorizedPage';
import { SidebarLayout } from './components/layout/SidebarLayout';
import { ProtectedRoute } from './routes/ProtectedRoute';
import { useAuthStore } from './store/authStore';
import { CustomerDashboard } from './pages/dashboard/CustomerDashboard';
import { CustomerSettings } from './pages/dashboard/CustomerSettings';
import { CustomerListPage } from './pages/dashboard/CustomerListPage';
import { BuyPolicyPage } from './pages/dashboard/BuyPolicyPage';
import { SubmitClaimPage } from './pages/dashboard/SubmitClaimPage';
import { PolicyListPage } from './pages/dashboard/PolicyListPage';
import { ClaimListPage } from './pages/dashboard/ClaimListPage';
import { AgentDashboard } from './pages/dashboard/AgentDashboard';
import { CustomerDetailPage } from './pages/dashboard/CustomerDetailPage';

// A simple component to redirect users to their specific dashboard based on role
const DashboardRouter = () => {
  const role = useAuthStore((state) => state.role);
  
  if (role === 'ROLE_CUSTOMER') return <Navigate to="/dashboard/customer" replace />;
  if (role === 'ROLE_AGENT' || role === 'ROLE_ADMIN') return <Navigate to="/dashboard/agent" replace />;
  
  return <Navigate to="/login" replace />;
};

function App() {
  return (
    <Router>
      <Routes>
        {/* Public Routes */}
        <Route path="/login" element={<LoginPage />} />
        <Route path="/register" element={<RegisterPage />} />
        <Route path="/unauthorized" element={<UnauthorizedPage />} />

        {/* Protected Routes inside Sidebar Layout */}
        <Route element={<SidebarLayout />}>
          
          {/* Main Entry - Redirects based on role */}
          <Route path="/" element={<ProtectedRoute />}>
            <Route index element={<DashboardRouter />} />
          </Route>

          {/* Customer Only Routes */}
          <Route path="/dashboard/customer" element={<ProtectedRoute allowedRoles={['ROLE_CUSTOMER']} />}>
            <Route index element={<CustomerDashboard />} />
          </Route>

          <Route path="/dashboard/settings" element={<ProtectedRoute allowedRoles={['ROLE_CUSTOMER']} />}>
            <Route index element={<CustomerSettings />} />
          </Route>

          <Route path="/dashboard/policies/new" element={<ProtectedRoute allowedRoles={['ROLE_CUSTOMER']} />}>
            <Route index element={<BuyPolicyPage />} />
          </Route>

          <Route path="/policies" element={<ProtectedRoute allowedRoles={['ROLE_CUSTOMER']} />}>
            <Route index element={<PolicyListPage />} />
          </Route>

          <Route path="/claims" element={<ProtectedRoute allowedRoles={['ROLE_CUSTOMER']} />}>
            <Route index element={<ClaimListPage />} />
          </Route>

          <Route path="/dashboard/claims/new" element={<ProtectedRoute allowedRoles={['ROLE_CUSTOMER']} />}>
            <Route index element={<SubmitClaimPage />} />
          </Route>

          {/* Agent Only Routes */}
          <Route path="/dashboard/agent" element={<ProtectedRoute allowedRoles={['ROLE_AGENT', 'ROLE_ADMIN']} />}>
            <Route index element={<AgentDashboard />} />
          </Route>

          <Route path="/customers" element={<ProtectedRoute allowedRoles={['ROLE_AGENT', 'ROLE_ADMIN']} />}>
            <Route index element={<CustomerListPage />} />
          </Route>

          <Route path="/customers/:id" element={<ProtectedRoute allowedRoles={['ROLE_AGENT', 'ROLE_ADMIN']} />}>
            <Route index element={<CustomerDetailPage />} />
          </Route>
          
          <Route path="/policies/all" element={<ProtectedRoute allowedRoles={['ROLE_AGENT', 'ROLE_ADMIN']} />}>
            <Route index element={<PolicyListPage />} />
          </Route>

          <Route path="/claims/queue" element={<ProtectedRoute allowedRoles={['ROLE_AGENT', 'ROLE_ADMIN']} />}>
            <Route index element={<ClaimListPage />} />
          </Route>

          <Route path="/risk" element={<ProtectedRoute allowedRoles={['ROLE_AGENT', 'ROLE_ADMIN']} />}>
            <Route index element={<div className="p-8">Risk Assessment Component (Coming Soon)</div>} />
          </Route>

        </Route>
      </Routes>
    </Router>
  );
}

export default App;
