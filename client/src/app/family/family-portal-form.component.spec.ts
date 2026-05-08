import { ComponentFixture, TestBed, waitForAsync } from '@angular/core/testing';
import { MatSnackBar } from '@angular/material/snack-bar';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { provideRouter, Router } from '@angular/router';
import { of, throwError } from 'rxjs';

import { FamilyPortalFormComponent } from './family-portal-form.component';
import { FamilyPortalService } from './family-portal.service';

describe('FamilyPortalFormComponent', () => {
  let component: FamilyPortalFormComponent;
  let fixture: ComponentFixture<FamilyPortalFormComponent>;
  let familyPortalServiceMock: jasmine.SpyObj<Pick<FamilyPortalService, 'getSummary' | 'upsertForm'>>;
  let router: Router;

  beforeEach(waitForAsync(() => {
    familyPortalServiceMock = jasmine.createSpyObj('FamilyPortalService', ['getSummary', 'upsertForm']);
    familyPortalServiceMock.getSummary.and.returnValue(of({
      profileComplete: false,
      family: null,
      schools: [{ name: 'North High' }],
    }));
    familyPortalServiceMock.upsertForm.and.returnValue(of({ profileComplete: true }));

    TestBed.configureTestingModule({
      imports: [FamilyPortalFormComponent, NoopAnimationsModule],
      providers: [
        provideRouter([]),
        { provide: FamilyPortalService, useValue: familyPortalServiceMock },
      ],
    }).compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(FamilyPortalFormComponent);
    component = fixture.componentInstance;
    router = TestBed.inject(Router);
    fixture.detectChanges();
  });

  function fillValidForm() {
    component.familyForm.patchValue({
      guardianName: 'Taylor Guardian',
      email: 'taylor@example.com',
      address: '123 Main St',
      timeSlot: 'to be assigned',
      timeAvailability: {
        earlyMorning: true,
        lateMorning: false,
        earlyAfternoon: true,
        lateAfternoon: false
      }
    });

    component.students.clear();
    component.addStudent();
    component.students.at(0).patchValue({
      name: 'Avery',
      grade: '3',
      school: 'North High',
      requestedSupplies: 'Pencils, Paper,  Markers '
    });
  }

  it('should create and add a student when no family exists yet', () => {
    expect(component).toBeTruthy();
    expect(component.isLoading).toBeFalse();
    expect(component.students.length).toBe(1);
    expect(component.schools).toEqual([{ name: 'North High' }]);
  });

  it('should patch the form from the summary family when present', () => {
    familyPortalServiceMock.getSummary.and.returnValue(of({
      profileComplete: true,
      schools: [{ name: 'North High' }],
      family: {
        guardianName: 'Taylor Guardian',
        email: 'taylor@example.com',
        address: '123 Main St',
        timeSlot: '9:00-10:00 AM',
        students: [{
          name: 'Avery',
          grade: '3',
          school: 'North High',
          requestedSupplies: ['Pencils', 'Paper']
        }],
        timeAvailability: {
          earlyMorning: true,
          lateMorning: false,
          earlyAfternoon: false,
          lateAfternoon: true
        }
      }
    }));

    fixture = TestBed.createComponent(FamilyPortalFormComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();

    expect(component.familyForm.value.guardianName).toBe('Taylor Guardian');
    expect(component.students.length).toBe(1);
    expect(component.students.at(0).value.requestedSupplies).toBe('Pencils, Paper');
  });

  it('should use custom availability labels from the summary', () => {
    familyPortalServiceMock.getSummary.and.returnValue(of({
      profileComplete: false,
      schools: [{ name: 'North High' }],
      timeAvailability: {
        earlyMorning: '7:30-8:30 AM',
        lateMorning: '8:30-9:30 AM',
        earlyAfternoon: '12:30-1:30 PM',
        lateAfternoon: '1:30-2:30 PM'
      },
      family: null
    }));

    fixture = TestBed.createComponent(FamilyPortalFormComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();

    expect(component.timeAvailabilityLabels.earlyMorning).toBe('7:30-8:30 AM');
    expect(component.timeAvailabilityLabels.lateAfternoon).toBe('1:30-2:30 PM');
  });

  it('should show a snackbar instead of saving when the form is invalid', () => {
    const snackBarOpenSpy = spyOn(
      (component as unknown as { snackBar: MatSnackBar }).snackBar,
      'open'
    );

    component.saveForm();

    expect(familyPortalServiceMock.upsertForm).not.toHaveBeenCalled();
    expect(snackBarOpenSpy).toHaveBeenCalledWith(
      'Please complete all required fields.',
      'OK',
      { duration: 2500 }
    );
  });

  it('should save a normalized payload and navigate to the portal on success', () => {
    const navigateSpy = spyOn(router, 'navigate');
    const snackBarOpenSpy = spyOn(
      (component as unknown as { snackBar: MatSnackBar }).snackBar,
      'open'
    );
    fillValidForm();

    component.saveForm();

    expect(familyPortalServiceMock.upsertForm).toHaveBeenCalledWith({
      guardianName: 'Taylor Guardian',
      email: 'taylor@example.com',
      address: '123 Main St',
      timeSlot: 'to be assigned',
      students: [{
        name: 'Avery',
        grade: '3',
        school: 'North High',
        requestedSupplies: ['Pencils', 'Paper', 'Markers']
      }],
      timeAvailability: {
        earlyMorning: true,
        lateMorning: false,
        earlyAfternoon: true,
        lateAfternoon: false
      }
    });
    expect(snackBarOpenSpy).toHaveBeenCalledWith('Family form saved.', 'OK', { duration: 2000 });
    expect(navigateSpy).toHaveBeenCalledWith(['/family-portal']);
  });

  it('should show the backend error when saving fails', () => {
    const snackBarOpenSpy = spyOn(
      (component as unknown as { snackBar: MatSnackBar }).snackBar,
      'open'
    );
    fillValidForm();
    familyPortalServiceMock.upsertForm.and.returnValue(throwError(() => ({
      error: { message: 'Duplicate family record.' }
    })));

    component.saveForm();

    expect(snackBarOpenSpy).toHaveBeenCalledWith('Duplicate family record.', 'OK', { duration: 3500 });
  });

  it('should fall back to the generic save error message when the backend omits one', () => {
    const snackBarOpenSpy = spyOn(
      (component as unknown as { snackBar: MatSnackBar }).snackBar,
      'open'
    );
    fillValidForm();
    familyPortalServiceMock.upsertForm.and.returnValue(throwError(() => ({ error: {} })));

    component.saveForm();

    expect(snackBarOpenSpy).toHaveBeenCalledWith('Unable to save family form.', 'OK', { duration: 3500 });
  });

  it('should fall back to default payload values when raw form data is missing', () => {
    const navigateSpy = spyOn(router, 'navigate');
    spyOn(component.familyForm, 'getRawValue').and.returnValue({
      guardianName: null,
      email: undefined,
      address: null,
      timeSlot: undefined,
      students: [{
        name: undefined,
        grade: null,
        school: undefined,
        requestedSupplies: undefined
      }],
      timeAvailability: undefined
    } as never);
    spyOnProperty(component.familyForm, 'valid', 'get').and.returnValue(true);

    component.saveForm();

    expect(familyPortalServiceMock.upsertForm).toHaveBeenCalledWith({
      guardianName: '',
      email: '',
      address: '',
      timeSlot: 'to be assigned',
      students: [{
        name: '',
        grade: '',
        school: '',
        requestedSupplies: []
      }],
      timeAvailability: {
        earlyMorning: false,
        lateMorning: false,
        earlyAfternoon: false,
        lateAfternoon: false
      }
    });
    expect(navigateSpy).toHaveBeenCalledWith(['/family-portal']);
  });

  it('should add a starter student when the summary request fails', () => {
    familyPortalServiceMock.getSummary.and.returnValue(throwError(() => new Error('load failed')));

    fixture = TestBed.createComponent(FamilyPortalFormComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();

    expect(component.isLoading).toBeFalse();
    expect(component.students.length).toBe(1);
  });

  it('should add a starter student when an existing family has no students', () => {
    familyPortalServiceMock.getSummary.and.returnValue(of({
      profileComplete: false,
      schools: [{ name: 'North High' }],
      family: {
        guardianName: 'Taylor Guardian',
        email: 'taylor@example.com',
        address: '123 Main St',
        timeSlot: '',
        students: [],
        timeAvailability: {
          earlyMorning: false,
          lateMorning: true,
          earlyAfternoon: false,
          lateAfternoon: false
        }
      }
    }));

    fixture = TestBed.createComponent(FamilyPortalFormComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();

    expect(component.familyForm.value.timeSlot).toBe('to be assigned');
    expect(component.students.length).toBe(1);
  });

  it('should remove a student from the form array', () => {
    component.addStudent();

    component.removeStudent(0);

    expect(component.students.length).toBe(1);
  });
});
