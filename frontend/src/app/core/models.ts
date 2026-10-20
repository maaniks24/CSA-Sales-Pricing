export interface Customer {
  id?: number;
  code: string;
  name: string;
  address?: string;
  city?: string;
  vertical?: string;
  contactName?: string;
  contactEmail?: string;
  contactPhone?: string;
  assetCount: number;
  branchCount: number;
  discountPercent: number;
  active: boolean;
}

export interface Software {
  id?: number;
  code: string;
  name: string;
  description?: string;
  scopeText?: string;
  price: number;
  assetBlock: number;
  implementationCharge: number;
  implementationText?: string;
  active: boolean;
}

export type ResourceType = 'TRACKING' | 'DATA_AUDIT' | 'SR_EXECUTIVE';

export interface ServiceItem {
  id?: number;
  code: string;
  name: string;
  activity?: string;
  sequenceNo: number;
  assetsPerDay: number;
  resourceType: ResourceType;
  includeRoom: boolean;
  includeFood: boolean;
  defaultInScope: boolean;
  active: boolean;
}

export interface PricingParams {
  trackingRoom: number;
  trackingFood: number;
  trackingSalary: number;
  dataAuditSalary: number;
  srExecRoom: number;
  srExecFood: number;
  srExecSalary: number;
  costPerBranch: number;
  travelCost: number;
  overheadPercent: number;
  profitPercent: number;
}

export interface DocTerms {
  gstPercent: number;
  validityDays: number;
  commencementDays: number;
  trackingTeamSize: number;
  dataTeamSize: number;
  pay1: number;
  pay2: number;
  pay3: number;
}

export interface PricingConfig {
  params: PricingParams;
  terms: DocTerms;
}

export type ProposalStatus = 'SAVED' | 'PO_GENERATED';

export interface ProposalRequest {
  customerId: number;
  contactName?: string;
  proposalDate?: string;
  assetCount?: number;
  branchCount?: number;
  srExecDays?: number;
  discountPercent?: number;
  notes?: string;
  params: PricingParams;
  terms: DocTerms;
  software: { softwareId: number; assetsCovered?: number | null }[];
  services: { serviceId: number; inScope: boolean; assetsPerDay: number }[];
}

export interface SoftwareLineDto {
  softwareId: number;
  code: string;
  name: string;
  assetsCovered: number;
  price: number;
  implementationCharge: number;
}

export interface ServiceLineDto {
  serviceId: number;
  code: string;
  name: string;
  inScope: boolean;
  assetsPerDay: number;
  effortDays: number;
  roomCost: number;
  foodCost: number;
  salaryCost: number;
  totalCost: number;
  workingDays: number;
}

export interface Totals {
  roomTotal: number;
  foodTotal: number;
  salaryTotal: number;
  branchCost: number;
  travelCost: number;
  srExecCost: number;
  servicesSubtotal: number;
  overheadAmount: number;
  profitAmount: number;
  servicesPrice: number;
  ratePerAsset: number;
  softwareTotal: number;
  grossTotal: number;
  discountAmount: number;
  netTotal: number;
  gstAmount: number;
  grandTotal: number;
}

export interface ProposalDto {
  id?: number;
  refNo?: string;
  version: number;
  proposalDate: string;
  status?: ProposalStatus;
  customerId: number;
  customerName: string;
  contactName?: string;
  assetCount: number;
  branchCount: number;
  srExecDays: number;
  discountPercent: number;
  notes?: string;
  params: PricingParams;
  terms: DocTerms;
  software: SoftwareLineDto[];
  services: ServiceLineDto[];
  totals: Totals;
  poNo?: string;
  poDate?: string;
}

export interface ProposalSummary {
  id: number;
  refNo: string;
  proposalDate: string;
  customerId: number;
  customerName: string;
  status: ProposalStatus;
  grandTotal: number;
  poNo?: string;
}

export interface DashboardDto {
  customers: number;
  software: number;
  services: number;
  proposals: number;
  poGenerated: number;
  totalValue: number;
  poValue: number;
  byStatus: { status: string; count: number; amount: number }[];
  monthly: { month: string; count: number; amount: number }[];
  topCustomers: { customer: string; count: number; amount: number }[];
  recent: ProposalSummary[];
}

export interface Numbering {
  proposalPrefix: string;
  poPrefix: string;
  includeFinancialYear: boolean;
  resetEachYear: boolean;
  numberWidth: number;
  seriesStartNo: number;
  nextProposalNo: number;
  nextPoNo: number;
  currentFinancialYear?: string;
  proposalExample?: string;
  poExample?: string;
  maxNumber?: number;
}
