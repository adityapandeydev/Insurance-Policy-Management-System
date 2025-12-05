import { FileText, AlertCircle, TrendingUp, Calendar, CreditCard, Loader2 } from 'lucide-react';
import { useNavigate } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import { Card, CardContent, CardHeader, CardTitle } from '../../components/ui/Card';
import { Badge } from '../../components/ui/Badge';
import { Button } from '../../components/ui/Button';
import { Table, TableHeader, TableRow, TableHead, TableCell } from '../../components/ui/Table';
import { useAuthStore } from '../../store/authStore';
import { customerService } from '../../api/services/customerService';
import { policyService } from '../../api/services/policyService';
import { claimService } from '../../api/services/claimService';

export const CustomerDashboard = () => {
  const { username } = useAuthStore();
  const navigate = useNavigate();

  // Fetch Profile
  const { data: profile, isLoading: isProfileLoading } = useQuery({
    queryKey: ['myProfile'],
    queryFn: customerService.getMyProfile,
  });

  // Fetch Policies
  const { data: policies, isLoading: isPoliciesLoading } = useQuery({
    queryKey: ['myPolicies', profile?.id],
    queryFn: () => policyService.getPoliciesByCustomer(profile!.id),
    enabled: !!profile?.id,
  });

  // Fetch Claims
  const { data: claims, isLoading: isClaimsLoading } = useQuery({
    queryKey: ['myClaims', profile?.id],
    queryFn: () => claimService.getAllClaims(0, 5),
    enabled: !!profile?.id,
  });

  if (isProfileLoading) {
    return (
      <div className="flex items-center justify-center h-[50vh]">
        <Loader2 className="w-8 h-8 animate-spin text-primary-500" />
      </div>
    );
  }

  const activePoliciesCount = policies?.content.filter(p => p.status === 'ACTIVE').length || 0;
  const pendingClaimsCount = claims?.content.filter(c => c.status === 'PENDING' || c.status === 'UNDER_REVIEW').length || 0;
  
  const isProfileIncomplete = !profile?.address || !profile?.nationalId || !profile?.emergencyContactName;

  return (
    <div className="space-y-8 animate-in fade-in slide-in-from-bottom-4 duration-500">
      
      {isProfileIncomplete && (
        <div className="bg-orange-50 dark:bg-orange-900/20 border border-orange-200 dark:border-orange-800/50 rounded-xl p-4 flex items-center justify-between">
          <div className="flex items-center gap-3">
            <div className="w-10 h-10 rounded-full bg-orange-100 dark:bg-orange-900/50 flex items-center justify-center text-orange-600 dark:text-orange-400">
              <AlertCircle className="w-5 h-5" />
            </div>
            <div>
              <p className="text-sm font-bold text-orange-800 dark:text-orange-300">Action Required: Complete Your Profile</p>
              <p className="text-xs text-orange-600 dark:text-orange-400/80 mt-0.5">Please provide your address and emergency contact information before purchasing a policy.</p>
            </div>
          </div>
          <Button variant="outline" size="sm" onClick={() => window.location.href='/dashboard/settings'} className="border-orange-200 dark:border-orange-800/50 text-orange-700 dark:text-orange-400 hover:bg-orange-100 dark:hover:bg-orange-900/50">
            Update Profile
          </Button>
        </div>
      )}

      {/* Header */}
      <div className="flex flex-col md:flex-row justify-between items-start md:items-center gap-4">
        <div>
          <h1 className="text-3xl font-bold text-foreground">Welcome back, {profile?.firstName || username}!</h1>
          <p className="text-foreground/60 mt-1">Here is an overview of your insurance portfolio.</p>
        </div>
        <Button onClick={() => navigate('/dashboard/policies/new')}>
          <FileText className="w-4 h-4 mr-2" />
          Buy New Policy
        </Button>
      </div>

      {/* KPI Cards */}
      <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
        <Card className="bg-gradient-to-br from-primary-500 to-primary-700 text-white border-none shadow-primary-500/20 shadow-lg relative overflow-hidden">
          <div className="absolute top-0 right-0 p-4 opacity-10">
            <FileText className="w-24 h-24" />
          </div>
          <CardContent className="p-6 relative z-10">
            <p className="text-primary-100 font-medium">Active Policies</p>
            <h2 className="text-4xl font-bold mt-2">
              {isPoliciesLoading ? <Loader2 className="w-6 h-6 animate-spin mt-2" /> : activePoliciesCount}
            </h2>
            <div className="mt-4 flex items-center text-sm bg-white/10 w-fit px-3 py-1 rounded-full backdrop-blur-md">
              <TrendingUp className="w-4 h-4 mr-1" />
              Fully Covered
            </div>
          </CardContent>
        </Card>

        <Card>
          <CardContent className="p-6 flex flex-col justify-center h-full">
            <div className="flex justify-between items-start">
              <div>
                <p className="text-foreground/60 font-medium">Total Claims</p>
                <h2 className="text-3xl font-bold text-foreground mt-2">{profile?.totalClaims || 0}</h2>
              </div>
              <div className="w-12 h-12 rounded-full bg-blue-100 dark:bg-blue-900/30 flex items-center justify-center text-blue-600 dark:text-blue-400">
                <Calendar className="w-6 h-6" />
              </div>
            </div>
            <div className="mt-4 flex items-center text-sm text-foreground/60">
              <span className="text-blue-600 dark:text-blue-400 font-medium mr-2">Lifetime claims</span>
              filed
            </div>
          </CardContent>
        </Card>

        <Card>
          <CardContent className="p-6 flex flex-col justify-center h-full">
            <div className="flex justify-between items-start">
              <div>
                <p className="text-foreground/60 font-medium">Pending Claims</p>
                <h2 className="text-3xl font-bold text-foreground mt-2">
                  {isClaimsLoading ? <Loader2 className="w-6 h-6 animate-spin mt-2" /> : pendingClaimsCount}
                </h2>
              </div>
              <div className="w-12 h-12 rounded-full bg-orange-100 dark:bg-orange-900/30 flex items-center justify-center text-orange-600 dark:text-orange-400 shadow-sm border border-orange-200 dark:border-orange-800/50">
                <AlertCircle className="w-6 h-6" />
              </div>
            </div>
            <div className="mt-4 flex items-center text-sm text-foreground/60">
              {pendingClaimsCount > 0 ? (
                <>
                  <Badge variant="warning" className="mr-2">{pendingClaimsCount} Active</Badge>
                  Currently processing
                </>
              ) : (
                <>
                  <Badge variant="success" className="mr-2">All Good</Badge>
                  No pending claims
                </>
              )}
            </div>
          </CardContent>
        </Card>
      </div>

      {/* Main Content Grid */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-8">
        
        {/* Left Column (Wider) */}
        <div className="lg:col-span-2 space-y-8">
          <Card>
            <CardHeader className="flex flex-row items-center justify-between">
              <CardTitle>My Policies</CardTitle>
              <Button variant="ghost" size="sm" onClick={() => navigate('/policies')}>View All</Button>
            </CardHeader>
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>Policy Number</TableHead>
                  <TableHead>Type</TableHead>
                  <TableHead>Coverage</TableHead>
                  <TableHead>Status</TableHead>
                </TableRow>
              </TableHeader>
              <tbody>
                {isPoliciesLoading ? (
                  <TableRow>
                    <TableCell colSpan={4} className="text-center py-8">
                      <Loader2 className="w-6 h-6 animate-spin mx-auto text-foreground/40" />
                    </TableCell>
                  </TableRow>
                ) : policies?.content && policies.content.length > 0 ? (
                  policies.content.map((policy) => (
                    <TableRow key={policy.id}>
                      <TableCell className="font-medium">{policy.policyNumber}</TableCell>
                      <TableCell>{policy.policyName || policy.policyType}</TableCell>
                      <TableCell>${policy.coverageAmount.toLocaleString()}</TableCell>
                      <TableCell>
                        <Badge variant={policy.status === 'ACTIVE' ? 'success' : 'default'}>
                          {policy.status}
                        </Badge>
                      </TableCell>
                    </TableRow>
                  ))
                ) : (
                  <TableRow>
                    <TableCell colSpan={4} className="text-center py-8 text-foreground/50">
                      No policies found.
                    </TableCell>
                  </TableRow>
                )}
              </tbody>
            </Table>
          </Card>

          <Card>
            <CardHeader className="flex flex-row items-center justify-between">
              <CardTitle>Recent Claims</CardTitle>
              <div className="space-x-2">
                <Button variant="outline" size="sm" onClick={() => navigate('/dashboard/claims/new')}>
                  File Claim
                </Button>
                <Button variant="ghost" size="sm" onClick={() => navigate('/claims')}>View All</Button>
              </div>
            </CardHeader>
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>Claim Number</TableHead>
                  <TableHead>Incident Date</TableHead>
                  <TableHead>Amount</TableHead>
                  <TableHead>Status</TableHead>
                </TableRow>
              </TableHeader>
              <tbody>
                {isClaimsLoading ? (
                  <TableRow>
                    <TableCell colSpan={4} className="text-center py-8">
                      <Loader2 className="w-6 h-6 animate-spin mx-auto text-foreground/40" />
                    </TableCell>
                  </TableRow>
                ) : claims?.content && claims.content.length > 0 ? (
                  claims.content.map((claim) => (
                    <TableRow key={claim.id}>
                      <TableCell className="font-medium">{claim.claimNumber}</TableCell>
                      <TableCell>{new Date(claim.incidentDate).toLocaleDateString()}</TableCell>
                      <TableCell>${claim.claimAmount.toLocaleString()}</TableCell>
                      <TableCell>
                        <Badge variant={
                          claim.status === 'APPROVED' ? 'success' : 
                          claim.status === 'REJECTED' ? 'destructive' : 
                          claim.status === 'UNDER_REVIEW' ? 'warning' : 'default'
                        }>
                          {claim.status}
                        </Badge>
                      </TableCell>
                    </TableRow>
                  ))
                ) : (
                  <TableRow>
                    <TableCell colSpan={4} className="text-center py-8 text-foreground/50">
                      No claims filed yet.
                    </TableCell>
                  </TableRow>
                )}
              </tbody>
            </Table>
          </Card>
        </div>

        {/* Right Column (Narrower) */}
        <div className="space-y-8">
          <Card>
            <CardHeader>
              <CardTitle>Recent Activity</CardTitle>
            </CardHeader>
            <CardContent className="p-0">
              <div className="divide-y divide-border">
                <div className="p-4 flex gap-4 hover:bg-foreground/[0.02] transition-colors">
                  <div className="mt-1 h-8 w-8 rounded-full bg-blue-100 dark:bg-blue-900/30 flex items-center justify-center flex-shrink-0">
                    <FileText className="h-4 w-4 text-blue-600 dark:text-blue-400" />
                  </div>
                  <div>
                    <p className="text-sm font-medium text-foreground">Policy Created</p>
                    <p className="text-xs text-foreground/60 mt-0.5">Comprehensive Auto Secure policy activated.</p>
                    <p className="text-xs text-foreground/40 mt-2">Recently</p>
                  </div>
                </div>
                {isProfileIncomplete && (
                  <div className="p-4 flex gap-4 hover:bg-foreground/[0.02] transition-colors">
                    <div className="mt-1 h-8 w-8 rounded-full bg-orange-100 dark:bg-orange-900/30 flex items-center justify-center flex-shrink-0">
                      <AlertCircle className="h-4 w-4 text-orange-600 dark:text-orange-400" />
                    </div>
                    <div>
                      <p className="text-sm font-medium text-foreground">Profile Incomplete</p>
                      <p className="text-xs text-foreground/60 mt-0.5">Please update your address and emergency contact details in settings.</p>
                      <p className="text-xs text-foreground/40 mt-2">Pending</p>
                    </div>
                  </div>
                )}
              </div>
            </CardContent>
          </Card>
        </div>
        
      </div>
    </div>
  );
};
