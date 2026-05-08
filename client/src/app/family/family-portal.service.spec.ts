import { TestBed } from '@angular/core/testing';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideHttpClient } from '@angular/common/http';

import { FamilyPortalService } from './family-portal.service';

describe('FamilyPortalService', () => {
  let service: FamilyPortalService;
  let httpTestingController: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        FamilyPortalService
      ]
    });

    service = TestBed.inject(FamilyPortalService);
    httpTestingController = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpTestingController.verify();
  });

  it('creates', () => {
    expect(service).toBeTruthy();
  });

  it('getSummary calls GET /api/family-portal', () => {
    service.getSummary().subscribe(summary => {
      expect(summary.profileComplete).toBeFalse();
    });

    const req = httpTestingController.expectOne('/api/family-portal');
    expect(req.request.method).toBe('GET');
    req.flush({ profileComplete: false, family: null });
  });

  it('upsertForm calls PUT /api/family-portal/form', () => {
    const payload = {
      guardianName: 'Alex Guardian',
      email: 'alex@example.com',
      address: '123 Main St'
    };

    service.upsertForm(payload).subscribe(response => {
      expect(response.profileComplete).toBeTrue();
    });

    const req = httpTestingController.expectOne('/api/family-portal/form');
    expect(req.request.method).toBe('PUT');
    expect(req.request.body.guardianName).toBe('Alex Guardian');
    req.flush({ profileComplete: true });
  });

  it('getChecklist calls GET /api/family-portal/checklist', () => {
    service.getChecklist().subscribe(checklists => {
      expect(checklists.length).toBe(1);
    });

    const req = httpTestingController.expectOne('/api/family-portal/checklist');
    expect(req.request.method).toBe('GET');
    req.flush([
      {
        _id: 'c1',
        studentName: 'Jamie',
        school: 'Morris',
        grade: '3',
        requestedSupplies: [],
        checklist: []
      }
    ]);
  });

  it('getDriveDay calls GET /api/family-portal/drive-day', () => {
    service.getDriveDay().subscribe(data => {
      expect(data.driveDay?.date).toBe('2026-08-15');
    });

    const req = httpTestingController.expectOne('/api/family-portal/drive-day');
    expect(req.request.method).toBe('GET');
    req.flush({ driveDay: { date: '2026-08-15', message: 'See you there' }, timeSlot: '9:00-10:00', timeSlotStatus: 'assigned' });
  });
});
