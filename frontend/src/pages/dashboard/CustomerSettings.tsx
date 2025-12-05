import { useState, useEffect } from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import * as z from 'zod';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { Save, User, MapPin, Phone, Shield, Briefcase, Loader2, CheckCircle2 } from 'lucide-react';

import { Card, CardContent, CardHeader, CardTitle, CardDescription } from '../../components/ui/Card';
import { Button } from '../../components/ui/Button';
import { customerService } from '../../api/services/customerService';
import { useAuthStore } from '../../store/authStore';
import { api } from '../../api/axiosInstance'; // For the raw PUT request since we didn't add it to customerService yet

// Form Validation Schema
const profileSchema = z.object({
  phoneNumber: z.string().min(10, 'Phone number must be at least 10 characters'),
  dateOfBirth: z.string().min(1, 'Date of Birth is required'),
  address: z.string().min(5, 'Address is required'),
  nationalId: z.string().min(5, 'National ID is required'),
  occupation: z.string().min(2, 'Occupation is required'),
  emergencyContactName: z.string().min(2, 'Emergency Contact Name is required'),
  emergencyContactPhone: z.string().min(10, 'Emergency Contact Phone is required'),
});

type ProfileFormValues = z.infer<typeof profileSchema>;

export const CustomerSettings = () => {
  const [successMsg, setSuccessMsg] = useState('');
  const queryClient = useQueryClient();
  const { username } = useAuthStore();

  const { data: profile, isLoading } = useQuery({
    queryKey: ['myProfile'],
    queryFn: customerService.getMyProfile,
  });

  const {
    register,
    handleSubmit,
    reset,
    formState: { errors, isSubmitting, isDirty },
  } = useForm<ProfileFormValues>({
    resolver: zodResolver(profileSchema),
    defaultValues: {
      phoneNumber: '',
      dateOfBirth: '',
      address: '',
      nationalId: '',
      occupation: '',
      emergencyContactName: '',
      emergencyContactPhone: '',
    },
  });

  // Populate form when profile data loads
  useEffect(() => {
    if (profile) {
      reset({
        phoneNumber: profile.phoneNumber || '',
        dateOfBirth: profile.dateOfBirth || '',
        address: profile.address || '',
        nationalId: profile.nationalId || '',
        occupation: profile.occupation || '',
        emergencyContactName: profile.emergencyContactName || '',
        emergencyContactPhone: profile.emergencyContactPhone || '',
      });
    }
  }, [profile, reset]);

  const updateMutation = useMutation({
    mutationFn: async (data: ProfileFormValues) => {
      const response = await api.put(`/customers/${profile?.id}`, data);
      return response.data.data;
    },
    onSuccess: () => {
      setSuccessMsg('Profile updated successfully!');
      queryClient.invalidateQueries({ queryKey: ['myProfile'] });
      setTimeout(() => setSuccessMsg(''), 5000);
    },
  });

  const onSubmit = (data: ProfileFormValues) => {
    updateMutation.mutate(data);
  };

  if (isLoading) {
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
        <h1 className="text-3xl font-bold text-foreground">Profile Settings</h1>
        <p className="text-foreground/60 mt-1">Manage your account information and preferences.</p>
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
              <User className="w-5 h-5 mr-2 text-primary-500" />
              Personal Details
            </CardTitle>
            <CardDescription>This information helps us tailor your insurance policies.</CardDescription>
          </CardHeader>
          <CardContent className="grid grid-cols-1 md:grid-cols-2 gap-6">
            
            <div className="space-y-2">
              <label className="text-sm font-medium text-foreground">Phone Number</label>
              <input
                type="tel"
                className="w-full p-2.5 bg-background border border-border rounded-lg text-foreground focus:ring-2 focus:ring-primary-500 outline-none transition-all"
                placeholder="+1 (555) 000-0000"
                {...register('phoneNumber')}
              />
              {errors.phoneNumber && <p className="text-sm text-red-500">{errors.phoneNumber.message}</p>}
            </div>

            <div className="space-y-2">
              <label className="text-sm font-medium text-foreground">Date of Birth</label>
              <input
                type="date"
                className="w-full p-2.5 bg-background border border-border rounded-lg text-foreground focus:ring-2 focus:ring-primary-500 outline-none transition-all"
                {...register('dateOfBirth')}
              />
              {errors.dateOfBirth && <p className="text-sm text-red-500">{errors.dateOfBirth.message}</p>}
            </div>

            <div className="space-y-2 md:col-span-2">
              <label className="text-sm font-medium text-foreground">Physical Address</label>
              <div className="relative">
                <MapPin className="absolute left-3 top-3 w-5 h-5 text-foreground/40" />
                <input
                  type="text"
                  className="w-full pl-10 p-2.5 bg-background border border-border rounded-lg text-foreground focus:ring-2 focus:ring-primary-500 outline-none transition-all"
                  placeholder="123 Silicon Valley, CA 94025"
                  {...register('address')}
                />
              </div>
              {errors.address && <p className="text-sm text-red-500">{errors.address.message}</p>}
            </div>

            <div className="space-y-2">
              <label className="text-sm font-medium text-foreground">National ID</label>
              <div className="relative">
                <Shield className="absolute left-3 top-3 w-5 h-5 text-foreground/40" />
                <input
                  type="text"
                  className="w-full pl-10 p-2.5 bg-background border border-border rounded-lg text-foreground focus:ring-2 focus:ring-primary-500 outline-none transition-all"
                  placeholder="ID-XXXXXXXX"
                  {...register('nationalId')}
                />
              </div>
              {errors.nationalId && <p className="text-sm text-red-500">{errors.nationalId.message}</p>}
            </div>

            <div className="space-y-2">
              <label className="text-sm font-medium text-foreground">Occupation</label>
              <div className="relative">
                <Briefcase className="absolute left-3 top-3 w-5 h-5 text-foreground/40" />
                <input
                  type="text"
                  className="w-full pl-10 p-2.5 bg-background border border-border rounded-lg text-foreground focus:ring-2 focus:ring-primary-500 outline-none transition-all"
                  placeholder="Software Engineer"
                  {...register('occupation')}
                />
              </div>
              {errors.occupation && <p className="text-sm text-red-500">{errors.occupation.message}</p>}
            </div>

          </CardContent>
        </Card>

        <Card>
          <CardHeader>
            <CardTitle className="flex items-center">
              <Phone className="w-5 h-5 mr-2 text-primary-500" />
              Emergency Contact
            </CardTitle>
            <CardDescription>Who should we contact in case of an emergency?</CardDescription>
          </CardHeader>
          <CardContent className="grid grid-cols-1 md:grid-cols-2 gap-6">
            
            <div className="space-y-2">
              <label className="text-sm font-medium text-foreground">Contact Name</label>
              <input
                type="text"
                className="w-full p-2.5 bg-background border border-border rounded-lg text-foreground focus:ring-2 focus:ring-primary-500 outline-none transition-all"
                placeholder="Jane Doe"
                {...register('emergencyContactName')}
              />
              {errors.emergencyContactName && <p className="text-sm text-red-500">{errors.emergencyContactName.message}</p>}
            </div>

            <div className="space-y-2">
              <label className="text-sm font-medium text-foreground">Contact Phone</label>
              <input
                type="tel"
                className="w-full p-2.5 bg-background border border-border rounded-lg text-foreground focus:ring-2 focus:ring-primary-500 outline-none transition-all"
                placeholder="+1 (555) 999-9999"
                {...register('emergencyContactPhone')}
              />
              {errors.emergencyContactPhone && <p className="text-sm text-red-500">{errors.emergencyContactPhone.message}</p>}
            </div>

          </CardContent>
        </Card>

        <div className="flex justify-end">
          <Button type="submit" disabled={!isDirty || isSubmitting || updateMutation.isPending} className="w-full md:w-auto">
            {(isSubmitting || updateMutation.isPending) ? (
              <Loader2 className="w-4 h-4 mr-2 animate-spin" />
            ) : (
              <Save className="w-4 h-4 mr-2" />
            )}
            Save Changes
          </Button>
        </div>
      </form>
      
    </div>
  );
};
