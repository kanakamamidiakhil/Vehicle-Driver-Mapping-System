import { HttpClient, HttpErrorResponse, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import {
  AiStatus, AskResponse, Assignment, AssignmentRequest, AssignmentStatus, ChatTurn,
  Driver, DriverRequest, NearbyDriver, SearchType, Vehicle,
} from './models';

/** Thin typed wrapper over the Spring Boot REST API (served under /api). */
@Injectable({ providedIn: 'root' })
export class ApiService {
  private readonly http = inject(HttpClient);

  // Drivers
  drivers(): Observable<Driver[]> {
    return this.http.get<Driver[]>('/api/drivers');
  }
  createDriver(body: DriverRequest): Observable<Driver> {
    return this.http.post<Driver>('/api/drivers', body);
  }
  deleteDriver(id: number): Observable<void> {
    return this.http.delete<void>(`/api/drivers/${id}`);
  }
  searchDrivers(type: SearchType, term: string): Observable<Driver[]> {
    return this.http.get<Driver[]>('/api/drivers/search', { params: { type, term } });
  }
  nearbyDrivers(x: number, y: number, range: number, at: string): Observable<NearbyDriver[]> {
    return this.http.get<NearbyDriver[]>('/api/drivers/nearby', { params: { x, y, range, at } });
  }
  driverAssignments(driverId: number, statuses: AssignmentStatus[] = []): Observable<Assignment[]> {
    let params = new HttpParams();
    statuses.forEach((s) => (params = params.append('status', s)));
    return this.http.get<Assignment[]>(`/api/drivers/${driverId}/assignments`, { params });
  }
  login(email: string, password: string): Observable<Driver> {
    return this.http.post<Driver>('/api/auth/driver/login', { email, password });
  }

  // Vehicles
  vehicles(): Observable<Vehicle[]> {
    return this.http.get<Vehicle[]>('/api/vehicles');
  }
  createVehicle(body: Omit<Vehicle, 'id'>): Observable<Vehicle> {
    return this.http.post<Vehicle>('/api/vehicles', body);
  }
  deleteVehicle(id: number): Observable<void> {
    return this.http.delete<void>(`/api/vehicles/${id}`);
  }

  // Assignments
  assignments(): Observable<Assignment[]> {
    return this.http.get<Assignment[]>('/api/assignments');
  }
  createAssignment(body: AssignmentRequest): Observable<Assignment> {
    return this.http.post<Assignment>('/api/assignments', body);
  }
  searchAssignments(type: SearchType, term: string, status: AssignmentStatus): Observable<Assignment[]> {
    return this.http.get<Assignment[]>('/api/assignments/search', { params: { type, term, status } });
  }
  accept(id: number): Observable<Assignment> {
    return this.http.post<Assignment>(`/api/assignments/${id}/accept`, {});
  }
  reject(id: number): Observable<Assignment> {
    return this.http.post<Assignment>(`/api/assignments/${id}/reject`, {});
  }
  unassign(id: number): Observable<void> {
    return this.http.delete<void>(`/api/assignments/${id}`);
  }

  // AI assistant
  ask(question: string, history: ChatTurn[]): Observable<AskResponse> {
    return this.http.post<AskResponse>('/api/ai/ask', { question, history });
  }
  aiStatus(): Observable<AiStatus> {
    return this.http.get<AiStatus>('/api/ai/status');
  }
}

/** Turns an HTTP error into a message suitable for display. */
export function errorMessage(err: unknown): string {
  if (err instanceof HttpErrorResponse) {
    if (err.status === 0) return 'Cannot reach the server. Is the backend running on port 8080?';
    const body = err.error as { message?: string; details?: string[] } | null;
    if (body?.message) {
      return body.details?.length ? `${body.message}: ${body.details.join('; ')}` : body.message;
    }
    return `${err.status} ${err.statusText}`;
  }
  return String(err);
}
