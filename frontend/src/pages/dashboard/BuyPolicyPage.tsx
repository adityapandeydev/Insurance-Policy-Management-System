import { useState } from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import * as z from 'zod';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { Shield, Save, Loader2, CheckCircle2, DollarSign, Calendar, Info, Activity } from 'lucide-react';
import { useNavigate } from 'react-router-dom';

import { Card, CardContent, CardHeader, CardTitle, CardDescription } from '../../components/ui/Card';
import { Button } from '../../components/ui/Button';
import { policyService } from '../../api/services/policyService';
import { customerService } from '../../api/services/customerService';

const policySchema = z.object({
  policyName: z.string().min(3, 'Policy name must be at least 3 characters').max(200),
  policyType: z.enum(['LIFE', 'HEALTH', 'VEHICLE', 'PROPERTY', 'TRAVEL']),
  description: z.string().optional(),
  coverageAmount: z.number({ invalid_type_error: "Coverage must be a number" }).min(1000, 'Minimum coverage is $1000').max(100000000),
  startDate: z.string().min(1, 'Start date is required'),
  endDate: z.string().min(1, 'End date is required'),
  premiumFrequency: z.enum(['MONTHLY', 'QUARTERLY', 'SEMI_ANNUAL', 'ANNUAL']),
});

type PolicyFormValues = z.infer<typeof policySchema>;

export const BuyPolicyPage = () => {
  const [successMsg, setSuccessMsg] = useState('');
  const queryClient = useQueryClient();
  const navigate = useNavigate();

  // Need profile to get customer ID
  const { data: profile, isLoading: profileLoading } = useQuery({
    queryKey: ['myProfile'],
    queryFn: customerService.getMyProfile,
  });

  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<PolicyFormValues>({
    resolver: zodResolver(policySchema),
    defaultValues: {
      policyName: '',
      policyType: 'HEALTH',
      description: '',
      coverageAmount: 50000,
      startDate: new Date().toISOString().split('T')[0],
      endDate: new Date(new Date().setFullYear(new Date().getFullYear() + 1)).toISOString().split('T')[0],
      premiumFrequency: 'MONTHLY',
    },
  });

  const createMutation = useMutation({
    mutationFn: async (data: PolicyFormValues) => {
      if (!profile?.id) throw new Error("Customer ID not found");
      return policyService.createPolicy({ ...data, customerId: profile.id });
    },
    onSuccess: (newPolicy) => {
      setSuccessMsg(`Policy successfully created! Your estimated premium is $${newPolicy.premiumAmount}/period.`);
      queryClient.invalidateQueries({ queryKey: ['myPolicies'] });
      setTimeout(() => navigate('/'), 3000);
    },
  });

  const onSubmit = (data: PolicyFormValues) => {
    createMutation.mutate(data);
  };

  if (profileLoading) {
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
        <h1 className="text-3xl font-bold text-foreground">Purchase New Policy</h1>
        <p className="text-foreground/60 mt-1">Configure your perfect insurance coverage below. Premiums are auto-calculated based on your risk profile.</p>
      </div>

      {successMsg && (
        <div className="p-4 bg-green-50 dark:bg-green-900/20 border border-green-200 dark:border-green-800/50 rounded-lg flex items-center text-green-700 dark:text-green-400 animate-in slide-in-from-top-2">
          <CheckCircle2 className="w-5 h-5 mr-2" />
          {successMsg}
        </div>
      )}

      <form onSubmit={handleSubmit(onSubmit)} className="space-y-8">
        <Card>
          <CardHeader>
            <CardTitle className="flex items-center">
              <Shield className="w-5 h-5 mr-2 text-primary-500" />
              Policy Details
            </CardTitle>
            <CardDescription>Select the type and amount of coverage you need.</CardDescription>
          </CardHeader>
          <CardContent className="grid grid-cols-1 md:grid-cols-2 gap-6">
            
            <div className="space-y-2">
              <label className="text-sm font-medium text-foreground">Policy Name</label>
              <div className="relative">
                <Info className="absolute left-3 top-3 w-5 h-5 text-foreground/40" />
                <input
                  type="text"
                  className="w-full pl-10 p-2.5 bg-background border border-border rounded-lg text-foreground focus:ring-2 focus:ring-primary-500 outline-none transition-all"
                  placeholder="e.g. My Primary Vehicle"
                  {...register('policyName')}
                />
              </div>
              {errors.policyName && <p className="text-sm text-red-500">{errors.policyName.message}</p>}
            </div>

            <div className="space-y-2">
              <label className="text-sm font-medium text-foreground">Policy Type</label>
              <div className="relative">
                <Activity className="absolute left-3 top-3 w-5 h-5 text-foreground/40" />
                <select
                  className="w-full pl-10 p-2.5 bg-background border border-border rounded-lg text-foreground focus:ring-2 focus:ring-primary-500 outline-none transition-all appearance-none"
                  {...register('policyType')}
                >
                  <option value="LIFE">Life Insurance</option>
                  <option value="HEALTH">Health Insurance</option>
                  <option value="VEHICLE">Vehicle Insurance</option>
                  <option value="PROPERTY">Property Insurance</option>
                  <option value="TRAVEL">Travel Insurance</option>
                </select>
              </div>
              {errors.policyType && <p className="text-sm text-red-500">{errors.policyType.message}</p>}
            </div>

            <div className="space-y-2 md:col-span-2">
              <label className="text-sm font-medium text-foreground">Coverage Amount ($)</label>
              <div className="relative">
                <DollarSign className="absolute left-3 top-3 w-5 h-5 text-foreground/40" />
                <input
                  type="number"
                  className="w-full pl-10 p-2.5 bg-background border border-border rounded-lg text-foreground focus:ring-2 focus:ring-primary-500 outline-none transition-all"
                  placeholder="50000"
                  {...register('coverageAmount', { valueAsNumber: true })}
                />
              </div>
              {errors.coverageAmount && <p className="text-sm text-red-500">{errors.coverageAmount.message}</p>}
            </div>

            <div className="space-y-2 md:col-span-2">
              <label className="text-sm font-medium text-foreground">Description (Optional)</label>
              <textarea
                className="w-full p-2.5 bg-background border border-border rounded-lg text-foreground focus:ring-2 focus:ring-primary-500 outline-none transition-all"
                placeholder="Any specific details about this policy..."
                rows={3}
                {...register('description')}
              />
            </div>

          </CardContent>
        </Card>

        <Card>
          <CardHeader>
            <CardTitle className="flex items-center">
              <Calendar className="w-5 h-5 mr-2 text-primary-500" />
              Terms & Schedule
            </CardTitle>
            <CardDescription>Configure the duration and payment frequency.</CardDescription>
          </CardHeader>
          <CardContent className="grid grid-cols-1 md:grid-cols-2 gap-6">
            
            <div className="space-y-2">
              <label className="text-sm font-medium text-foreground">Start Date</label>
              <input
                type="date"
                className="w-full p-2.5 bg-background border border-border rounded-lg text-foreground focus:ring-2 focus:ring-primary-500 outline-none transition-all"
                {...register('startDate')}
              />
              {errors.startDate && <p className="text-sm text-red-500">{errors.startDate.message}</p>}
            </div>

            <div className="space-y-2">
              <label className="text-sm font-medium text-foreground">End Date</label>
              <input
                type="date"
                className="w-full p-2.5 bg-background border border-border rounded-lg text-foreground focus:ring-2 focus:ring-primary-500 outline-none transition-all"
                {...register('endDate')}
              />
              {errors.endDate && <p className="text-sm text-red-500">{errors.endDate.message}</p>}
            </div>

            <div className="space-y-2 md:col-span-2">
              <label className="text-sm font-medium text-foreground">Premium Payment Frequency</label>
              <select
                className="w-full p-2.5 bg-background border border-border rounded-lg text-foreground focus:ring-2 focus:ring-primary-500 outline-none transition-all"
                {...register('premiumFrequency')}
              >
                <option value="MONTHLY">Monthly</option>
                <option value="QUARTERLY">Quarterly</option>
                <option value="SEMI_ANNUAL">Semi-Annually</option>
                <option value="ANNUAL">Annually</option>
              </select>
              {errors.premiumFrequency && <p className="text-sm text-red-500">{errors.premiumFrequency.message}</p>}
            </div>

          </CardContent>
        </Card>

        <div className="flex justify-end">
          <Button type="submit" disabled={isSubmitting || createMutation.isPending} className="w-full md:w-auto px-8">
            {(isSubmitting || createMutation.isPending) ? (
              <Loader2 className="w-4 h-4 mr-2 animate-spin" />
            ) : (
              <Save className="w-4 h-4 mr-2" />
            )}
            Purchase Policy
          </Button>
        </div>
      </form>
      
    </div>
  );
};
