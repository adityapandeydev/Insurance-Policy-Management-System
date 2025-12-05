import { useState } from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import * as z from 'zod';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { AlertCircle, Save, Loader2, CheckCircle2, DollarSign, Calendar, FileText, Activity } from 'lucide-react';
import { useNavigate } from 'react-router-dom';

import { Card, CardContent, CardHeader, CardTitle, CardDescription } from '../../components/ui/Card';
import { Button } from '../../components/ui/Button';
import { policyService } from '../../api/services/policyService';
import { claimService } from '../../api/services/claimService';
import { customerService } from '../../api/services/customerService';

const claimSchema = z.object({
  policyId: z.number({ invalid_type_error: "Please select a policy" }),
  claimAmount: z.number({ invalid_type_error: "Claim amount must be a number" }).min(1, 'Minimum claim amount is $1'),
  incidentDate: z.string().min(1, 'Incident date is required'),
  description: z.string().min(10, 'Please provide at least 10 characters describing the incident.'),
});

type ClaimFormValues = z.infer<typeof claimSchema>;

export const SubmitClaimPage = () => {
  const [successMsg, setSuccessMsg] = useState('');
  const queryClient = useQueryClient();
  const navigate = useNavigate();

  // Need profile to get customer ID to fetch policies
  const { data: profile, isLoading: profileLoading } = useQuery({
    queryKey: ['myProfile'],
    queryFn: customerService.getMyProfile,
  });

  const { data: policiesData, isLoading: policiesLoading } = useQuery({
    queryKey: ['myPolicies', profile?.id],
    queryFn: () => policyService.getPoliciesByCustomer(profile!.id),
    enabled: !!profile?.id,
  });

  // Only allow claims on ACTIVE policies
  const activePolicies = policiesData?.content?.filter((p: any) => p.status === 'ACTIVE') || [];

  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<ClaimFormValues>({
    resolver: zodResolver(claimSchema),
    defaultValues: {
      incidentDate: new Date().toISOString().split('T')[0],
    },
  });

  const submitMutation = useMutation({
    mutationFn: async (data: ClaimFormValues) => {
      return claimService.submitClaim(data);
    },
    onSuccess: (newClaim) => {
      setSuccessMsg(`Claim ${newClaim.claimNumber} submitted successfully! It is now PENDING review.`);
      queryClient.invalidateQueries({ queryKey: ['myClaims'] });
      queryClient.invalidateQueries({ queryKey: ['myPolicies'] });
      setTimeout(() => navigate('/dashboard'), 3000);
    },
  });

  const onSubmit = (data: ClaimFormValues) => {
    submitMutation.mutate(data);
  };

  if (profileLoading || policiesLoading) {
    return (
      <div className="flex items-center justify-center h-[50vh]">
        <Loader2 className="w-8 h-8 animate-spin text-primary-500" />
      </div>
    );
  }

  return (
    <div className="max-w-4xl mx-auto space-y-8 animate-in fade-in slide-in-from-bottom-4 duration-500">
      
      {/* Header */}
      <div>
        <h1 className="text-3xl font-bold text-foreground">File a New Claim</h1>
        <p className="text-foreground/60 mt-1">Submit an incident report against one of your active policies.</p>
      </div>

      {successMsg && (
        <div className="p-4 bg-green-50 dark:bg-green-900/20 border border-green-200 dark:border-green-800/50 rounded-lg flex items-center text-green-700 dark:text-green-400 animate-in slide-in-from-top-2">
          <CheckCircle2 className="w-5 h-5 mr-2" />
          {successMsg}
        </div>
      )}

      {activePolicies.length === 0 ? (
        <Card className="bg-orange-50 dark:bg-orange-900/20 border-orange-200 dark:border-orange-800/50">
          <CardContent className="flex items-center p-6 text-orange-700 dark:text-orange-400">
            <AlertCircle className="w-6 h-6 mr-4" />
            <div>
              <p className="font-semibold">No Active Policies Found</p>
              <p className="text-sm">You must have an active policy before you can file a claim. If your policy is pending activation, please wait for an agent to process it.</p>
            </div>
          </CardContent>
        </Card>
      ) : (
        <form onSubmit={handleSubmit(onSubmit)} className="space-y-8">
          <Card>
            <CardHeader>
              <CardTitle className="flex items-center">
                <FileText className="w-5 h-5 mr-2 text-primary-500" />
                Incident Details
              </CardTitle>
              <CardDescription>Provide accurate details so our agents can review your claim quickly.</CardDescription>
            </CardHeader>
            <CardContent className="grid grid-cols-1 md:grid-cols-2 gap-6">
              
              <div className="space-y-2 md:col-span-2">
                <label className="text-sm font-medium text-foreground">Select Active Policy</label>
                <div className="relative">
                  <Activity className="absolute left-3 top-3 w-5 h-5 text-foreground/40" />
                  <select
                    className="w-full pl-10 p-2.5 bg-background border border-border rounded-lg text-foreground focus:ring-2 focus:ring-primary-500 outline-none transition-all appearance-none"
                    {...register('policyId', { valueAsNumber: true })}
                  >
                    <option value="">-- Choose a Policy --</option>
                    {activePolicies.map((p: any) => (
                      <option key={p.id} value={p.id}>
                        {p.policyName} ({p.policyNumber}) - Coverage: ${p.coverageAmount}
                      </option>
                    ))}
                  </select>
                </div>
                {errors.policyId && <p className="text-sm text-red-500">{errors.policyId.message}</p>}
              </div>

              <div className="space-y-2">
                <label className="text-sm font-medium text-foreground">Claim Amount ($)</label>
                <div className="relative">
                  <DollarSign className="absolute left-3 top-3 w-5 h-5 text-foreground/40" />
                  <input
                    type="number"
                    className="w-full pl-10 p-2.5 bg-background border border-border rounded-lg text-foreground focus:ring-2 focus:ring-primary-500 outline-none transition-all"
                    placeholder="2500"
                    {...register('claimAmount', { valueAsNumber: true })}
                  />
                </div>
                {errors.claimAmount && <p className="text-sm text-red-500">{errors.claimAmount.message}</p>}
              </div>

              <div className="space-y-2">
                <label className="text-sm font-medium text-foreground">Date of Incident</label>
                <div className="relative">
                  <Calendar className="absolute left-3 top-3 w-5 h-5 text-foreground/40" />
                  <input
                    type="date"
                    className="w-full pl-10 p-2.5 bg-background border border-border rounded-lg text-foreground focus:ring-2 focus:ring-primary-500 outline-none transition-all"
                    {...register('incidentDate')}
                  />
                </div>
                {errors.incidentDate && <p className="text-sm text-red-500">{errors.incidentDate.message}</p>}
              </div>

              <div className="space-y-2 md:col-span-2">
                <label className="text-sm font-medium text-foreground">Incident Description</label>
                <textarea
                  className="w-full p-2.5 bg-background border border-border rounded-lg text-foreground focus:ring-2 focus:ring-primary-500 outline-none transition-all"
                  placeholder="Describe what happened in detail..."
                  rows={4}
                  {...register('description')}
                />
                {errors.description && <p className="text-sm text-red-500">{errors.description.message}</p>}
              </div>

            </CardContent>
          </Card>

          <div className="flex justify-end">
            <Button type="submit" disabled={isSubmitting || submitMutation.isPending} className="w-full md:w-auto px-8">
              {(isSubmitting || submitMutation.isPending) ? (
                <Loader2 className="w-4 h-4 mr-2 animate-spin" />
              ) : (
                <Save className="w-4 h-4 mr-2" />
              )}
              Submit Claim
            </Button>
          </div>
        </form>
      )}
      
    </div>
  );
};
