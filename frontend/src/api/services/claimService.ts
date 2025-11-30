import { api as axiosInstance } from '../axiosInstance';
import type { ApiResponse, ClaimResponse, PaginatedResponse } from '../../types/api';

export const claimService = {
  getClaimsByPolicy: async (policyId: number): Promise<ClaimResponse[]> => {
    const { data } = await axiosInstance.get<ApiResponse<ClaimResponse[]>>(`/claims/policy/${policyId}`);
    return data.data;
  },

  getAllClaims: async (page = 0, size = 10): Promise<PaginatedResponse<ClaimResponse>> => {
    const { data } = await axiosInstance.get<ApiResponse<PaginatedResponse<ClaimResponse>>>(`/claims?page=${page}&size=${size}`);
    return data.data;
  },

  getClaimById: async (id: number): Promise<ClaimResponse> => {
    const { data } = await axiosInstance.get<ApiResponse<ClaimResponse>>(`/claims/${id}`);
    return data.data;
  },

  submitClaim: async (claimData: any): Promise<ClaimResponse> => {
    const { data } = await axiosInstance.post<ApiResponse<ClaimResponse>>('/claims', claimData);
    return data.data;
  },

  approveClaim: async (id: number, reviewNotes: string): Promise<ClaimResponse> => {
    const { data } = await axiosInstance.post<ApiResponse<ClaimResponse>>(`/claims/${id}/approve`, { reviewNotes });
    return data.data;
  },

  rejectClaim: async (id: number, reviewNotes: string): Promise<ClaimResponse> => {
    const { data } = await axiosInstance.post<ApiResponse<ClaimResponse>>(`/claims/${id}/reject`, { reviewNotes });
    return data.data;
  }
};
