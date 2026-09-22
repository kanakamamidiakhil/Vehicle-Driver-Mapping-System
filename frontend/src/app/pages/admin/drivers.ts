import { Component, inject, signal } from '@angular/core';
import { FormsModule, NgForm } from '@angular/forms';
import { ApiService, errorMessage } from '../../core/api.service';
import { Driver, DriverRequest } from '../../core/models';

@Component({
  selector: 'app-drivers',
  imports: [FormsModule],
  template: `
    <h1>Drivers</h1>
    <section class="card">
      <h2>Add driver</h2>
      <form #f="ngForm" (ngSubmit)="create(f)" class="grid-form">
        <label>Name <input name="name" [(ngModel)]="form.name" required /></label>
        <label>Email <input name="email" type="email" [(ngModel)]="form.email" required email /></label>
        <label>Phone <input name="phone" [(ngModel)]="form.phone" required pattern="\\+?[0-9]{7,14}" /></label>
        <label>Password <input name="password" type="password" [(ngModel)]="form.password" required minlength="4" /></label>
        <label>Location X <input name="locationX" type="number" step="any" [(ngModel)]="form.locationX" /></label>
        <label>Location Y <input name="locationY" type="number" step="any" [(ngModel)]="form.locationY" /></label>
        <button type="submit" [disabled]="f.invalid">Add driver</button>
      </form>
      @if (message()) { <p class="alert" [class.error]="isError()">{{ message() }}</p> }
    </section>

    <section class="card">
      <h2>All drivers ({{ drivers().length }})</h2>
      <table>
        <thead><tr><th>Name</th><th>Email</th><th>Phone</th><th>Location</th><th></th></tr></thead>
        <tbody>
          @for (d of drivers(); track d.id) {
            <tr>
              <td>{{ d.name }}</td><td>{{ d.email }}</td><td>{{ d.phone }}</td>
              <td>{{ d.locationX ?? '–' }}, {{ d.locationY ?? '–' }}</td>
              <td><button class="danger small" (click)="remove(d)">Delete</button></td>
            </tr>
          } @empty {
            <tr><td colspan="5" class="muted">No drivers yet.</td></tr>
          }
        </tbody>
      </table>
    </section>
  `,
})
export class DriversPage {
  private readonly api = inject(ApiService);
  protected readonly drivers = signal<Driver[]>([]);
  protected readonly message = signal('');
  protected readonly isError = signal(false);
  protected form: DriverRequest = blank();

  constructor() {
    this.load();
  }

  load(): void {
    this.api.drivers().subscribe({ next: (d) => this.drivers.set(d), error: (e) => this.fail(e) });
  }

  create(f: NgForm): void {
    this.api.createDriver(this.form).subscribe({
      next: (d) => {
        this.ok(`Driver ${d.name} added.`);
        f.resetForm();
        this.form = blank();
        this.load();
      },
      error: (e) => this.fail(e),
    });
  }

  remove(d: Driver): void {
    if (!confirm(`Delete ${d.name} and all their assignments?`)) return;
    this.api.deleteDriver(d.id).subscribe({ next: () => this.load(), error: (e) => this.fail(e) });
  }

  private ok(msg: string): void {
    this.isError.set(false);
    this.message.set(msg);
  }

  private fail(e: unknown): void {
    this.isError.set(true);
    this.message.set(errorMessage(e));
  }
}

function blank(): DriverRequest {
  return { name: '', email: '', phone: '', password: '', locationX: null, locationY: null };
}
