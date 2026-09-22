import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { forkJoin } from 'rxjs';
import { ApiService, errorMessage } from '../../core/api.service';
import { Driver, Vehicle } from '../../core/models';
import { nowForInput } from '../../core/format';

@Component({
  selector: 'app-assign-vehicle',
  imports: [FormsModule],
  template: `
    <h1>Assign vehicle</h1>
    <section class="card">
      <p class="muted">The request is sent to the driver, who can accept or reject it from the driver portal.</p>
      <form #f="ngForm" (ngSubmit)="submit()" class="grid-form">
        <label>Driver
          <select name="driverId" [(ngModel)]="driverId" required>
            @for (d of drivers(); track d.id) { <option [ngValue]="d.id">{{ d.name }} ({{ d.phone }})</option> }
          </select>
        </label>
        <label>Vehicle
          <select name="vehicleId" [(ngModel)]="vehicleId" required>
            @for (v of vehicles(); track v.id) { <option [ngValue]="v.id">{{ v.makeModel }} – {{ v.licensePlate }}</option> }
          </select>
        </label>
        <label>Start <input name="start" type="datetime-local" [(ngModel)]="start" required /></label>
        <label>End <input name="end" type="datetime-local" [(ngModel)]="end" required /></label>
        <button type="submit" [disabled]="f.invalid">Send assignment request</button>
      </form>
      @if (message()) { <p class="alert" [class.error]="isError()">{{ message() }}</p> }
    </section>
  `,
})
export class AssignVehiclePage {
  private readonly api = inject(ApiService);
  protected readonly drivers = signal<Driver[]>([]);
  protected readonly vehicles = signal<Vehicle[]>([]);
  protected readonly message = signal('');
  protected readonly isError = signal(false);
  protected driverId: number | null = null;
  protected vehicleId: number | null = null;
  protected start = nowForInput(1);
  protected end = nowForInput(9);

  constructor() {
    forkJoin([this.api.drivers(), this.api.vehicles()]).subscribe({
      next: ([d, v]) => {
        this.drivers.set(d);
        this.vehicles.set(v);
      },
      error: (e) => this.fail(e),
    });
  }

  submit(): void {
    if (this.driverId == null || this.vehicleId == null) return;
    this.api
      .createAssignment({ driverId: this.driverId, vehicleId: this.vehicleId, startTime: this.start, endTime: this.end })
      .subscribe({
        next: (a) => {
          this.isError.set(false);
          this.message.set(`Request sent to ${a.driverName} for ${a.vehicleMakeModel} (${a.licensePlate}).`);
        },
        error: (e) => this.fail(e),
      });
  }

  private fail(e: unknown): void {
    this.isError.set(true);
    this.message.set(errorMessage(e));
  }
}
