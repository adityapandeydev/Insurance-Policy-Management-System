import { useState } from 'react';
import { Outlet, NavLink, useNavigate, useLocation } from 'react-router-dom';
import { motion, AnimatePresence } from 'framer-motion';
import { useAuthStore } from '../../store/authStore';
import { ThemeToggle } from '../common/ThemeToggle';
import { 
  Shield, 
  LayoutDashboard, 
  FileText, 
  AlertCircle, 
  Users, 
  Activity,
  LogOut,
  Menu,
  X,
  User as UserIcon
} from 'lucide-react';

export const SidebarLayout = () => {
  const { role, username, logout } = useAuthStore();
  const navigate = useNavigate();
  const location = useLocation();
  const [isMobileMenuOpen, setIsMobileMenuOpen] = useState(false);

  const handleLogout = () => {
    logout();
    navigate('/login');
  };

  const customerLinks = [
    { name: 'Dashboard', path: '/dashboard/customer', icon: <LayoutDashboard size={20} /> },
    { name: 'My Policies', path: '/policies', icon: <FileText size={20} /> },
    { name: 'My Claims', path: '/claims', icon: <AlertCircle size={20} /> },
  ];

  const agentLinks = [
    { name: 'Dashboard', path: '/dashboard/agent', icon: <LayoutDashboard size={20} /> },
    { name: 'Customers', path: '/customers', icon: <Users size={20} /> },
    { name: 'Policies', path: '/policies/all', icon: <FileText size={20} /> },
    { name: 'Claims Queue', path: '/claims/queue', icon: <AlertCircle size={20} /> },
    { name: 'Risk Assessment', path: '/risk', icon: <Activity size={20} /> },
  ];

  const links = role === 'ROLE_CUSTOMER' ? customerLinks : agentLinks;

  return (
    <div className="flex h-screen bg-background overflow-hidden selection:bg-primary-500 selection:text-white">
      
      {/* Mobile Menu Overlay */}
      <AnimatePresence>
        {isMobileMenuOpen && (
          <motion.div
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            exit={{ opacity: 0 }}
            onClick={() => setIsMobileMenuOpen(false)}
            className="fixed inset-0 bg-background/80 backdrop-blur-sm z-40 lg:hidden"
          />
        )}
      </AnimatePresence>

      {/* Sidebar */}
      <motion.aside
        initial={false}
        animate={{ 
          x: isMobileMenuOpen ? 0 : -320,
        }}
        className="fixed lg:static inset-y-0 left-0 z-50 w-72 bg-card border-r border-border shadow-xl lg:shadow-none lg:translate-x-0 flex flex-col transition-transform duration-300 ease-in-out lg:!transform-none"
      >
        <div className="p-6 flex items-center justify-between">
          <div className="flex items-center gap-3 text-primary-600 dark:text-primary-400">
            <Shield className="h-8 w-8" />
            <span className="text-xl font-bold text-foreground">SecureInsure</span>
          </div>
          <button onClick={() => setIsMobileMenuOpen(false)} className="lg:hidden text-foreground/50 hover:text-foreground">
            <X size={24} />
          </button>
        </div>

        {/* User Profile Card (Clickable) */}
        <div className="px-6 mb-4">
          <NavLink
            to="/dashboard/settings"
            onClick={() => setIsMobileMenuOpen(false)}
            className={({ isActive }) =>
              `flex items-center gap-3 p-3 rounded-xl transition-all duration-200 border cursor-pointer hover:bg-foreground/[0.03] ${
                isActive
                  ? 'bg-primary-50 dark:bg-primary-900/20 border-primary-100 dark:border-primary-800/30 shadow-sm'
                  : 'bg-background border-border shadow-sm'
              }`
            }
          >
            <div className="w-10 h-10 rounded-full bg-primary-100 dark:bg-primary-900/30 flex items-center justify-center flex-shrink-0 text-primary-700 dark:text-primary-400 font-bold border border-primary-200 dark:border-primary-800/50">
              {username?.charAt(0).toUpperCase()}
            </div>
            <div className="overflow-hidden">
              <p className="text-sm font-semibold text-foreground truncate">{username}</p>
              <p className="text-xs text-foreground/60 truncate capitalize">{role?.replace('ROLE_', '').toLowerCase()}</p>
            </div>
          </NavLink>
        </div>

        <nav className="flex-1 px-4 space-y-1 overflow-y-auto">
          {links.map((link) => (
            <NavLink
              key={link.path}
              to={link.path}
              onClick={() => setIsMobileMenuOpen(false)}
              className={({ isActive }) => `
                flex items-center gap-3 px-4 py-3 rounded-lg text-sm font-medium transition-all duration-200
                ${isActive 
                  ? 'bg-primary-50 dark:bg-primary-500/10 text-primary-600 dark:text-primary-400' 
                  : 'text-foreground/70 hover:bg-foreground/5 hover:text-foreground'}
              `}
            >
              {link.icon}
              {link.name}
            </NavLink>
          ))}
        </nav>

        <div className="p-4 border-t border-border flex items-center gap-2">
          <ThemeToggle />
          <button 
            onClick={handleLogout}
            className="flex-1 flex items-center justify-center gap-2 px-4 py-2.5 rounded-lg text-red-600 dark:text-red-400 hover:bg-red-50 dark:hover:bg-red-500/10 transition-colors font-medium text-sm"
          >
            <LogOut size={18} />
            Sign Out
          </button>
        </div>
      </motion.aside>

      {/* Main Content */}
      <div className="flex-1 flex flex-col min-w-0">
        
        {/* Mobile Header */}
        <header className="lg:hidden bg-card border-b border-border p-4 flex items-center justify-between">
          <div className="flex items-center gap-2 text-primary-600 dark:text-primary-400">
            <Shield className="h-6 w-6" />
            <span className="font-bold text-foreground">SecureInsure</span>
          </div>
          <button onClick={() => setIsMobileMenuOpen(true)} className="text-foreground/70 hover:text-foreground">
            <Menu size={24} />
          </button>
        </header>

        {/* Page Content with Framer Motion AnimatePresence */}
        <main className="flex-1 overflow-y-auto p-4 lg:p-8 bg-background relative">
          <AnimatePresence mode="wait">
            <motion.div
              key={location.pathname}
              initial={{ opacity: 0, y: 10 }}
              animate={{ opacity: 1, y: 0 }}
              exit={{ opacity: 0, y: -10 }}
              transition={{ duration: 0.2 }}
              className="h-full"
            >
              <Outlet />
            </motion.div>
          </AnimatePresence>
        </main>
      </div>
    </div>
  );
};
