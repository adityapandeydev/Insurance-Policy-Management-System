import { api as axiosInstance } from '../axiosInstance';
import type { ApiResponse, CustomerResponse, PaginatedResponse } from '../../types/api';

export const customerService = {
  getMyProfile: async (): Promise<CustomerResponse> => {
    const { data } = await axiosInstance.get<ApiResponse<CustomerResponse>>('/customers/me');
    return data.data;
  },

  getAllCustomers: async (page = 0, size = 10, search?: string): Promise<PaginatedResponse<CustomerResponse>> => {
    const params = new URLSearchParams({ page: page.toString(), size: size.toString() });
    if (search) params.append('search', search);
    
    const { data } = await axiosInstance.get<ApiResponse<PaginatedResponse<CustomerResponse>>>(`/customers?${params.toString()}`);
    return data.data;
  },

  getCustomerById: async (id: number): Promise<CustomerResponse> => {
    const { data } = await axiosInstance.get<ApiResponse<CustomerResponse>>(`/customers/${id}`);
    return data.data;
  },

  createCustomerByAgent: async (customerData: any): Promise<CustomerResponse> => {
    const { data } = await axiosInstance.post<ApiResponse<CustomerResponse>>('/customers', customerData);
    return data.data;
  }
};
