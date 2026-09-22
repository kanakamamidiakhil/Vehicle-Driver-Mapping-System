export type AssignmentStatus = 'SENT' | 'ACCEPTED' | 'REJECTED';
export type SearchType = 'NAME' | 'PHONE';

export interface Driver {
  id: number;
  name: string;
  email: string;
  phone: string;
  locationX: number | null;
  locationY: number | null;
}

export interface DriverRequest {
  name: string;
  email: string;
  phone: string;
  password?: string;
  locationX: number | null;
  locationY: number | null;
}

export interface Vehicle {
  id: number;
  makeModel: string;
  licensePlate: string;
}

export interface Assignment {
  id: number;
  driverId: number;
  driverName: string;
  driverPhone: string;
  vehicleId: number;
  vehicleMakeModel: string;
  licensePlate: string;
  startTime: string;
  endTime: string;
  status: AssignmentStatus;
}

export interface AssignmentRequest {
  driverId: number;
  vehicleId: number;
  startTime: string;
  endTime: string;
}

export interface NearbyDriver {
  driver: Driver;
  distance: number;
  activeAssignment: Assignment;
}

export interface ChatTurn {
  role: 'user' | 'assistant';
  content: string;
}

export interface AskResponse {
  answer: string;
  mode: 'LLM' | 'RULE_BASED';
  model: string | null;
  toolCalls: { tool: string; arguments: string }[];
  notice: string | null;
}

export interface AiStatus {
  enabled: boolean;
  provider: string;
  model: string;
  baseUrl: string;
  reachable: boolean;
}

export interface ApiError {
  status: number;
  message: string;
  details: string[];
}
