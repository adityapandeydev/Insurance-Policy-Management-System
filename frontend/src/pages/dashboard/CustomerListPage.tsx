import { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { useNavigate } from 'react-router-dom';
import { Search, Mail, Phone, MapPin, Loader2 } from 'lucide-react';
import { Card, CardContent } from '../../components/ui/Card';
import { Button } from '../../components/ui/Button';
import { customerService } from '../../api/services/customerService';
import { useAuthStore } from '../../store/authStore';
import { AddCustomerModal } from './AddCustomerModal';
import { UserPlus } from 'lucide-react';

export const CustomerListPage = () => {
  const [searchTerm, setSearchTerm] = useState('');
  const [isModalOpen, setIsModalOpen] = useState(false);
  const { role } = useAuthStore();
  const navigate = useNavigate();
  const isAgent = role === 'ROLE_AGENT' || role === 'ROLE_ADMIN';

  const { data: customers, isLoading } = useQuery({
    queryKey: ['customers', searchTerm],
    queryFn: () => {
      if (searchTerm) {
        return customerService.searchCustomers(searchTerm);
      }
      return customerService.getAllCustomers();
    },
    enabled: isAgent,
  });

  if (!isAgent) {
    return <div className="p-8 text-center text-red-500">Access Denied</div>;
  }

  return (
    <div className="space-y-8 animate-in fade-in slide-in-from-bottom-4 duration-500 max-w-7xl mx-auto">
      <div className="flex flex-col md:flex-row justify-between items-start md:items-center gap-4">
        <div>
          <h1 className="text-3xl font-bold text-foreground">My Customers</h1>
          <p className="text-foreground/60 mt-1">Manage and view details of your assigned customers.</p>
        </div>
        
        <div className="relative w-full md:w-64">
          <input
            type="text"
            placeholder="Search customers..."
            value={searchTerm}
            onChange={(e) => setSearchTerm(e.target.value)}
            className="w-full pl-10 pr-4 py-2 rounded-xl bg-background border border-border text-foreground focus:outline-none focus:ring-2 focus:ring-primary-500"
          />
          <Search className="absolute left-3 top-2.5 w-5 h-5 text-foreground/40" />
        </div>
        <Button onClick={() => setIsModalOpen(true)}>
          <UserPlus className="w-4 h-4 mr-2" />
          Add Customer
        </Button>
      </div>

      {isLoading ? (
        <div className="flex flex-col items-center justify-center h-64 text-foreground/50">
          <Loader2 className="w-8 h-8 animate-spin mb-4 text-primary-500" />
          <p>Loading customers...</p>
        </div>
      ) : customers?.content?.length === 0 ? (
        <Card className="border-dashed">
          <CardContent className="flex flex-col items-center justify-center py-16 text-center">
            <div className="w-16 h-16 rounded-full bg-primary-100 dark:bg-primary-900/30 flex items-center justify-center text-primary-500 mb-4">
              <Search className="w-8 h-8" />
            </div>
            <h3 className="text-xl font-bold text-foreground">No customers found</h3>
            <p className="text-foreground/60 mt-2 max-w-md">
              {searchTerm 
                ? `No results match your search for "${searchTerm}".`
                : "You don't have any customers assigned to you yet."}
            </p>
          </CardContent>
        </Card>
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
          {customers?.content?.map((customer) => (
            <Card key={customer.id} className="hover:shadow-md transition-shadow group relative overflow-hidden">
              <div className="absolute top-0 left-0 w-1 h-full bg-primary-500 opacity-0 group-hover:opacity-100 transition-opacity" />
              <CardContent className="p-6">
                <div className="flex items-start justify-between mb-4">
                  <div className="flex items-center gap-3">
                    <div className="w-12 h-12 rounded-full bg-primary-100 dark:bg-primary-900/30 flex items-center justify-center text-primary-700 dark:text-primary-400 font-bold text-lg">
                      {customer.firstName?.charAt(0)}
                    </div>
                    <div>
                      <h3 className="font-bold text-foreground text-lg">{customer.firstName} {customer.lastName}</h3>
                      <p className="text-sm text-foreground/60">ID: {customer.id}</p>
                    </div>
                  </div>
                </div>

                <div className="space-y-3 mt-6">
                  <div className="flex items-center text-sm text-foreground/70">
                    <Mail className="w-4 h-4 mr-3 text-foreground/40" />
                    <span className="truncate">{customer.email}</span>
                  </div>
                  <div className="flex items-center text-sm text-foreground/70">
                    <Phone className="w-4 h-4 mr-3 text-foreground/40" />
                    <span>{customer.phoneNumber}</span>
                  </div>
                  <div className="flex items-center text-sm text-foreground/70">
                    <MapPin className="w-4 h-4 mr-3 text-foreground/40" />
                    <span className="truncate">{customer.address}</span>
                  </div>
                </div>
                
                <div className="mt-6 pt-4 border-t border-border flex justify-end">
                  <Button variant="outline" size="sm" className="w-full" onClick={() => navigate(`/customers/${customer.id}`)}>
                    View Details
                  </Button>
                </div>
              </CardContent>
            </Card>
          ))}
        </div>
      )}

      <AddCustomerModal isOpen={isModalOpen} onClose={() => setIsModalOpen(false)} />
    </div>
  );
};
