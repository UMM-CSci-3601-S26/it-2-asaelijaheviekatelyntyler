import { ChecklistService } from './checklist.service';
import { Checklist } from './checklist';
import { environment } from '../../environments/environment';

describe('ChecklistService', () => {
  let service: ChecklistService;
  let httpTestingController: HttpTestingController;

  const checklistUrl = `${environment.apiUrl}checklists`;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [ChecklistService]
    });

    service = TestBed.inject(ChecklistService);
    httpTestingController = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpTestingController.verify();
  });
