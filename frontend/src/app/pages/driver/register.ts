import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { ApiService, errorMessage } from '../../core/api.service';
import { DriverRequest } from '../../core/models';

@Component({
  selector: 'app-driver-register',
  imports: [FormsModule],
  template: `
    <section class="card narrow">
      <h1>Driver registration</h1>
      <form #f="ngForm" (ngSubmit)="register()" class="stack-form">
        <label>Name <input name="name" [(ngModel)]="form.name" required /></label>
        <label>Email <input name="email" type="email" [(ngModel)]="form.email" required email /></label>
        <label>Phone <input name="phone" [(ngModel)]="form.phone" required pattern="\\+?[0-9]{7,14}" /></label>
        <label>Password <input name="password" type="password" [(ngModel)]="form.password" required minlength="4" /></label>
        <div class="row">
          <label>Location X <input name="locationX" type="number" step="any" [(ngModel)]="form.locationX" /></label>
          <label>Location Y <input name="locationY" type="number" step="any" [(ngModel)]="form.locationY" /></label>
        </div>
        <button type="submit" [disabled]="f.invalid">Register</button>
      </form>
      @if (error()) { <p class="alert error">{{ error() }}</p> }
    </section>
  `,
})
export class DriverRegisterPage {
  private readonly api = inject(ApiService);
  private readonly router = inject(Router);
  protected readonly error = signal('');
  protected form: DriverRequest = { name: '', email: '', phone: '', password: '', locationX: null, locationY: null };

  register(): void {
    this.api.createDriver(this.form).subscribe({
      next: () => this.router.navigateByUrl('/driver/login'),
      error: (e) => this.error.set(errorMessage(e)),
    });
  }
}
