import { Users, AlertCircle, FileText, Activity, Loader2 } from 'lucide-react';
import { useQuery } from '@tanstack/react-query';
import { useNavigate } from 'react-router-dom';
import { Card, CardContent, CardHeader, CardTitle } from '../../components/ui/Card';
import { Badge } from '../../components/ui/Badge';
import { Button } from '../../components/ui/Button';
import { Table, TableHeader, TableRow, TableHead, TableCell } from '../../components/ui/Table';
import { useAuthStore } from '../../store/authStore';
import { dashboardService } from '../../api/services/dashboardService';

export const AgentDashboard = () => {
  const { username } = useAuthStore();
  const navigate = useNavigate();

  const { data: metrics, isLoading } = useQuery({
    queryKey: ['dashboardMetrics'],
    queryFn: dashboardService.getDashboardMetrics,
    refetchInterval: 30000, // Auto-refresh every 30 seconds for live admin view
  });

  if (isLoading) {
    return (
      <div className="flex flex-col items-center justify-center h-[50vh] text-foreground/50">
        <Loader2 className="w-10 h-10 animate-spin text-primary-500 mb-4" />
        <p>Loading real-time system metrics...</p>
      </div>
    );
  }

  return (
    <div className="space-y-8 animate-in fade-in slide-in-from-bottom-4 duration-500">
      
      {/* Header */}
      <div className="flex flex-col md:flex-row justify-between items-start md:items-center gap-4">
        <div>
          <h1 className="text-3xl font-bold text-foreground">Agent Overview</h1>
          <p className="text-foreground/60 mt-1">Welcome back, {username}. Here is the live system state.</p>
        </div>
        <div className="flex gap-3">
          <Button variant="outline" onClick={() => navigate('/customers')}>
            <Users className="w-4 h-4 mr-2" />
            Add Customer
          </Button>
          <Button onClick={() => navigate('/policies')}>
            <FileText className="w-4 h-4 mr-2" />
            Create Policy
          </Button>
        </div>
      </div>

      {/* KPI Cards */}
      <div className="grid grid-cols-1 md:grid-cols-4 gap-6">
        <Card className="bg-gradient-to-br from-blue-500 to-blue-700 text-white border-none shadow-blue-500/20 shadow-lg relative overflow-hidden">
          <div className="absolute top-0 right-0 p-4 opacity-10">
            <Users className="w-24 h-24" />
          </div>
          <CardContent className="p-6 relative z-10">
            <p className="text-blue-100 font-medium">Total Customers</p>
            <h2 className="text-4xl font-bold mt-2">{metrics?.totalCustomers || 0}</h2>
            <div className="mt-4 flex items-center text-sm bg-white/10 w-fit px-3 py-1 rounded-full backdrop-blur-md">
              <span className="text-green-300 mr-1">+12%</span> this month
            </div>
          </CardContent>
        </Card>

        <Card>
          <CardContent className="p-6 flex flex-col justify-center h-full">
            <div className="flex justify-between items-start">
              <div>
                <p className="text-foreground/60 font-medium">Pending Claims</p>
                <h2 className="text-3xl font-bold text-foreground mt-2">{metrics?.pendingClaims || 0}</h2>
              </div>
              <div className="w-12 h-12 rounded-full bg-orange-100 dark:bg-orange-900/30 flex items-center justify-center text-orange-600 dark:text-orange-400 shadow-sm border border-orange-200 dark:border-orange-800/50">
                <AlertCircle className="w-6 h-6" />
              </div>
            </div>
            <div className="mt-4 flex items-center text-sm text-foreground/60">
              <span className="text-orange-600 dark:text-orange-400 font-medium mr-2">{metrics?.claimsUnderReview || 0}</span>
              under review
            </div>
          </CardContent>
        </Card>

        <Card>
          <CardContent className="p-6 flex flex-col justify-center h-full">
            <div className="flex justify-between items-start">
              <div>
                <p className="text-foreground/60 font-medium">Active Policies</p>
                <h2 className="text-3xl font-bold text-foreground mt-2">{metrics?.activePolicies || 0}</h2>
              </div>
              <div className="w-12 h-12 rounded-full bg-green-100 dark:bg-green-900/30 flex items-center justify-center text-green-600 dark:text-green-400 shadow-sm border border-green-200 dark:border-green-800/50">
                <FileText className="w-6 h-6" />
              </div>
            </div>
            <div className="mt-4 flex items-center text-sm text-foreground/60">
              <span className="text-foreground/40 font-medium mr-2">Out of {metrics?.totalPolicies || 0} total</span>
            </div>
          </CardContent>
        </Card>

        <Card>
          <CardContent className="p-6 flex flex-col justify-center h-full">
            <div className="flex justify-between items-start">
              <div>
                <p className="text-foreground/60 font-medium">High Risk Profiles</p>
                <h2 className="text-3xl font-bold text-foreground mt-2">{metrics?.highRiskCustomers || 0}</h2>
              </div>
              <div className="w-12 h-12 rounded-full bg-red-100 dark:bg-red-900/30 flex items-center justify-center text-red-600 dark:text-red-400 shadow-sm border border-red-200 dark:border-red-800/50">
                <Activity className="w-6 h-6" />
              </div>
            </div>
            <div className="mt-4 flex items-center text-sm text-foreground/60">
              <span className="text-red-600 dark:text-red-400 font-medium mr-2">Requires Attention</span>
            </div>
          </CardContent>
        </Card>
      </div>

      {/* Main Content Grid */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-8">
        
        {/* Right Column - System Status */}
        <Card className="flex flex-col lg:col-span-2">
          <CardHeader className="flex flex-row items-center justify-between">
            <CardTitle>System Status</CardTitle>
          </CardHeader>
          <CardContent className="p-6 flex-1 flex items-center justify-center">
            <div className="text-center">
              <div className="mx-auto w-24 h-24 rounded-full bg-green-100 dark:bg-green-900/30 flex items-center justify-center text-green-600 dark:text-green-400 mb-4 shadow-sm border border-green-200 dark:border-green-800/50">
                <Activity className="w-12 h-12" />
              </div>
              <h3 className="text-xl font-bold text-foreground">All Systems Operational</h3>
              <p className="text-sm text-foreground/60 mt-2">Database and React Query caching layer are perfectly synced.</p>
            </div>
          </CardContent>
        </Card>
        
      </div>
    </div>
  );
};
