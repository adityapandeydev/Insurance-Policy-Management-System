import { useParams, useNavigate } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import { ArrowLeft, Mail, Phone, MapPin, Calendar, Activity, Loader2, Shield } from 'lucide-react';
import { Card, CardContent, CardHeader, CardTitle } from '../../components/ui/Card';
import { Button } from '../../components/ui/Button';
import { Badge } from '../../components/ui/Badge';
import { customerService } from '../../api/services/customerService';
import { useAuthStore } from '../../store/authStore';

export const CustomerDetailPage = () => {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const { role } = useAuthStore();
  const isAgent = role === 'ROLE_AGENT' || role === 'ROLE_ADMIN';

  const { data: customer, isLoading } = useQuery({
    queryKey: ['customer', id],
    queryFn: () => customerService.getCustomerById(Number(id)),
    enabled: isAgent && !!id,
  });

  if (!isAgent) {
    return <div className="p-8 text-center text-red-500">Access Denied</div>;
  }

  if (isLoading) {
    return (
      <div className="flex flex-col items-center justify-center h-64 text-foreground/50">
        <Loader2 className="w-8 h-8 animate-spin mb-4 text-primary-500" />
        <p>Loading customer details...</p>
      </div>
    );
  }

  if (!customer) {
    return (
      <div className="p-8 text-center text-foreground/60">
        Customer not found.
      </div>
    );
  }

  return (
    <div className="space-y-8 animate-in fade-in slide-in-from-bottom-4 duration-500 max-w-4xl mx-auto">
      <div className="flex items-center justify-between">
        <div className="flex items-center gap-4">
          <Button variant="outline" onClick={() => navigate('/customers')} className="px-3">
            <ArrowLeft className="w-4 h-4" />
          </Button>
          <div>
            <h1 className="text-3xl font-bold text-foreground">
              {customer.firstName} {customer.lastName}
            </h1>
            <p className="text-foreground/60">Customer ID: {customer.id}</p>
          </div>
        </div>
        <Badge variant={customer.enabled ? "success" : "destructive"}>
          {customer.enabled ? "Active Account" : "Inactive Account"}
        </Badge>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
        <Card>
          <CardHeader>
            <CardTitle className="flex items-center text-lg">
              <Activity className="w-5 h-5 mr-2 text-primary-500" />
              Contact Information
            </CardTitle>
          </CardHeader>
          <CardContent className="space-y-4">
            <div className="flex items-center text-foreground/80">
              <Mail className="w-5 h-5 mr-3 text-foreground/40" />
              {customer.email}
            </div>
            <div className="flex items-center text-foreground/80">
              <Phone className="w-5 h-5 mr-3 text-foreground/40" />
              {customer.phoneNumber || 'Not provided'}
            </div>
            <div className="flex items-start text-foreground/80">
              <MapPin className="w-5 h-5 mr-3 mt-1 text-foreground/40" />
              <span>{customer.address || 'Not provided'}</span>
            </div>
          </CardContent>
        </Card>

        <Card>
          <CardHeader>
            <CardTitle className="flex items-center text-lg">
              <Shield className="w-5 h-5 mr-2 text-primary-500" />
              Personal Details
            </CardTitle>
          </CardHeader>
          <CardContent className="space-y-4">
            <div className="flex items-center text-foreground/80">
              <Calendar className="w-5 h-5 mr-3 text-foreground/40" />
              DOB: {customer.dateOfBirth ? new Date(customer.dateOfBirth).toLocaleDateString() : 'Not provided'}
            </div>
            <div className="flex items-center text-foreground/80">
              <span className="font-semibold w-24">National ID:</span>
              {customer.nationalId || 'Not provided'}
            </div>
            <div className="flex items-center text-foreground/80">
              <span className="font-semibold w-24">Occupation:</span>
              {customer.occupation || 'Not provided'}
            </div>
          </CardContent>
        </Card>
      </div>
    </div>
  );
};
