// Angular Testing Imports
import { ComponentFixture, TestBed, waitForAsync } from '@angular/core/testing';
import { FamilyCardComponent } from './family-card.component';
import { AuthService } from '../auth/auth-service';

// Family Interface Import
import { Family } from './family';

// Test suite for the FamilyCardComponent, which displays information about a family and their requested supplies
describe('FamilyCardComponent', () => {
  let component: FamilyCardComponent;
  let fixture: ComponentFixture<FamilyCardComponent>;
  let expectedFamily: Family;

  beforeEach(waitForAsync(() => {
    TestBed.configureTestingModule({
      imports: [
        FamilyCardComponent
      ],
      providers: [
        {
          provide: AuthService,
          useValue: {
            isVolunteer: () => false,
            hasPermission: () => false
          }
        }
      ]
    })
    // Compile the component and its template before running tests
      .compileComponents();
  }));

  // Set up the component instance and provide it with a sample family before each test
  beforeEach(() => {
    fixture = TestBed.createComponent(FamilyCardComponent);
    component = fixture.componentInstance;
    expectedFamily = {
      // Example Family with Two Kids
      _id: 'chris_id',
      guardianName: 'Chris',
      address: '123 Street',
      email: 'chris@email.com',
      timeSlot: '9:00-10:00',
      timeAvailability: { earlyMorning: true, lateMorning: false, earlyAfternoon: true, lateAfternoon: false },
      students: [
        {
          name: 'Chris Jr.',
          grade: '2',
          school: "Morris Elementary",
          requestedSupplies: ['backpack', 'markers']
        },
        {
          name: 'Christy',
          grade: '2',
          school: "Morris Elementary",
          requestedSupplies: ['backpack', 'pencils']
        }
      ]
    };

    fixture.componentRef.setInput('family', expectedFamily);
    fixture.detectChanges();
  });

  // Test to ensure the component is created successfully
  it('should create', () => {
    expect(component).toBeTruthy();
  });

  // Test to verify that the component's family input is correctly associated with the expected family data
  it('should be associated with the correct family', () => {
    expect(component.family()).toEqual(expectedFamily);
  });

  // Test to check that the guardian's name in the family data is correctly displayed as "Chris"
  it('should be the family named Chris', () => {
    expect(component.family().guardianName).toEqual('Chris');
  });

  it('shows linked guardian status when ownerUserId exists', () => {
    const linkedFamily: Family = {
      ...expectedFamily,
      ownerUserId: 'guardian-user-id-1'
    };

    fixture.componentRef.setInput('family', linkedFamily);
    fixture.detectChanges();

    expect(component.hasLinkedGuardianAccount).toBeTrue();
    expect(component.guardianLinkStatusLabel).toContain('Linked Guardian Account');
  });

  it('shows manual status when ownerUserId is missing', () => {
    const manualFamily: Family = {
      ...expectedFamily,
      ownerUserId: undefined
    };

    fixture.componentRef.setInput('family', manualFamily);
    fixture.detectChanges();

    expect(component.hasLinkedGuardianAccount).toBeFalse();
    expect(component.guardianLinkStatusLabel).toContain('Manually Added');
  });

  it('reports pending delete request status when request exists', () => {
    const pendingFamily: Family = {
      ...expectedFamily,
      deleteRequest: {
        requested: true,
        message: 'Duplicate submission'
      }
    };

    fixture.componentRef.setInput('family', pendingFamily);
    fixture.detectChanges();

    expect(component.hasPendingDeleteRequest).toBeTrue();
  });

  it('reports no pending delete request when request is missing', () => {
    const activeFamily: Family = {
      ...expectedFamily,
      deleteRequest: undefined
    };

    fixture.componentRef.setInput('family', activeFamily);
    fixture.detectChanges();

    expect(component.hasPendingDeleteRequest).toBeFalse();
  });

  it('lists available times when time availability options are enabled', () => {
    expect(component.getAvailableTimes()).toBe('Early Morning, Early Afternoon');
  });

  it('returns none when no time availability exists or no options are selected', () => {
    fixture.componentRef.setInput('family', {
      ...expectedFamily,
      timeAvailability: {
        earlyMorning: false,
        lateMorning: false,
        earlyAfternoon: false,
        lateAfternoon: false
      }
    });
    fixture.detectChanges();
    expect(component.getAvailableTimes()).toBe('None');

    fixture.componentRef.setInput('family', {
      ...expectedFamily,
      timeAvailability: undefined as never
    });
    fixture.detectChanges();
    expect(component.getAvailableTimes()).toBe('None');
  });

  it('lists student names when students are present', () => {
    expect(component.studentNames).toBe('Chris Jr., Christy');
  });

  it('shows a fallback when no students are listed', () => {
    fixture.componentRef.setInput('family', {
      ...expectedFamily,
      students: []
    });
    fixture.detectChanges();

    expect(component.studentNames).toBe('No students listed');
  });

  it('emits a delete request event when asked', () => {
    spyOn(component.requestDelete, 'emit');

    component.onRequestDelete();

    expect(component.requestDelete.emit).toHaveBeenCalled();
  });
});
