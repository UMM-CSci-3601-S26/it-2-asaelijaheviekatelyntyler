// Angular and Material Imports
import { Component, inject, input, output } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { MatListModule } from '@angular/material/list';
import { CommonModule } from '@angular/common';

// Family Interface Import
import { Family } from './family';
import { AuthService } from '../auth/auth-service';

@Component({
  selector: 'app-family-card',
  templateUrl: './family-card.component.html',
  styleUrls: ['./family-card.component.scss'],
  imports: [
    MatCardModule,
    MatButtonModule,
    MatListModule,
    CommonModule,
    MatIconModule
  ]
})

// Component for displaying a card with family information and their requested supplies.
// The component takes a Family object as input and renders the details in a user-friendly format.
export class FamilyCardComponent {
  family = input.required<Family>();
  compact = input(false);
  requestDelete = output<void>();
  authService = inject(AuthService);
  get canRequestDelete(): boolean {
    return this.authService.hasPermission('request_family_delete');
  }

  get hasLinkedGuardianAccount(): boolean {
    return !!this.family().ownerUserId?.trim();
  }

  get guardianLinkStatusLabel(): string {
    return this.hasLinkedGuardianAccount ? 'Linked Guardian Account' : 'Manually Added (No Guardian Login)';
  }

  getAvailableTimes(): string {
    const a = this.family().timeAvailability;
    if (!a) {
      return 'None';
    }
    const times: string[] = [];
    if (a.earlyMorning) {
      times.push('Early Morning');
    }
    if (a.lateMorning) {
      times.push('Late Morning');
    }
    if (a.earlyAfternoon) {
      times.push('Early Afternoon');
    }
    if (a.lateAfternoon) {
      times.push('Late Afternoon');
    }
    return times.length ? times.join(', ') : 'None';
  }

  get hasPendingDeleteRequest(): boolean {
    return !!this.family().deleteRequest?.requested;
  }

  get studentNames(): string {
    const students = this.family().students ?? [];
    if (students.length === 0) {
      return 'No students listed';
    }
    return students.map(student => student.name).join(', ');
  }

  onRequestDelete(): void {
    this.requestDelete.emit();
  }
}
