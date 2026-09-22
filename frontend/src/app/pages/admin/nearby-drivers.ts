import { Component, inject, signal } from '@angular/core';
import { DecimalPipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ApiService, errorMessage } from '../../core/api.service';
import { NearbyDriver } from '../../core/models';
import { nowForInput } from '../../core/format';

@Component({
  selector: 'app-nearby-drivers',
  imports: [FormsModule, DecimalPipe],
  template: `
    <h1>Nearby drivers</h1>
    <section class="card">
      <p class="muted">Drivers within range of a point who are on an accepted assignment at the chosen time, nearest first.</p>
      <form #f="ngForm" (ngSubmit)="search()" class="grid-form">
        <label>X <input name="x" type="number" step="any" [(ngModel)]="x" required /></label>
        <label>Y <input name="y" type="number" step="any" [(ngModel)]="y" required /></label>
        <label>Range <input name="range" type="number" step="any" min="0" [(ngModel)]="range" required /></label>
        <label>Date &amp; time <input name="at" type="datetime-local" [(ngModel)]="at" required /></label>
        <button type="submit" [disabled]="f.invalid">Search</button>
      </form>
      @if (error()) { <p class="alert error">{{ error() }}</p> }
      @if (searched()) {
        <table>
          <thead><tr><th>Name</th><th>Phone</th><th>Location</th><th>Distance</th><th>Vehicle</th></tr></thead>
          <tbody>
            @for (n of results(); track n.driver.id) {
              <tr>
                <td>{{ n.driver.name }}</td><td>{{ n.driver.phone }}</td>
                <td>{{ n.driver.locationX }}, {{ n.driver.locationY }}</td>
                <td>{{ n.distance | number: '1.0-2' }}</td>
                <td>{{ n.activeAssignment.vehicleMakeModel }} <span class="plate">{{ n.activeAssignment.licensePlate }}</span></td>
              </tr>
            } @empty {
              <tr><td colspan="5" class="muted">No on-duty drivers in range.</td></tr>
            }
          </tbody>
        </table>
      }
    </section>
  `,
})
export class NearbyDriversPage {
  private readonly api = inject(ApiService);
  protected readonly results = signal<NearbyDriver[]>([]);
  protected readonly searched = signal(false);
  protected readonly error = signal('');
  protected x = 0;
  protected y = 0;
  protected range = 10;
  protected at = nowForInput();

  search(): void {
    this.api.nearbyDrivers(this.x, this.y, this.range, this.at).subscribe({
      next: (r) => {
        this.error.set('');
        this.results.set(r);
        this.searched.set(true);
      },
      error: (e) => this.error.set(errorMessage(e)),
    });
  }
}
