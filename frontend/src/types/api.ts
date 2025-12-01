export type Role = 'ROLE_CUSTOMER' | 'ROLE_AGENT' | 'ROLE_ADMIN';
export type PolicyStatus = 'PENDING' | 'ACTIVE' | 'EXPIRED' | 'CANCELLED';
export type PolicyType = 'HEALTH' | 'AUTO' | 'HOME' | 'LIFE' | 'TRAVEL';
export type ClaimStatus = 'SUBMITTED' | 'UNDER_REVIEW' | 'APPROVED' | 'REJECTED' | 'PAID';

export interface ApiResponse<T> {
  success: boolean;
  message: string;
  data: T;
  statusCode: number;
}

export interface CustomerResponse {
  id: number;
  phoneNumber: string;
  address: string;
  dateOfBirth: string;
  nationalId: string;
  emergencyContactName: string;
  emergencyContactPhone: string;
  occupation: string;
  age: number;
  userId: number;
  firstName: string;
  lastName: string;
  email: string;
  role: Role;
  totalPolicies: number;
  totalClaims: number;
  createdAt: string;
  updatedAt: string;
}

export interface PolicyResponse {
  id: number;
  policyNumber: string;
  policyName: string;
  policyType: PolicyType;
  description: string;
  coverageAmount: number;
  premiumAmount: number;
  startDate: string;
  endDate: string;
  status: PolicyStatus;
  premiumFrequency: string;
  customerId: number;
  customerName: string;
  customerEmail: string;
  totalClaims: number;
  approvedClaims: number;
  createdAt: string;
  updatedAt: string;
}

export interface ClaimResponse {
  id: number;
  claimNumber: string;
  description: string;
  claimAmount: number;
  status: ClaimStatus;
  reviewNotes: string;
  submittedAt: string;
  reviewedAt: string;
  incidentDate: string;
  policyId: number;
  policyNumber: string;
  policyName: string;
  policyCoverageAmount: number;
  customerId: number;
  customerName: string;
  customerEmail: string;
  createdAt: string;
  updatedAt: string;
}

export interface DashboardResponse {
  totalCustomers: number;
  totalPolicies: number;
  activePolicies: number;
  expiredPolicies: number;
  pendingPolicies: number;
  cancelledPolicies: number;
  totalClaims: number;
  pendingClaims: number;
  claimsUnderReview: number;
  approvedClaims: number;
  rejectedClaims: number;
  lowRiskCustomers: number;
  mediumRiskCustomers: number;
  highRiskCustomers: number;
  totalUsers: number;
  totalAgents: number;
  totalAdmins: number;
  generatedAt: string;
}

export interface PaginatedResponse<T> {
  content: T[];
  pageable: {
    pageNumber: number;
    pageSize: number;
  };
  totalElements: number;
  totalPages: number;
  last: boolean;
  size: number;
  number: number;
  first: boolean;
  numberOfElements: number;
  empty: boolean;
}
