import { Component, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ApiService, errorMessage } from '../../core/api.service';
import { Assignment } from '../../core/models';
import { AssignmentTable } from './assignment-table';

@Component({
  selector: 'app-assignments',
  imports: [FormsModule, AssignmentTable],
  template: `
    <h1>Assignments</h1>
    @if (error()) { <p class="alert error">{{ error() }}</p> }
    <section class="card">
      <div class="toolbar">
        <label>Status
          <select [ngModel]="filter()" (ngModelChange)="filter.set($event)">
            <option value="">All</option><option value="SENT">Sent</option>
            <option value="ACCEPTED">Accepted</option><option value="REJECTED">Rejected</option>
          </select>
        </label>
        <span class="muted">{{ visible().length }} shown</span>
      </div>
      <app-assignment-table [assignments]="visible()" />
    </section>
  `,
})
export class AssignmentsPage {
  private readonly api = inject(ApiService);
  protected readonly all = signal<Assignment[]>([]);
  protected readonly filter = signal('');
  protected readonly error = signal('');
  protected readonly visible = computed(() =>
    this.filter() ? this.all().filter((a) => a.status === this.filter()) : this.all(),
  );

  constructor() {
    this.api.assignments().subscribe({ next: (a) => this.all.set(a), error: (e) => this.error.set(errorMessage(e)) });
  }
}
