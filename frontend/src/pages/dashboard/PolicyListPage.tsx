import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { Loader2, FileText, AlertCircle, Ban, CheckCircle } from 'lucide-react';

import { Card, CardContent, CardHeader, CardTitle, CardDescription } from '../../components/ui/Card';
import { Table, TableHeader, TableRow, TableHead, TableCell } from '../../components/ui/Table';
import { Badge } from '../../components/ui/Badge';
import { Button } from '../../components/ui/Button';
import { policyService } from '../../api/services/policyService';
import { useAuthStore } from '../../store/authStore';

export const PolicyListPage = () => {
  const [page, setPage] = useState(0);
  const queryClient = useQueryClient();
  const { role } = useAuthStore();
  const isAgent = role === 'ROLE_AGENT' || role === 'ROLE_ADMIN';

  const { data: policiesData, isLoading } = useQuery({
    queryKey: ['allPolicies', page],
    queryFn: () => policyService.getAllPolicies(page, 20),
  });

  const updateStatusMutation = useMutation({
    mutationFn: ({ id, status }: { id: number, status: string }) => policyService.updatePolicyStatus(id, status),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['allPolicies'] });
      queryClient.invalidateQueries({ queryKey: ['myPolicies'] });
    },
  });

  const handleStatusChange = (id: number, status: string) => {
    if (confirm(`Are you sure you want to change this policy status to ${status}?`)) {
      updateStatusMutation.mutate({ id, status });
    }
  };

  return (
    <div className="max-w-6xl mx-auto space-y-8 animate-in fade-in duration-500">
      
      <div>
        <h1 className="text-3xl font-bold text-foreground">
          {isAgent ? 'All Policies' : 'My Policies'}
        </h1>
        <p className="text-foreground/60 mt-1">
          {isAgent ? 'Manage all customer policies across the platform.' : 'View and manage your insurance policies.'}
        </p>
      </div>

      <Card>
        <CardHeader>
          <CardTitle className="flex items-center">
            <FileText className="w-5 h-5 mr-2 text-primary-500" />
            Policy Directory
          </CardTitle>
          <CardDescription>
            {policiesData?.totalElements || 0} total policies found.
          </CardDescription>
        </CardHeader>
        <Table>
          <TableHeader>
            <TableRow>
              <TableHead>Policy Number</TableHead>
              {isAgent && <TableHead>Customer ID</TableHead>}
              <TableHead>Type</TableHead>
              <TableHead>Coverage</TableHead>
              <TableHead>Premium</TableHead>
              <TableHead>Dates</TableHead>
              <TableHead>Status</TableHead>
              <TableHead>Actions</TableHead>
            </TableRow>
          </TableHeader>
          <tbody>
            {isLoading ? (
              <TableRow>
                <TableCell colSpan={isAgent ? 8 : 7} className="text-center py-12">
                  <Loader2 className="w-8 h-8 animate-spin mx-auto text-primary-500" />
                </TableCell>
              </TableRow>
            ) : policiesData?.content && policiesData.content.length > 0 ? (
              policiesData.content.map((policy) => (
                <TableRow key={policy.id}>
                  <TableCell className="font-medium text-primary-600 dark:text-primary-400">
                    {policy.policyNumber}
                  </TableCell>
                  {isAgent && (
                    <TableCell>#{policy.customerId}</TableCell>
                  )}
                  <TableCell>{policy.policyType}</TableCell>
                  <TableCell>${policy.coverageAmount.toLocaleString()}</TableCell>
                  <TableCell>${policy.premiumAmount}/{policy.premiumFrequency.toLowerCase()}</TableCell>
                  <TableCell>
                    <div className="text-xs">
                      <div>Start: {new Date(policy.startDate).toLocaleDateString()}</div>
                      <div>End: {new Date(policy.endDate).toLocaleDateString()}</div>
                    </div>
                  </TableCell>
                  <TableCell>
                    <Badge variant={
                      policy.status === 'ACTIVE' ? 'success' : 
                      policy.status === 'PENDING' ? 'warning' : 
                      policy.status === 'CANCELLED' ? 'destructive' : 'default'
                    }>
                      {policy.status}
                    </Badge>
                  </TableCell>
                  <TableCell>
                    <div className="flex gap-2">
                      {isAgent && policy.status === 'PENDING' && (
                        <Button 
                          variant="outline" 
                          size="sm" 
                          className="text-green-600 border-green-200 hover:bg-green-50"
                          onClick={() => handleStatusChange(policy.id, 'ACTIVE')}
                          disabled={updateStatusMutation.isPending}
                        >
                          <CheckCircle className="w-4 h-4" />
                        </Button>
                      )}
                      {(policy.status === 'ACTIVE' || policy.status === 'PENDING') && (
                        <Button 
                          variant="outline" 
                          size="sm" 
                          className="text-red-600 border-red-200 hover:bg-red-50"
                          onClick={() => handleStatusChange(policy.id, 'CANCELLED')}
                          disabled={updateStatusMutation.isPending}
                        >
                          <Ban className="w-4 h-4" />
                        </Button>
                      )}
                    </div>
                  </TableCell>
                </TableRow>
              ))
            ) : (
              <TableRow>
                <TableCell colSpan={isAgent ? 8 : 7} className="text-center py-12 text-foreground/50">
                  <AlertCircle className="w-8 h-8 mx-auto mb-3 text-foreground/30" />
                  No policies found.
                </TableCell>
              </TableRow>
            )}
          </tbody>
        </Table>
        
        {/* Simple Pagination */}
        {policiesData && policiesData.totalPages > 1 && (
          <div className="p-4 border-t border-border flex items-center justify-between">
            <Button 
              variant="outline" 
              onClick={() => setPage(p => Math.max(0, p - 1))}
              disabled={page === 0}
            >
              Previous
            </Button>
            <span className="text-sm text-foreground/60">
              Page {page + 1} of {policiesData.totalPages}
            </span>
            <Button 
              variant="outline" 
              onClick={() => setPage(p => Math.min(policiesData.totalPages - 1, p + 1))}
              disabled={page === policiesData.totalPages - 1}
            >
              Next
            </Button>
          </div>
        )}
      </Card>
    </div>
  );
};
