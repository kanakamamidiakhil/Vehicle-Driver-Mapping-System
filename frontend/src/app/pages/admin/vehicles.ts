import { Component, inject, signal } from '@angular/core';
import { FormsModule, NgForm } from '@angular/forms';
import { ApiService, errorMessage } from '../../core/api.service';
import { Vehicle } from '../../core/models';

@Component({
  selector: 'app-vehicles',
  imports: [FormsModule],
  template: `
    <h1>Vehicles</h1>
    <section class="card">
      <h2>Add vehicle</h2>
      <form #f="ngForm" (ngSubmit)="create(f)" class="grid-form">
        <label>Make &amp; model <input name="makeModel" [(ngModel)]="makeModel" required placeholder="Maruti Suzuki Alto" /></label>
        <label>License plate <input name="licensePlate" [(ngModel)]="licensePlate" required maxlength="20" placeholder="TS 09 AB 1234" /></label>
        <button type="submit" [disabled]="f.invalid">Add vehicle</button>
      </form>
      @if (message()) { <p class="alert" [class.error]="isError()">{{ message() }}</p> }
    </section>

    <section class="card">
      <h2>All vehicles ({{ vehicles().length }})</h2>
      <table>
        <thead><tr><th>Make &amp; model</th><th>License plate</th><th></th></tr></thead>
        <tbody>
          @for (v of vehicles(); track v.id) {
            <tr>
              <td>{{ v.makeModel }}</td><td class="plate">{{ v.licensePlate }}</td>
              <td><button class="danger small" (click)="remove(v)">Delete</button></td>
            </tr>
          } @empty {
            <tr><td colspan="3" class="muted">No vehicles yet.</td></tr>
          }
        </tbody>
      </table>
    </section>
  `,
})
export class VehiclesPage {
  private readonly api = inject(ApiService);
  protected readonly vehicles = signal<Vehicle[]>([]);
  protected readonly message = signal('');
  protected readonly isError = signal(false);
  protected makeModel = '';
  protected licensePlate = '';

  constructor() {
    this.load();
  }

  load(): void {
    this.api.vehicles().subscribe({ next: (v) => this.vehicles.set(v), error: (e) => this.fail(e) });
  }

  create(f: NgForm): void {
    this.api.createVehicle({ makeModel: this.makeModel, licensePlate: this.licensePlate }).subscribe({
      next: (v) => {
        this.isError.set(false);
        this.message.set(`${v.makeModel} (${v.licensePlate}) added.`);
        f.resetForm();
        this.load();
      },
      error: (e) => this.fail(e),
    });
  }

  remove(v: Vehicle): void {
    if (!confirm(`Delete ${v.makeModel} (${v.licensePlate}) and its assignments?`)) return;
    this.api.deleteVehicle(v.id).subscribe({ next: () => this.load(), error: (e) => this.fail(e) });
  }

  private fail(e: unknown): void {
    this.isError.set(true);
    this.message.set(errorMessage(e));
  }
}
