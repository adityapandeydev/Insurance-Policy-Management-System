import { api as axiosInstance } from '../axiosInstance';
import type { ApiResponse, DashboardResponse } from '../../types/api';

export const dashboardService = {
  getDashboardMetrics: async (): Promise<DashboardResponse> => {
    const { data } = await axiosInstance.get<ApiResponse<DashboardResponse>>('/dashboard');
    return data.data;
  }
};
