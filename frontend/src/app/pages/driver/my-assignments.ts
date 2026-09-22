import { Component, inject, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { ApiService, errorMessage } from '../../core/api.service';
import { DriverSession } from '../../core/driver-session.service';
import { Assignment } from '../../core/models';

@Component({
  selector: 'app-my-assignments',
  imports: [DatePipe],
  template: `
    <h1>Assignments for {{ session.driver()?.name }}</h1>
    @if (error()) { <p class="alert error">{{ error() }}</p> }
    <section class="card">
      <table>
        <thead><tr><th>Vehicle</th><th>Plate</th><th>Start</th><th>End</th><th>Status</th><th>Action</th></tr></thead>
        <tbody>
          @for (a of assignments(); track a.id) {
            <tr>
              <td>{{ a.vehicleMakeModel }}</td><td class="plate">{{ a.licensePlate }}</td>
              <td>{{ a.startTime | date: 'medium' }}</td><td>{{ a.endTime | date: 'medium' }}</td>
              <td><span class="badge" [class]="a.status">{{ a.status }}</span></td>
              <td>
                @if (a.status === 'SENT') {
                  <button class="small" (click)="accept(a)">Accept</button>
                  <button class="small danger" (click)="reject(a)">Reject</button>
                }
              </td>
            </tr>
          } @empty {
            <tr><td colspan="6" class="muted">No pending or accepted assignments.</td></tr>
          }
        </tbody>
      </table>
    </section>
  `,
})
export class MyAssignmentsPage {
  private readonly api = inject(ApiService);
  protected readonly session = inject(DriverSession);
  protected readonly assignments = signal<Assignment[]>([]);
  protected readonly error = signal('');

  constructor() {
    this.load();
  }

  load(): void {
    const d = this.session.driver();
    if (!d) return;
    this.api.driverAssignments(d.id, ['SENT', 'ACCEPTED']).subscribe({
      next: (a) => this.assignments.set(a),
      error: (e) => this.error.set(errorMessage(e)),
    });
  }

  accept(a: Assignment): void {
    this.api.accept(a.id).subscribe({ next: () => this.done(), error: (e) => this.error.set(errorMessage(e)) });
  }

  reject(a: Assignment): void {
    this.api.reject(a.id).subscribe({ next: () => this.done(), error: (e) => this.error.set(errorMessage(e)) });
  }

  private done(): void {
    this.error.set('');
    this.load();
  }
}
