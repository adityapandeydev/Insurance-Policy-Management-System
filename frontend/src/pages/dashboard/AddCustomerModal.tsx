import { useState } from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import * as z from 'zod';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { UserPlus, Loader2, X } from 'lucide-react';
import { Button } from '../../components/ui/Button';
import { customerService } from '../../api/services/customerService';

const customerSchema = z.object({
  firstName: z.string().min(2, 'First name is required'),
  lastName: z.string().min(2, 'Last name is required'),
  email: z.string().email('Invalid email address'),
  password: z.string().min(6, 'Password must be at least 6 characters'),
  phoneNumber: z.string().optional(),
  dateOfBirth: z.string().optional(),
  address: z.string().optional()
});

type CustomerFormValues = z.infer<typeof customerSchema>;

export const AddCustomerModal = ({ isOpen, onClose }: { isOpen: boolean, onClose: () => void }) => {
  const queryClient = useQueryClient();
  const [errorMsg, setErrorMsg] = useState('');

  const {
    register,
    handleSubmit,
    reset,
    formState: { errors, isSubmitting },
  } = useForm<CustomerFormValues>({
    resolver: zodResolver(customerSchema),
  });

  const createMutation = useMutation({
    mutationFn: (data: CustomerFormValues) => customerService.createCustomerByAgent(data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['customers'] });
      reset();
      onClose();
    },
    onError: (error: any) => {
      setErrorMsg(error?.response?.data?.message || 'Failed to create customer');
    }
  });

  const onSubmit = (data: CustomerFormValues) => {
    setErrorMsg('');
    createMutation.mutate(data);
  };

  if (!isOpen) return null;

  return (
    <div className="fixed inset-0 z-50 bg-background/80 backdrop-blur-sm flex items-center justify-center p-4">
      <div className="bg-card w-full max-w-lg rounded-xl shadow-xl border border-border animate-in zoom-in-95 duration-200">
        <div className="flex items-center justify-between p-6 border-b border-border">
          <h2 className="text-xl font-semibold flex items-center">
            <UserPlus className="w-5 h-5 mr-2 text-primary-500" />
            Add New Customer
          </h2>
          <button onClick={onClose} className="text-foreground/50 hover:text-foreground">
            <X className="w-5 h-5" />
          </button>
        </div>

        <form onSubmit={handleSubmit(onSubmit)} className="p-6 space-y-4">
          {errorMsg && (
            <div className="p-3 bg-red-50 dark:bg-red-900/20 text-red-600 dark:text-red-400 rounded-lg text-sm">
              {errorMsg}
            </div>
          )}

          <div className="grid grid-cols-2 gap-4">
            <div className="space-y-1">
              <label className="text-sm font-medium">First Name *</label>
              <input
                type="text"
                className="w-full p-2 bg-background border border-border rounded-lg"
                {...register('firstName')}
              />
              {errors.firstName && <p className="text-xs text-red-500">{errors.firstName.message}</p>}
            </div>
            <div className="space-y-1">
              <label className="text-sm font-medium">Last Name *</label>
              <input
                type="text"
                className="w-full p-2 bg-background border border-border rounded-lg"
                {...register('lastName')}
              />
              {errors.lastName && <p className="text-xs text-red-500">{errors.lastName.message}</p>}
            </div>
          </div>

          <div className="space-y-1">
            <label className="text-sm font-medium">Email *</label>
            <input
              type="email"
              className="w-full p-2 bg-background border border-border rounded-lg"
              {...register('email')}
            />
            {errors.email && <p className="text-xs text-red-500">{errors.email.message}</p>}
          </div>

          <div className="space-y-1">
            <label className="text-sm font-medium">Temporary Password *</label>
            <input
              type="password"
              className="w-full p-2 bg-background border border-border rounded-lg"
              {...register('password')}
            />
            {errors.password && <p className="text-xs text-red-500">{errors.password.message}</p>}
          </div>

          <div className="grid grid-cols-2 gap-4">
            <div className="space-y-1">
              <label className="text-sm font-medium">Phone</label>
              <input
                type="text"
                className="w-full p-2 bg-background border border-border rounded-lg"
                {...register('phoneNumber')}
              />
            </div>
            <div className="space-y-1">
              <label className="text-sm font-medium">Date of Birth</label>
              <input
                type="date"
                className="w-full p-2 bg-background border border-border rounded-lg"
                {...register('dateOfBirth')}
              />
            </div>
          </div>

          <div className="pt-4 flex justify-end gap-3">
            <Button type="button" variant="outline" onClick={onClose}>
              Cancel
            </Button>
            <Button type="submit" disabled={isSubmitting || createMutation.isPending}>
              {(isSubmitting || createMutation.isPending) && <Loader2 className="w-4 h-4 mr-2 animate-spin" />}
              Create Customer
            </Button>
          </div>
        </form>
      </div>
    </div>
  );
};
