import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { environment } from '../../environments/environment';
import { Checklist } from '../checklist/checklist';
import { Family } from './family';
import { SchoolInfo, TimeAvailabilityLabels } from '../settings/settings';

export interface FamilyPortalSummary {
  profileComplete: boolean;
  family: Family | null;
  driveDay?: {
    date: string;
    message?: string;
  };
  timeSlot?: string;
  timeSlotStatus?: 'pending' | 'assigned';
  schools?: SchoolInfo[];
  timeAvailability?: TimeAvailabilityLabels;
}

export interface FamilyPortalDriveDay {
  driveDay?: {
    date: string;
    message?: string;
  };
  timeSlot?: string;
  timeSlotStatus?: 'pending' | 'assigned';
}

@Injectable({
  providedIn: 'root'
})
export class FamilyPortalService {
  private httpClient = inject(HttpClient);

  private readonly familyPortalUrl = `${environment.apiUrl}family-portal`;

  getSummary(): Observable<FamilyPortalSummary> {
    return this.httpClient.get<FamilyPortalSummary>(this.familyPortalUrl);
  }

  upsertForm(family: Partial<Family>): Observable<{ profileComplete: boolean }> {
    return this.httpClient.put<{ profileComplete: boolean }>(`${this.familyPortalUrl}/form`, family);
  }

  getChecklist(): Observable<Checklist[]> {
    return this.httpClient.get<Checklist[]>(`${this.familyPortalUrl}/checklist`);
  }

  getDriveDay(): Observable<FamilyPortalDriveDay> {
    return this.httpClient.get<FamilyPortalDriveDay>(`${this.familyPortalUrl}/drive-day`);
  }
}
