import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { HomeComponent } from './home/home.component';
import { OperatorDashComponent } from './operator-dash/operator-dash.component';
import { FamilyViewComponent } from './family/family-view.component';
import { AddFamilyComponent } from './family/add-family.component';
import { AddInventoryComponent } from './inventory/add-inventory.component';
import { InventoryViewComponent } from './inventory/inventory.component';

// Routing configuration for the application
const routes: Routes = [
  {path: '', component: HomeComponent, title: 'Home'},
  {path: 'dashboard', component: OperatorDashComponent, title: 'Operator Dashboard'},
  {path: 'families', component: FamilyViewComponent, title: 'Families'},
  {path: 'families/new', component: AddFamilyComponent, title: 'Add Family'},
  {path: 'inventory', component: InventoryViewComponent, title: 'Inventory'},
  {path: 'inventory/new', component: AddInventoryComponent, title: 'Add Inventory'}
];

@NgModule({
  imports: [RouterModule.forRoot(routes)],
  exports: [RouterModule]
})
export class AppRoutingModule { }
