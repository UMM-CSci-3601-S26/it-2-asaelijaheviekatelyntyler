// Angular Imports
import { ComponentFixture, TestBed, waitForAsync, tick, fakeAsync } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { InventoryService } from './inventory.service';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { MatPaginatorHarness } from '@angular/material/paginator/testing';
import { TestbedHarnessEnvironment } from '@angular/cdk/testing/testbed';
import { MatTableHarness } from '@angular/material/table/testing';
import { HarnessLoader } from '@angular/cdk/testing';

// RxJS Imports
import { Observable, of } from 'rxjs';

// Inventory Imports
import { MockInventoryService } from 'src/testing/inventory.service.mock';
import { Inventory } from './inventory';
import { InventoryComponent } from './inventory.component';


describe('Inventory Table', () => {
  let inventoryTable: InventoryComponent;
  let fixture: ComponentFixture<InventoryComponent>
  let inventoryService: InventoryService;
  let loader: HarnessLoader

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [InventoryComponent],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: InventoryService, useClass: MockInventoryService },
        provideRouter([])
      ],
    });
  });

  beforeEach(waitForAsync(() => {
    TestBed.compileComponents().then(() => {
      fixture = TestBed.createComponent(InventoryComponent);
      inventoryTable = fixture.componentInstance;
      inventoryService = TestBed.inject(InventoryService);
      fixture.detectChanges();
      loader = TestbedHarnessEnvironment.loader(fixture);
    });
  }));

  it('should create the component', () => {
    expect(inventoryTable).toBeTruthy();
  });

  it('should initialize with serverFilteredTable available', () => {
    const inventory = inventoryTable.serverFilteredInventory();
    expect(inventory).toBeDefined();
    expect(Array.isArray(inventory)).toBe(true);
  });

  it('should load the paginator harnesses', async () => {
    const paginators = await loader.getAllHarnesses(MatPaginatorHarness);
    expect(paginators.length).toBe(1);
  });

  it('should load harness for the table', async () => {
    const tables = await loader.getAllHarnesses(MatTableHarness);
    expect(tables.length).toBe(1);
  });

  it('should call getInventory() when item signal changes', fakeAsync(() => {
    const spy = spyOn(inventoryService, 'getInventory').and.callThrough();
    inventoryTable.item.set('Markers');
    fixture.detectChanges();
    tick(300);
    expect(spy).toHaveBeenCalledWith({ item: 'Markers', brand: undefined, color: undefined, size: undefined, type: undefined, material: undefined });
  }));

  it('should call getInventory() when brand signal changes', fakeAsync(() => {
    const spy = spyOn(inventoryService, 'getInventory').and.callThrough();
    inventoryTable.brand.set('Crayola');
    fixture.detectChanges();
    tick(300);
    expect(spy).toHaveBeenCalledWith({ item: undefined, brand: 'Crayola', color: undefined, size: undefined, type: undefined, material: undefined });
  }));

  it('should call getInventory() when color signal changes', fakeAsync(() => {
    const spy = spyOn(inventoryService, 'getInventory').and.callThrough();
    inventoryTable.color.set('Red');
    fixture.detectChanges();
    tick(300);
    expect(spy).toHaveBeenCalledWith({ item: undefined, brand: undefined, color: 'Red', size: undefined, type: undefined, material: undefined });
  }));

  it('should call getInventory() when size signal changes', fakeAsync(() => {
    const spy = spyOn(inventoryService, 'getInventory').and.callThrough();
    inventoryTable.size.set('Wide Ruled');
    fixture.detectChanges();
    tick(300);
    expect(spy).toHaveBeenCalledWith({ item: undefined, brand: undefined, color: undefined, size: 'Wide Ruled', type: undefined, material: undefined });
  }));

  it('should call getInventory() when type signal changes', fakeAsync(() => {
    const spy = spyOn(inventoryService, 'getInventory').and.callThrough();
    inventoryTable.type.set('Spiral');
    fixture.detectChanges();
    tick(300);
    expect(spy).toHaveBeenCalledWith({ item: undefined, brand: undefined, color: undefined, size: undefined, type: 'Spiral', material: undefined });
  }));

  it('should call getInventory() when material signal changes', fakeAsync(() => {
    const spy = spyOn(inventoryService, 'getInventory').and.callThrough();
    inventoryTable.material.set('Plastic');
    fixture.detectChanges();
    tick(300);
    expect(spy).toHaveBeenCalledWith({ item: undefined, brand: undefined, color: undefined, size: undefined, type: undefined, material: 'Plastic' });
  }));

  it('should call getInventory() when brand and color signals change', fakeAsync(() => {
    const spy = spyOn(inventoryService, 'getInventory').and.callThrough();
    inventoryTable.color.set('Black');
    inventoryTable.brand.set('Crayola');
    fixture.detectChanges();
    tick(300);
    expect(spy).toHaveBeenCalledWith({ item: undefined, brand: 'Crayola', color: 'Black', size: undefined, type: undefined, material: undefined });
  }));

  it('should call getInventory() when item, brand, color, and material signals change', fakeAsync(() => {
    const spy = spyOn(inventoryService, 'getInventory').and.callThrough();
    inventoryTable.item.set('Notebook');
    inventoryTable.brand.set('Five Star');
    inventoryTable.color.set('Yellow');
    inventoryTable.type.set('Spiral');
    fixture.detectChanges();
    tick(300);
    expect(spy).toHaveBeenCalledWith({ item: 'Notebook', brand: 'Five Star', color: 'Yellow', size: undefined, type: 'Spiral', material: undefined });
  }));

  it('should not show error message on successful load', () => {
    expect(inventoryTable.errMsg()).toBeUndefined();
  });

  // Nothing to delete: should report a user-facing error and not call backend.
  it('should set errMsg when delete is called with undefined id', () => {
    const deleteSpy = spyOn(inventoryService, 'deleteInventory').and.returnValue(of(undefined));
    inventoryTable.confirmDelete(undefined);

    expect(inventoryTable.errMsg()).toEqual('Cannot delete: missing item ID');
    expect(deleteSpy).not.toHaveBeenCalled();
  });

  // Confirmed delete flow: user confirmation -> service.deleteInventory() -> row removed.
  it('should call deleteInventory and remove row when confirmed', fakeAsync(() => {
    const idToDelete = 'item-123';
    const deleteSpy = spyOn(inventoryService, 'deleteInventory').and.returnValue(of(undefined));

    //avoid browser confirm dialog side effect and simulate user clicking "OK"
    spyOn(window, 'confirm').and.returnValue(true);

    // Matrix row pre-seeding to ensure the item to delete is present in the table's data source before deletion.
    inventoryTable.dataSource.data = [
      { _id: idToDelete, item: 'Markers', description: 'test', brand: 'Crayola', color: 'Black', count: 1, size: 'Wide', type: 'Washable', material: 'Plastic', quantity: 0, notes: 'n/a' } as unknown as Inventory,
    ];

    inventoryTable.confirmDelete(idToDelete);
    tick();

    expect(deleteSpy).toHaveBeenCalledWith(idToDelete);
    expect(inventoryTable.dataSource.data.length).toBe(0);
  }));
});

describe('Misbehaving Inventory Table', () => {
  let inventoryTable: InventoryComponent;
  let fixture: ComponentFixture<InventoryComponent>;

  let inventoryServiceStub: {
    getInventory: () => Observable<Inventory[]>;
    //filterInventory: () => Inventory[];
  };

  beforeEach(() => {
    inventoryServiceStub = {
      getInventory: () =>
        new Observable((observer) => {
          observer.error('getInventory() Observer generates an error');
        }),
      //filterInventory: () => []
    };
  });

  beforeEach(waitForAsync(() => {
    TestBed.configureTestingModule({
      imports: [
        InventoryComponent
      ],
      providers: [{
        provide: InventoryService,
        useValue: inventoryServiceStub
      }, provideRouter([])],
    })
      .compileComponents();
  }));

  beforeEach(fakeAsync(() => {
    fixture = TestBed.createComponent(InventoryComponent);
    inventoryTable = fixture.componentInstance;
    fixture.detectChanges();
    tick(300);
  }));

  it("generates an error if we don't set up a InventoryService", () => {
    expect(inventoryTable.serverFilteredInventory())
      .withContext("service can't give values to the list if it's not there")
      .toEqual([]);
    expect(inventoryTable.errMsg())
      .withContext('the error message will be')
      .toContain('Problem contacting the server – Error Code:');
  });
});
