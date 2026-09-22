import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { ApiService, errorMessage } from '../../core/api.service';
import { DriverSession } from '../../core/driver-session.service';

@Component({
  selector: 'app-driver-login',
  imports: [FormsModule, RouterLink],
  template: `
    <section class="card narrow">
      <h1>Driver login</h1>
      <form #f="ngForm" (ngSubmit)="login()" class="stack-form">
        <label>Email <input name="email" type="email" [(ngModel)]="email" required email autocomplete="username" /></label>
        <label>Password <input name="password" type="password" [(ngModel)]="password" required autocomplete="current-password" /></label>
        <button type="submit" [disabled]="f.invalid">Log in</button>
      </form>
      @if (error()) { <p class="alert error">{{ error() }}</p> }
      <p class="muted">New driver? <a routerLink="/driver/register">Register here</a>.
        Demo accounts: ravi&#64;fleet.com / driver123.</p>
    </section>
  `,
})
export class DriverLoginPage {
  private readonly api = inject(ApiService);
  private readonly session = inject(DriverSession);
  private readonly router = inject(Router);
  protected readonly error = signal('');
  protected email = '';
  protected password = '';

  login(): void {
    this.api.login(this.email, this.password).subscribe({
      next: (d) => {
        this.session.login(d);
        this.router.navigateByUrl('/driver/assignments');
      },
      error: (e) => this.error.set(errorMessage(e)),
    });
  }
}
