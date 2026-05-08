import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MatDialog } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { provideRouter } from '@angular/router';
import { of, throwError } from 'rxjs';

import { AuthService } from '../auth/auth-service';
import { Family } from './family';
import { FamilyService } from './family.service';
import { FamilyViewComponent } from './family-view.component';

type FamilyViewInjectedServices = {
  dialog: jasmine.SpyObj<MatDialog>;
  snackBar: jasmine.SpyObj<MatSnackBar>;
};

describe('FamilyViewComponent', () => {
  let component: FamilyViewComponent;
  let fixture: ComponentFixture<FamilyViewComponent>;
  let familyService: jasmine.SpyObj<FamilyService>;
  let authService: jasmine.SpyObj<AuthService>;
  let dialog: jasmine.SpyObj<MatDialog>;
  let snackBar: jasmine.SpyObj<MatSnackBar>;

  const families: Family[] = [
    {
      _id: 'f1',
      guardianName: 'Linked Family',
      address: '123 Street',
      email: 'linked@example.com',
      ownerUserId: 'owner-1',
      timeSlot: '9:00',
      timeAvailability: { earlyMorning: true, lateMorning: false, earlyAfternoon: false, lateAfternoon: false },
      students: []
    },
    {
      _id: 'f2',
      guardianName: 'Manual Family',
      address: '456 Street',
      email: 'manual@example.com',
      ownerUserId: '   ',
      timeSlot: '10:00',
      timeAvailability: { earlyMorning: false, lateMorning: true, earlyAfternoon: false, lateAfternoon: false },
      students: []
    }
  ];

  beforeEach(async () => {
    familyService = jasmine.createSpyObj<FamilyService>('FamilyService', [
      'getFamilies',
      'exportFamilies',
      'requestFamilyDelete'
    ]);
    authService = jasmine.createSpyObj<AuthService>('AuthService', ['isVolunteer', 'hasPermission']);
    dialog = jasmine.createSpyObj<MatDialog>('MatDialog', ['open']);
    snackBar = jasmine.createSpyObj<MatSnackBar>('MatSnackBar', ['open']);

    familyService.getFamilies.and.returnValue(of(families));
    familyService.exportFamilies.and.returnValue(of('csv-data'));
    familyService.requestFamilyDelete.and.returnValue(of({}));
    authService.isVolunteer.and.returnValue(false);
    authService.hasPermission.and.callFake((permission: string) => permission !== 'request_family_delete');

    await TestBed.configureTestingModule({
      imports: [FamilyViewComponent],
      providers: [
        provideRouter([]),
        { provide: FamilyService, useValue: familyService },
        { provide: AuthService, useValue: authService },
        { provide: MatDialog, useValue: dialog },
        { provide: MatSnackBar, useValue: snackBar }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(FamilyViewComponent);
    component = fixture.componentInstance;
    const injectedServices = component as unknown as FamilyViewInjectedServices;
    injectedServices.dialog = dialog;
    injectedServices.snackBar = snackBar;
    fixture.detectChanges();
  });

  it('creates and loads families from the service', () => {
    expect(component).toBeTruthy();
    expect(component.families()).toEqual(families);
  });

  it('returns an empty family list when loading fails', async () => {
    familyService.getFamilies.and.returnValue(throwError(() => new Error('load failed')));
    const localFixture = TestBed.createComponent(FamilyViewComponent);
    const localComponent = localFixture.componentInstance;
    localFixture.detectChanges();
    await localFixture.whenStable();

    expect(localComponent.families()).toEqual([]);
  });

  it('filters families by linked status and can clear the filter', () => {
    component.linkStatusFilter = 'linked';
    expect(component.filteredFamilies.map(family => family.guardianName)).toEqual(['Linked Family']);

    component.linkStatusFilter = 'manual';
    expect(component.filteredFamilies.map(family => family.guardianName)).toEqual(['Manual Family']);

    component.clearFamilyFilters();
    expect(component.linkStatusFilter).toBe('all');
    expect(component.filteredFamilies.length).toBe(2);
  });

  it('downloads CSV data through a generated anchor tag', () => {
    spyOn(URL, 'createObjectURL').and.returnValue('blob-url');
    spyOn(URL, 'revokeObjectURL');
    const click = jasmine.createSpy('click');
    spyOn(document, 'createElement').and.returnValue({ click } as unknown as HTMLAnchorElement);

    component.downloadCSV();

    expect(familyService.exportFamilies).toHaveBeenCalled();
    expect(document.createElement).toHaveBeenCalledWith('a');
    expect(click).toHaveBeenCalled();
    expect(URL.revokeObjectURL).toHaveBeenCalledWith('blob-url');
  });

  it('does not open a delete request flow for non-volunteers or missing ids', () => {
    authService.hasPermission.and.returnValue(true);
    component.submitDeleteRequest({ ...families[0], _id: undefined });
    expect(dialog.open).not.toHaveBeenCalled();

    authService.isVolunteer.and.returnValue(false);
    authService.hasPermission.and.returnValue(false);
    component.submitDeleteRequest(families[0]);
    expect(dialog.open).not.toHaveBeenCalled();
  });

  it('stops delete request submission when the dialog result is blank', () => {
    authService.isVolunteer.and.returnValue(true);
    authService.hasPermission.and.returnValue(true);
    dialog.open.and.returnValue({
      afterClosed: () => of({ message: '   ' })
    } as never);

    component.submitDeleteRequest(families[0]);

    expect(dialog.open).toHaveBeenCalled();
    expect(familyService.requestFamilyDelete).not.toHaveBeenCalled();
  });

  it('submits a volunteer delete request and updates the local family state', () => {
    authService.isVolunteer.and.returnValue(true);
    authService.hasPermission.and.returnValue(true);
    dialog.open.and.returnValue({
      afterClosed: () => of({ message: '  duplicate entry  ' })
    } as never);

    const family = { ...families[0], deleteRequest: undefined };
    component.submitDeleteRequest(family);

    expect(familyService.requestFamilyDelete).toHaveBeenCalledWith('f1', 'duplicate entry');
    expect(family.deleteRequest).toEqual({
      requested: true,
      message: 'duplicate entry'
    });
    expect(snackBar.open).toHaveBeenCalledWith('Delete request submitted for admin review.', 'Close', { duration: 2500 });
  });

  it('shows the backend message when submitting a delete request fails', () => {
    authService.isVolunteer.and.returnValue(true);
    authService.hasPermission.and.returnValue(true);
    dialog.open.and.returnValue({
      afterClosed: () => of({ message: 'cleanup needed' })
    } as never);
    familyService.requestFamilyDelete.and.returnValue(throwError(() => ({ error: { message: 'Delete denied' } })));

    component.submitDeleteRequest(families[0]);

    expect(snackBar.open).toHaveBeenCalledWith('Delete denied', 'Close', { duration: 3500 });
  });
});
