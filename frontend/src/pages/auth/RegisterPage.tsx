import { useState } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { useAuthStore } from '../../store/authStore';
import { api } from '../../api/axiosInstance';
import { UserPlus, Loader2, Eye, EyeOff } from 'lucide-react';

export const RegisterPage = () => {
  const [firstName, setFirstName] = useState('');
  const [lastName, setLastName] = useState('');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [showPassword, setShowPassword] = useState(false);
  const [phoneNumber, setPhoneNumber] = useState('');
  const [dateOfBirth, setDateOfBirth] = useState('');
  const [address, setAddress] = useState('');
  const [role, setRole] = useState('CUSTOMER');
  
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);
  
  const setToken = useAuthStore((state) => state.setToken);
  const navigate = useNavigate();

  const handleRegister = async (e: React.FormEvent) => {
    e.preventDefault();
    setError('');
    setLoading(true);

    try {
      const response = await api.post('/auth/register', {
        firstName,
        lastName,
        email,
        password,
        phoneNumber,
        dateOfBirth,
        address,
        role
      });
      // The backend returns an ApiResponse, so the actual AuthResponse is in response.data.data
      setToken(response.data.data.accessToken);
      navigate('/');
    } catch (err: any) {
      setError(err.response?.data?.message || 'Registration failed. Please check your inputs.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="min-h-screen flex items-center justify-center bg-background p-4 py-12 relative overflow-hidden">
      {/* Background decorations */}
      <div className="absolute top-[-10%] right-[-10%] w-[40%] h-[40%] bg-primary-500/20 rounded-full blur-[100px] pointer-events-none" />
      <div className="absolute bottom-[-10%] left-[-10%] w-[40%] h-[40%] bg-blue-500/20 rounded-full blur-[100px] pointer-events-none" />

      <div className="max-w-2xl w-full relative z-10">
        <div className="bg-card border border-border rounded-2xl shadow-xl overflow-hidden">
          <div className="p-8">
            <div className="flex justify-center mb-6">
              <div className="h-14 w-14 bg-primary-100 dark:bg-primary-900/50 rounded-xl flex items-center justify-center">
                <UserPlus className="h-8 w-8 text-primary-600 dark:text-primary-400" />
              </div>
            </div>
            
            <h2 className="text-2xl font-bold text-center text-foreground mb-2">Create an Account</h2>
            <p className="text-center text-foreground/60 mb-8">Join to manage your insurance policies</p>

            {error && (
              <div className="mb-6 p-4 bg-red-50 dark:bg-red-900/20 border border-red-200 dark:border-red-800 text-red-600 dark:text-red-400 rounded-lg text-sm text-center">
                {error}
              </div>
            )}

            <form onSubmit={handleRegister} className="space-y-5">
              <div className="grid grid-cols-1 md:grid-cols-2 gap-5">
                <div>
                  <label className="block text-sm font-medium text-foreground mb-1.5">First Name</label>
                  <input
                    type="text"
                    value={firstName}
                    onChange={(e) => setFirstName(e.target.value)}
                    className="w-full px-4 py-2.5 rounded-lg bg-input border border-border text-foreground focus:ring-2 focus:ring-primary-500 focus:border-primary-500 outline-none"
                    required
                  />
                </div>
                <div>
                  <label className="block text-sm font-medium text-foreground mb-1.5">Last Name</label>
                  <input
                    type="text"
                    value={lastName}
                    onChange={(e) => setLastName(e.target.value)}
                    className="w-full px-4 py-2.5 rounded-lg bg-input border border-border text-foreground focus:ring-2 focus:ring-primary-500 focus:border-primary-500 outline-none"
                    required
                  />
                </div>
              </div>

              <div>
                <label className="block text-sm font-medium text-foreground mb-1.5">Email Address</label>
                <input
                  type="email"
                  value={email}
                  onChange={(e) => setEmail(e.target.value)}
                  className="w-full px-4 py-2.5 rounded-lg bg-input border border-border text-foreground focus:ring-2 focus:ring-primary-500 focus:border-primary-500 outline-none"
                  required
                />
              </div>

              <div>
                <label className="block text-sm font-medium text-foreground mb-1.5">Password</label>
                <div className="relative">
                  <input
                    type={showPassword ? 'text' : 'password'}
                    value={password}
                    onChange={(e) => setPassword(e.target.value)}
                    className="w-full px-4 py-2.5 rounded-lg bg-input border border-border text-foreground focus:ring-2 focus:ring-primary-500 focus:border-primary-500 outline-none pr-10"
                    required
                  />
                  <button
                    type="button"
                    onClick={() => setShowPassword(!showPassword)}
                    className="absolute inset-y-0 right-0 pr-3 flex items-center text-foreground/50 hover:text-foreground/80 focus:outline-none"
                    tabIndex={-1}
                  >
                    {showPassword ? <EyeOff className="h-5 w-5" /> : <Eye className="h-5 w-5" />}
                  </button>
                </div>
              </div>

              <div className="grid grid-cols-1 md:grid-cols-2 gap-5">
                <div>
                  <label className="block text-sm font-medium text-foreground mb-1.5">Phone Number</label>
                  <input
                    type="tel"
                    value={phoneNumber}
                    onChange={(e) => setPhoneNumber(e.target.value)}
                    className="w-full px-4 py-2.5 rounded-lg bg-input border border-border text-foreground focus:ring-2 focus:ring-primary-500 focus:border-primary-500 outline-none"
                    required
                  />
                </div>
                <div>
                  <label className="block text-sm font-medium text-foreground mb-1.5">Date of Birth</label>
                  <input
                    type="date"
                    value={dateOfBirth}
                    onChange={(e) => setDateOfBirth(e.target.value)}
                    className="w-full px-4 py-2.5 rounded-lg bg-input border border-border text-foreground focus:ring-2 focus:ring-primary-500 focus:border-primary-500 outline-none dark:[color-scheme:dark]"
                    required
                  />
                </div>
              </div>

              <div>
                <label className="block text-sm font-medium text-foreground mb-1.5">Full Address</label>
                <textarea
                  value={address}
                  onChange={(e) => setAddress(e.target.value)}
                  rows={2}
                  className="w-full px-4 py-2.5 rounded-lg bg-input border border-border text-foreground focus:ring-2 focus:ring-primary-500 focus:border-primary-500 outline-none resize-none"
                  required
                />
              </div>

              <div>
                <label className="block text-sm font-medium text-foreground mb-1.5">Account Type</label>
                <div className="flex gap-4">
                  <label className="flex items-center space-x-2 cursor-pointer">
                    <input
                      type="radio"
                      name="role"
                      value="CUSTOMER"
                      checked={role === 'CUSTOMER'}
                      onChange={() => setRole('CUSTOMER')}
                      className="text-primary-600 focus:ring-primary-500"
                    />
                    <span className="text-foreground">Customer</span>
                  </label>
                  <label className="flex items-center space-x-2 cursor-pointer">
                    <input
                      type="radio"
                      name="role"
                      value="AGENT"
                      checked={role === 'AGENT'}
                      onChange={() => setRole('AGENT')}
                      className="text-primary-600 focus:ring-primary-500"
                    />
                    <span className="text-foreground">Agent</span>
                  </label>
                </div>
              </div>

              <button
                type="submit"
                disabled={loading}
                className="w-full flex items-center justify-center py-2.5 px-4 rounded-lg bg-primary-600 hover:bg-primary-700 text-white font-medium transition-colors disabled:opacity-70 disabled:cursor-not-allowed mt-4"
              >
                {loading ? <Loader2 className="h-5 w-5 animate-spin" /> : 'Create Account'}
              </button>
            </form>
          </div>
          
          <div className="p-6 bg-foreground/5 border-t border-border text-center">
            <p className="text-sm text-foreground/70">
              Already have an account?{' '}
              <Link to="/login" className="text-primary-600 dark:text-primary-400 hover:underline font-medium">
                Sign in here
              </Link>
            </p>
          </div>
        </div>
      </div>
    </div>
  );
};
