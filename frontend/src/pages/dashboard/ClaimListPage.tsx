import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { Loader2, AlertCircle, CheckCircle, Ban, MessageSquare } from 'lucide-react';

import { Card, CardContent, CardHeader, CardTitle, CardDescription } from '../../components/ui/Card';
import { Table, TableHeader, TableRow, TableHead, TableCell } from '../../components/ui/Table';
import { Badge } from '../../components/ui/Badge';
import { Button } from '../../components/ui/Button';
import { claimService } from '../../api/services/claimService';
import { useAuthStore } from '../../store/authStore';

export const ClaimListPage = () => {
  const [page, setPage] = useState(0);
  const [reviewingClaimId, setReviewingClaimId] = useState<number | null>(null);
  const [reviewNotes, setReviewNotes] = useState('');
  
  const queryClient = useQueryClient();
  const { role } = useAuthStore();
  const isAgent = role === 'ROLE_AGENT' || role === 'ROLE_ADMIN';

  const { data: claimsData, isLoading } = useQuery({
    queryKey: ['allClaims', page],
    queryFn: () => claimService.getAllClaims(page, 20),
  });

  const approveMutation = useMutation({
    mutationFn: ({ id, notes }: { id: number, notes: string }) => claimService.approveClaim(id, notes),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['allClaims'] });
      setReviewingClaimId(null);
      setReviewNotes('');
    },
  });

  const rejectMutation = useMutation({
    mutationFn: ({ id, notes }: { id: number, notes: string }) => claimService.rejectClaim(id, notes),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['allClaims'] });
      setReviewingClaimId(null);
      setReviewNotes('');
    },
  });

  const handleApprove = () => {
    if (reviewingClaimId && reviewNotes.length > 5) {
      approveMutation.mutate({ id: reviewingClaimId, notes: reviewNotes });
    } else {
      alert("Please provide at least 5 characters of review notes.");
    }
  };

  const handleReject = () => {
    if (reviewingClaimId && reviewNotes.length > 5) {
      rejectMutation.mutate({ id: reviewingClaimId, notes: reviewNotes });
    } else {
      alert("Please provide at least 5 characters of review notes for rejection.");
    }
  };

  return (
    <div className="max-w-6xl mx-auto space-y-8 animate-in fade-in duration-500">
      
      <div>
        <h1 className="text-3xl font-bold text-foreground">
          {isAgent ? 'Claims Queue' : 'My Claims'}
        </h1>
        <p className="text-foreground/60 mt-1">
          {isAgent ? 'Review and process incoming customer insurance claims.' : 'Track the status of your filed claims.'}
        </p>
      </div>

      {reviewingClaimId && (
        <Card className="border-primary-500 shadow-md">
          <CardHeader className="bg-primary-50 dark:bg-primary-900/10">
            <CardTitle className="text-primary-700 flex items-center">
              <MessageSquare className="w-5 h-5 mr-2" />
              Claim Review Action
            </CardTitle>
            <CardDescription>You are reviewing claim ID: {reviewingClaimId}</CardDescription>
          </CardHeader>
          <CardContent className="pt-6 space-y-4">
            <div className="space-y-2">
              <label className="text-sm font-medium text-foreground">Review Notes (Required)</label>
              <textarea
                className="w-full p-3 bg-background border border-border rounded-lg focus:ring-2 focus:ring-primary-500 outline-none"
                placeholder="Detail the reason for approval or rejection..."
                rows={3}
                value={reviewNotes}
                onChange={(e) => setReviewNotes(e.target.value)}
              />
            </div>
            <div className="flex gap-4 justify-end">
              <Button variant="outline" onClick={() => setReviewingClaimId(null)}>Cancel</Button>
              <Button 
                variant="outline" 
                className="text-red-600 border-red-200 hover:bg-red-50"
                onClick={handleReject}
                disabled={rejectMutation.isPending || approveMutation.isPending}
              >
                {rejectMutation.isPending ? <Loader2 className="w-4 h-4 mr-2 animate-spin" /> : <Ban className="w-4 h-4 mr-2" />}
                Reject Claim
              </Button>
              <Button 
                className="bg-green-600 hover:bg-green-700 text-white"
                onClick={handleApprove}
                disabled={rejectMutation.isPending || approveMutation.isPending}
              >
                {approveMutation.isPending ? <Loader2 className="w-4 h-4 mr-2 animate-spin" /> : <CheckCircle className="w-4 h-4 mr-2" />}
                Approve Claim
              </Button>
            </div>
          </CardContent>
        </Card>
      )}

      <Card>
        <CardHeader>
          <CardTitle className="flex items-center">
            <AlertCircle className="w-5 h-5 mr-2 text-primary-500" />
            Claims Directory
          </CardTitle>
          <CardDescription>
            {claimsData?.totalElements || 0} total claims found.
          </CardDescription>
        </CardHeader>
        <Table>
          <TableHeader>
            <TableRow>
              <TableHead>Claim Number</TableHead>
              {isAgent && <TableHead>Customer ID</TableHead>}
              <TableHead>Policy ID</TableHead>
              <TableHead>Amount</TableHead>
              <TableHead>Incident Date</TableHead>
              <TableHead>Status</TableHead>
              {isAgent && <TableHead>Actions</TableHead>}
            </TableRow>
          </TableHeader>
          <tbody>
            {isLoading ? (
              <TableRow>
                <TableCell colSpan={isAgent ? 7 : 6} className="text-center py-12">
                  <Loader2 className="w-8 h-8 animate-spin mx-auto text-primary-500" />
                </TableCell>
              </TableRow>
            ) : claimsData?.content && claimsData.content.length > 0 ? (
              claimsData.content.map((claim) => (
                <TableRow key={claim.id}>
                  <TableCell className="font-medium text-primary-600 dark:text-primary-400">
                    {claim.claimNumber}
                  </TableCell>
                  {isAgent && (
                    <TableCell>#{claim.customerId}</TableCell>
                  )}
                  <TableCell>#{claim.policyId}</TableCell>
                  <TableCell>${claim.claimAmount.toLocaleString()}</TableCell>
                  <TableCell>{new Date(claim.incidentDate).toLocaleDateString()}</TableCell>
                  <TableCell>
                    <Badge variant={
                      claim.status === 'APPROVED' ? 'success' : 
                      claim.status === 'PENDING' || claim.status === 'UNDER_REVIEW' ? 'warning' : 
                      claim.status === 'REJECTED' ? 'destructive' : 'default'
                    }>
                      {claim.status}
                    </Badge>
                  </TableCell>
                  {isAgent && (
                    <TableCell>
                      {(claim.status === 'PENDING' || claim.status === 'UNDER_REVIEW') && (
                        <Button 
                          variant="outline" 
                          size="sm" 
                          onClick={() => setReviewingClaimId(claim.id)}
                          disabled={reviewingClaimId === claim.id}
                        >
                          Review
                        </Button>
                      )}
                    </TableCell>
                  )}
                </TableRow>
              ))
            ) : (
              <TableRow>
                <TableCell colSpan={isAgent ? 7 : 6} className="text-center py-12 text-foreground/50">
                  <AlertCircle className="w-8 h-8 mx-auto mb-3 text-foreground/30" />
                  No claims found.
                </TableCell>
              </TableRow>
            )}
          </tbody>
        </Table>
        
        {/* Simple Pagination */}
        {claimsData && claimsData.totalPages > 1 && (
          <div className="p-4 border-t border-border flex items-center justify-between">
            <Button 
              variant="outline" 
              onClick={() => setPage(p => Math.max(0, p - 1))}
              disabled={page === 0}
            >
              Previous
            </Button>
            <span className="text-sm text-foreground/60">
              Page {page + 1} of {claimsData.totalPages}
            </span>
            <Button 
              variant="outline" 
              onClick={() => setPage(p => Math.min(claimsData.totalPages - 1, p + 1))}
              disabled={page === claimsData.totalPages - 1}
            >
              Next
            </Button>
          </div>
        )}
      </Card>
    </div>
  );
};
