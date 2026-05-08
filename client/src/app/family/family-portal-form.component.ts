import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormArray, FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { Router } from '@angular/router';

import { SchoolInfo, TimeAvailabilityLabels } from '../settings/settings';
import { FamilyPortalService } from './family-portal.service';

@Component({
  selector: 'app-family-portal-form',
  templateUrl: './family-portal-form.component.html',
  styleUrls: ['./family-portal-form.component.scss'],
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatCardModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatButtonModule,
    MatCheckboxModule,
    MatSnackBarModule,
  ]
})
export class FamilyPortalFormComponent implements OnInit {
  private familyPortalService = inject(FamilyPortalService);
  private router = inject(Router);
  private snackBar = inject(MatSnackBar);

  schools: SchoolInfo[] = [];
  isLoading = true;

  timeAvailabilityLabels: TimeAvailabilityLabels = {
    earlyMorning: '8:00-9:00 AM',
    lateMorning: '9:00-10:00 AM',
    earlyAfternoon: '12:00-1:00 PM',
    lateAfternoon: '1:00-2:00 PM',
  };

  readonly grades: string[] = [
    'Pre-K', 'K', '1', '2', '3', '4', '5',
    '6', '7', '8', '9', '10', '11', '12'
  ];

  familyForm = new FormGroup({
    guardianName: new FormControl('', [Validators.required]),
    email: new FormControl('', [Validators.required, Validators.email]),
    address: new FormControl('', [Validators.required]),
    timeSlot: new FormControl('to be assigned'),
    students: new FormArray([], [Validators.required]),
    timeAvailability: new FormGroup({
      earlyMorning: new FormControl(false),
      lateMorning: new FormControl(false),
      earlyAfternoon: new FormControl(false),
      lateAfternoon: new FormControl(false)
    })
  });

  get students(): FormArray {
    return this.familyForm.get('students') as FormArray;
  }

  ngOnInit(): void {
    this.familyPortalService.getSummary().subscribe({
      next: summary => {
        this.schools = summary.schools ?? [];
        if (summary.timeAvailability) {
          this.timeAvailabilityLabels = summary.timeAvailability;
        }

        if (summary.family) {
          this.patchFromFamily(summary.family);
        } else {
          this.addStudent();
        }
        this.isLoading = false;
      },
      error: () => {
        this.addStudent();
        this.isLoading = false;
      }
    });
  }

  addStudent() {
    this.students.push(new FormGroup({
      name: new FormControl('', [Validators.required]),
      grade: new FormControl('', [Validators.required]),
      school: new FormControl('', [Validators.required]),
      requestedSupplies: new FormControl('')
    }));
  }

  removeStudent(index: number) {
    this.students.removeAt(index);
  }

  saveForm() {
    if (!this.familyForm.valid) {
      this.snackBar.open('Please complete all required fields.', 'OK', { duration: 2500 });
      return;
    }

    const raw = this.familyForm.getRawValue();
    const payload = {
      guardianName: raw.guardianName ?? '',
      email: raw.email ?? '',
      address: raw.address ?? '',
      timeSlot: raw.timeSlot ?? 'to be assigned',
      students: (raw.students ?? []).map(student => ({
        name: student?.name ?? '',
        grade: student?.grade ?? '',
        school: student?.school ?? '',
        requestedSupplies: (student?.requestedSupplies ?? '')
          .split(',')
          .map(item => item.trim())
          .filter(item => item.length > 0)
      })),
      timeAvailability: raw.timeAvailability ?? {
        earlyMorning: false,
        lateMorning: false,
        earlyAfternoon: false,
        lateAfternoon: false,
      }
    };

    this.familyPortalService.upsertForm(payload).subscribe({
      next: () => {
        this.snackBar.open('Family form saved.', 'OK', { duration: 2000 });
        this.router.navigate(['/family-portal']);
      },
      error: (error) => {
        const message = error?.error?.message ?? 'Unable to save family form.';
        this.snackBar.open(message, 'OK', { duration: 3500 });
      }
    });
  }

  private patchFromFamily(family: {
    guardianName: string;
    email: string;
    address: string;
    timeSlot: string;
    students: Array<{ name: string; grade: string; school: string; requestedSupplies: string[] }>;
    timeAvailability: {
      earlyMorning: boolean;
      lateMorning: boolean;
      earlyAfternoon: boolean;
      lateAfternoon: boolean;
    };
  }) {
    this.familyForm.patchValue({
      guardianName: family.guardianName,
      email: family.email,
      address: family.address,
      timeSlot: family.timeSlot || 'to be assigned',
      timeAvailability: family.timeAvailability
    });

    this.students.clear();
    for (const student of family.students ?? []) {
      this.students.push(new FormGroup({
        name: new FormControl(student.name, [Validators.required]),
        grade: new FormControl(student.grade, [Validators.required]),
        school: new FormControl(student.school, [Validators.required]),
        requestedSupplies: new FormControl((student.requestedSupplies ?? []).join(', '))
      }));
    }

    if (this.students.length === 0) {
      this.addStudent();
    }
  }
}
