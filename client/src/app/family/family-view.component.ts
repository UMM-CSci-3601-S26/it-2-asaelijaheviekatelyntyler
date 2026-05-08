// Angular imports
import { Component, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatOptionModule } from '@angular/material/core';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatListModule } from '@angular/material/list';
import { MatRadioModule } from '@angular/material/radio';
import { MatSelectModule } from '@angular/material/select';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatDialog, MatDialogModule } from '@angular/material/dialog';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { RouterLink } from '@angular/router';
import { toSignal } from '@angular/core/rxjs-interop';

// RxJS imports
import { catchError, of } from 'rxjs';

// Family Component and Service Import
import { Family } from './family';
import { FamilyCardComponent } from './family-card.component';
import { FamilyService } from './family.service';
import { AuthService } from '../auth/auth-service';
import { DeleteFamilyRequestDialogComponent, DeleteFamilyRequestDialogResult } from './delete-family-request-dialog.component';

@Component({
  selector: 'app-family',
  templateUrl: './family-view.component.html',
  styleUrls: ['./family-view.component.scss'],
  providers: [],
  imports: [
    MatCardModule,
    MatFormFieldModule,
    MatInputModule,
    FormsModule,
    MatSelectModule,
    MatOptionModule,
    MatRadioModule,
    FamilyCardComponent,
    MatListModule,
    RouterLink,
    MatButtonModule,
    MatTooltipModule,
    MatIconModule,
    MatDialogModule,
    MatSnackBarModule,
    FamilyCardComponent
  ],
})

/**
 * Component for displaying the list of families and their requested supplies.
 * - Fetches family data from the FamilyService and handles errors gracefully.
 * - Provides a method to download the family data as a CSV file.
 * - Uses Angular Material components for styling and layout.
 */
export class FamilyViewComponent {
  // Inject the FamilyService to fetch family data and handle CSV export functionality
  private familyService = inject(FamilyService);
  private authService = inject(AuthService);
  private dialog = inject(MatDialog);
  private snackBar = inject(MatSnackBar);

  get canExportFamilies(): boolean {
    return this.authService.hasPermission('export_families_csv');
  }

  get canAddFamily(): boolean {
    return this.authService.hasPermission('add_family');
  }

  get canRequestFamilyDelete(): boolean {
    return this.authService.hasPermission('request_family_delete');
  }

  // Create a signal to hold the list of families, fetching data from the FamilyService and handling errors by returning an empty array
  families = toSignal<Family[]>(
    this.familyService.getFamilies().pipe(
      catchError(() => of([]))
    )
  );

  linkStatusFilter: 'all' | 'linked' | 'manual' = 'all';

  get filteredFamilies(): Family[] {
    const loadedFamilies = this.families() ?? [];
    if (this.linkStatusFilter === 'all') {
      return loadedFamilies;
    }

    return loadedFamilies.filter(family => {
      const hasLinkedGuardian = !!family.ownerUserId?.trim();
      return this.linkStatusFilter === 'linked' ? hasLinkedGuardian : !hasLinkedGuardian;
    });
  }

  clearFamilyFilters() {
    this.linkStatusFilter = 'all';
  }

  /**
   * Method to download the family data as a CSV file. It calls the exportFamilies() method from the FamilyService, creates a Blob from the CSV data, and triggers a download in the browser.
   * Handles the CSV export functionality by creating a temporary anchor element and simulating a click to download the file, then revokes the object URL to free up memory.
   */
  downloadCSV() {
    this.familyService.exportFamilies().subscribe(csvData => {
      const blob = new Blob([csvData], { type: 'text/csv' });
      const url = window.URL.createObjectURL(blob);

      const a = document.createElement('a');
      a.href = url;
      a.download = 'families.csv';
      a.click();

      window.URL.revokeObjectURL(url);
    });
  }

  submitDeleteRequest(family: Family) {
    if (!this.canRequestFamilyDelete || !family._id) {
      return;
    }

    const dialogRef = this.dialog.open(DeleteFamilyRequestDialogComponent, {
      width: '520px',
      data: { guardianName: family.guardianName }
    });

    dialogRef.afterClosed().subscribe((result: DeleteFamilyRequestDialogResult | undefined) => {
      if (!result?.message?.trim()) {
        return;
      }

      this.familyService.requestFamilyDelete(family._id!, result.message.trim()).subscribe({
        next: () => {
          if (!family.deleteRequest) {
            family.deleteRequest = { requested: true };
          }
          family.deleteRequest.requested = true;
          family.deleteRequest.message = result.message.trim();
          this.snackBar.open('Delete request submitted for admin review.', 'Close', { duration: 2500 });
        },
        error: error => {
          this.snackBar.open(
            error.error?.message || 'Unable to submit delete request right now. Please try again.',
            'Close',
            { duration: 3500 }
          );
        }
      });
    });
  }
}
