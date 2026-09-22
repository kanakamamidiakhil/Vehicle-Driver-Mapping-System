import { Component, computed, inject, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { RouterLink } from '@angular/router';
import { forkJoin } from 'rxjs';
import { ApiService, errorMessage } from '../core/api.service';
import { Assignment } from '../core/models';

@Component({
  selector: 'app-dashboard',
  imports: [RouterLink, DatePipe],
  template: `
    <h1>Fleet dashboard</h1>
    @if (error()) { <p class="alert error">{{ error() }}</p> }
    <section class="stats">
      <div class="stat"><span>{{ driverCount() }}</span>Drivers</div>
      <div class="stat"><span>{{ vehicleCount() }}</span>Vehicles</div>
      <div class="stat"><span>{{ onDuty().length }}</span>On duty now</div>
      <div class="stat"><span>{{ pending() }}</span>Pending requests</div>
    </section>

    <section class="card ai-teaser">
      <h2>✨ Ask the AI assistant</h2>
      <p>Ask in plain English, e.g. <em>"Who is driving the Alto TS 09 AB 1234?"</em> or
        <em>"What time is Priya assigned?"</em></p>
      <a routerLink="/assistant" class="button">Open assistant</a>
    </section>

    <section class="card">
      <h2>On duty right now</h2>
      @if (onDuty().length === 0) {
        <p class="muted">Nobody is on an accepted assignment at the moment.</p>
      } @else {
        <table>
          <thead><tr><th>Driver</th><th>Vehicle</th><th>Plate</th><th>Until</th></tr></thead>
          <tbody>
            @for (a of onDuty(); track a.id) {
              <tr>
                <td>{{ a.driverName }}</td><td>{{ a.vehicleMakeModel }}</td>
                <td class="plate">{{ a.licensePlate }}</td><td>{{ a.endTime | date: 'medium' }}</td>
              </tr>
            }
          </tbody>
        </table>
      }
    </section>
  `,
})
export class Dashboard {
  private readonly api = inject(ApiService);
  protected readonly driverCount = signal(0);
  protected readonly vehicleCount = signal(0);
  protected readonly assignments = signal<Assignment[]>([]);
  protected readonly error = signal('');

  protected readonly onDuty = computed(() => {
    const now = Date.now();
    return this.assignments().filter(
      (a) => a.status === 'ACCEPTED' && Date.parse(a.startTime) <= now && Date.parse(a.endTime) >= now,
    );
  });
  protected readonly pending = computed(() => this.assignments().filter((a) => a.status === 'SENT').length);

  constructor() {
    forkJoin([this.api.drivers(), this.api.vehicles(), this.api.assignments()]).subscribe({
      next: ([d, v, a]) => {
        this.driverCount.set(d.length);
        this.vehicleCount.set(v.length);
        this.assignments.set(a);
      },
      error: (e) => this.error.set(errorMessage(e)),
    });
  }
}
