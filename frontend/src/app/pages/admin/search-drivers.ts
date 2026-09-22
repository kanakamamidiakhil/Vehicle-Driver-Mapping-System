import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ApiService, errorMessage } from '../../core/api.service';
import { Driver, SearchType } from '../../core/models';

@Component({
  selector: 'app-search-drivers',
  imports: [FormsModule],
  template: `
    <h1>Search drivers</h1>
    <section class="card">
      <form (ngSubmit)="search()" class="toolbar">
        <select name="type" [(ngModel)]="type">
          <option value="NAME">By name</option><option value="PHONE">By phone</option>
        </select>
        <input name="term" [(ngModel)]="term" required placeholder="Search…" />
        <button type="submit">Search</button>
      </form>
      @if (error()) { <p class="alert error">{{ error() }}</p> }
      @if (searched()) {
        <table>
          <thead><tr><th>Name</th><th>Email</th><th>Phone</th><th>Location</th></tr></thead>
          <tbody>
            @for (d of results(); track d.id) {
              <tr><td>{{ d.name }}</td><td>{{ d.email }}</td><td>{{ d.phone }}</td>
                <td>{{ d.locationX ?? '–' }}, {{ d.locationY ?? '–' }}</td></tr>
            } @empty {
              <tr><td colspan="4" class="muted">No drivers found.</td></tr>
            }
          </tbody>
        </table>
      }
    </section>
  `,
})
export class SearchDriversPage {
  private readonly api = inject(ApiService);
  protected readonly results = signal<Driver[]>([]);
  protected readonly searched = signal(false);
  protected readonly error = signal('');
  protected type: SearchType = 'NAME';
  protected term = '';

  search(): void {
    this.api.searchDrivers(this.type, this.term).subscribe({
      next: (d) => {
        this.error.set('');
        this.results.set(d);
        this.searched.set(true);
      },
      error: (e) => this.error.set(errorMessage(e)),
    });
  }
}
