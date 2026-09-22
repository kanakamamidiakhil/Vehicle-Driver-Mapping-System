import { Injectable, computed, signal } from '@angular/core';
import { Driver } from './models';

const KEY = 'vdms.driver';

/** Remembers the logged-in driver for the driver portal. */
@Injectable({ providedIn: 'root' })
export class DriverSession {
  private readonly current = signal<Driver | null>(this.restore());
  readonly driver = this.current.asReadonly();
  readonly loggedIn = computed(() => this.current() !== null);

  login(driver: Driver): void {
    this.current.set(driver);
    try {
      sessionStorage.setItem(KEY, JSON.stringify(driver));
    } catch {
      /* storage unavailable: session lasts for this page only */
    }
  }

  logout(): void {
    this.current.set(null);
    try {
      sessionStorage.removeItem(KEY);
    } catch {
      /* ignore */
    }
  }

  private restore(): Driver | null {
    try {
      const raw = sessionStorage.getItem(KEY);
      return raw ? (JSON.parse(raw) as Driver) : null;
    } catch {
      return null;
    }
  }
}
