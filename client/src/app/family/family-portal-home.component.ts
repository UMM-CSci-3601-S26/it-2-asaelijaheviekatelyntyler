import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { Router, RouterLink } from '@angular/router';

import { Checklist } from '../checklist/checklist';
import { FamilyPortalService, FamilyPortalSummary } from './family-portal.service';

@Component({
  selector: 'app-family-portal-home',
  templateUrl: './family-portal-home.component.html',
  styleUrls: ['./family-portal-home.component.scss'],
  imports: [
    CommonModule,
    RouterLink,
    MatCardModule,
    MatButtonModule,
    MatIconModule,
  ]
})
export class FamilyPortalHomeComponent implements OnInit {
  private familyPortalService = inject(FamilyPortalService);
  private router = inject(Router);

  summary: FamilyPortalSummary | null = null;
  checklists: Checklist[] = [];
  isLoading = true;
  error: string | null = null;

  ngOnInit(): void {
    this.loadPortalData();
  }

  private loadPortalData() {
    this.familyPortalService.getSummary().subscribe({
      next: summary => {
        this.summary = summary;
        if (!summary.profileComplete) {
          this.router.navigate(['/family-portal/form']);
          return;
        }

        this.familyPortalService.getChecklist().subscribe({
          next: checklists => {
            this.checklists = checklists;
            this.isLoading = false;
          },
          error: () => {
            this.error = 'Unable to load your checklist right now.';
            this.isLoading = false;
          }
        });
      },
      error: () => {
        this.error = 'Unable to load your family portal data right now.';
        this.isLoading = false;
      }
    });
  }
}
