import { Component, inject } from '@angular/core';
import { Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { DriverSession } from './core/driver-session.service';

@Component({
  selector: 'app-root',
  imports: [RouterOutlet, RouterLink, RouterLinkActive],
  template: `
    <header class="topbar">
      <a routerLink="/" class="brand">🚗 Vehicle Driver Mapping</a>
      <nav>
        <span class="nav-group">Admin</span>
        <a routerLink="/admin/drivers" routerLinkActive="active">Drivers</a>
        <a routerLink="/admin/vehicles" routerLinkActive="active">Vehicles</a>
        <a routerLink="/admin/assignments" routerLinkActive="active">Assignments</a>
        <a routerLink="/admin/assign" routerLinkActive="active">Assign vehicle</a>
        <a routerLink="/admin/search-drivers" routerLinkActive="active">Search drivers</a>
        <a routerLink="/admin/driver-assignments" routerLinkActive="active">Driver assignments</a>
        <a routerLink="/admin/nearby" routerLinkActive="active">Nearby drivers</a>
        <span class="nav-group">Driver</span>
        @if (session.driver(); as d) {
          <a routerLink="/driver/assignments" routerLinkActive="active">My assignments</a>
          <button class="link" (click)="logout()">Log out {{ d.name }}</button>
        } @else {
          <a routerLink="/driver/login" routerLinkActive="active">Login</a>
          <a routerLink="/driver/register" routerLinkActive="active">Register</a>
        }
        <a routerLink="/assistant" routerLinkActive="active" class="ai-link">✨ Ask AI</a>
      </nav>
    </header>
    <main class="container">
      <router-outlet />
    </main>
  `,
})
export class App {
  protected readonly session = inject(DriverSession);
  private readonly router = inject(Router);

  logout(): void {
    this.session.logout();
    this.router.navigateByUrl('/driver/login');
  }
}
