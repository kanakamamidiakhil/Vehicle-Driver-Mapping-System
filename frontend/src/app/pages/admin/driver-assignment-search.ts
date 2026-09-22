import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ApiService, errorMessage } from '../../core/api.service';
import { Assignment, AssignmentStatus, SearchType } from '../../core/models';
import { AssignmentTable } from './assignment-table';

@Component({
  selector: 'app-driver-assignment-search',
  imports: [FormsModule, AssignmentTable],
  template: `
    <h1>Search driver assignments</h1>
    <section class="card">
      <form (ngSubmit)="search()" class="toolbar">
        <select name="type" [(ngModel)]="type">
          <option value="NAME">By name</option><option value="PHONE">By phone</option>
        </select>
        <input name="term" [(ngModel)]="term" required placeholder="Driver name or phone" />
        <select name="status" [(ngModel)]="status">
          <option value="ACCEPTED">Accepted</option><option value="SENT">Sent</option><option value="REJECTED">Rejected</option>
        </select>
        <button type="submit">Search</button>
      </form>
      @if (error()) { <p class="alert error">{{ error() }}</p> }
      @if (searched()) {
        <app-assignment-table [assignments]="results()" actionLabel="Unassign" (action)="unassign($event)"
                              emptyText="No matching assignments." />
      }
    </section>
  `,
})
export class DriverAssignmentSearchPage {
  private readonly api = inject(ApiService);
  protected readonly results = signal<Assignment[]>([]);
  protected readonly searched = signal(false);
  protected readonly error = signal('');
  protected type: SearchType = 'NAME';
  protected term = '';
  protected status: AssignmentStatus = 'ACCEPTED';

  search(): void {
    this.api.searchAssignments(this.type, this.term, this.status).subscribe({
      next: (a) => {
        this.error.set('');
        this.results.set(a);
        this.searched.set(true);
      },
      error: (e) => this.error.set(errorMessage(e)),
    });
  }

  unassign(a: Assignment): void {
    if (!confirm(`Unassign ${a.driverName} from ${a.vehicleMakeModel} (${a.licensePlate})?`)) return;
    this.api.unassign(a.id).subscribe({ next: () => this.search(), error: (e) => this.error.set(errorMessage(e)) });
  }
}
