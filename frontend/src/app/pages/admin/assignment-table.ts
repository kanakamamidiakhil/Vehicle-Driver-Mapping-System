import { Component, input, output } from '@angular/core';
import { DatePipe } from '@angular/common';
import { Assignment } from '../../core/models';

/** Reusable assignments table; the optional action column is driven by the parent. */
@Component({
  selector: 'app-assignment-table',
  imports: [DatePipe],
  template: `
    <table>
      <thead>
        <tr>
          <th>Driver</th><th>Phone</th><th>Vehicle</th><th>Plate</th><th>Start</th><th>End</th><th>Status</th>
          @if (actionLabel()) { <th></th> }
        </tr>
      </thead>
      <tbody>
        @for (a of assignments(); track a.id) {
          <tr>
            <td>{{ a.driverName }}</td><td>{{ a.driverPhone }}</td><td>{{ a.vehicleMakeModel }}</td>
            <td class="plate">{{ a.licensePlate }}</td>
            <td>{{ a.startTime | date: 'medium' }}</td><td>{{ a.endTime | date: 'medium' }}</td>
            <td><span class="badge" [class]="a.status">{{ a.status }}</span></td>
            @if (actionLabel()) {
              <td><button class="danger small" (click)="action.emit(a)">{{ actionLabel() }}</button></td>
            }
          </tr>
        } @empty {
          <tr><td colspan="8" class="muted">{{ emptyText() }}</td></tr>
        }
      </tbody>
    </table>
  `,
})
export class AssignmentTable {
  readonly assignments = input.required<Assignment[]>();
  readonly actionLabel = input<string>('');
  readonly emptyText = input<string>('No assignments.');
  readonly action = output<Assignment>();
}
