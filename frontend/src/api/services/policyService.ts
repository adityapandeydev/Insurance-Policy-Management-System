import { api as axiosInstance } from '../axiosInstance';
import type { ApiResponse, PolicyResponse, PaginatedResponse } from '../../types/api';

export const policyService = {
  getPoliciesByCustomer: async (customerId: number): Promise<PaginatedResponse<PolicyResponse>> => {
    const { data } = await axiosInstance.get<ApiResponse<PaginatedResponse<PolicyResponse>>>(`/policies/customer/${customerId}`);
    return data.data;
  },

  getAllPolicies: async (page = 0, size = 10): Promise<PaginatedResponse<PolicyResponse>> => {
    const { data } = await axiosInstance.get<ApiResponse<PaginatedResponse<PolicyResponse>>>(`/policies?page=${page}&size=${size}`);
    return data.data;
  },

  getPolicyById: async (id: number): Promise<PolicyResponse> => {
    const { data } = await axiosInstance.get<ApiResponse<PolicyResponse>>(`/policies/${id}`);
    return data.data;
  },

  createPolicy: async (policyData: any): Promise<PolicyResponse> => {
    const { data } = await axiosInstance.post<ApiResponse<PolicyResponse>>('/policies', policyData);
    return data.data;
  },

  updatePolicyStatus: async (id: number, status: string): Promise<PolicyResponse> => {
    const { data } = await axiosInstance.patch<ApiResponse<PolicyResponse>>(`/policies/${id}/status?status=${status}`);
    return data.data;
  }
};
