import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { AuthGuard } from './auth/auth.guard';
import { RoleGuard } from './auth/role.guard';

// Routing configuration for the application
//
// Public routes (no guards)
// -------------------------
//   /                  Home landing page — sign-up entry point
//   /login             General login (role is determined by stored account)
//   /sign-up           Volunteer self-registration
//   /guardian-sign-up  Guardian self-registration
//                      Both sign-up routes reuse SignUpComponent; the component
//                      reads router.url to determine which role to assign.
//
// Protected routes (AuthGuard + RoleGuard)
// ----------------------------------------
//   Role 'admin' only             : /dashboard, /families/new, /settings, /supplylist/new
//   Role 'admin' or 'volunteer'   : /checklists, /families, /inventory
//   Role 'admin', 'volunteer',
//         or 'guardian'           : /supplylist
//
// Future: when email-invite links are added for guardians, the link will point
// directly to /guardian-sign-up so no routing changes will be needed.
const routes: Routes = [
  {path: '', loadComponent: () => import('./home/home.component').then(m => m.HomeComponent), title: 'Home'},
  {path: 'login', loadComponent: () => import('./auth/login/login.component').then(m => m.LoginComponent), title: 'Login'},
  {path: 'sign-up', loadComponent: () => import('./auth/sign-up/sign-up.component').then(m => m.SignUpComponent), title: 'Volunteer Sign Up'},
  {path: 'guardian-sign-up', loadComponent: () => import('./auth/sign-up/sign-up.component').then(m => m.SignUpComponent), title: 'Guardian Sign Up'},
  {path: 'family-portal', loadComponent: () => import('./family/family-portal-home.component').then(m => m.FamilyPortalHomeComponent), title: 'Family Portal',
    canActivate: [AuthGuard, RoleGuard], data: { roles: ['GUARDIAN'], permissions: ['family_portal_access'] }},
  {path: 'family-portal/form', loadComponent: () => import('./family/family-portal-form.component').then(m => m.FamilyPortalFormComponent), title: 'Family Form',
    canActivate: [AuthGuard, RoleGuard], data: { roles: ['GUARDIAN'], permissions: ['family_portal_access'] }},
  {path: 'dashboard', loadComponent: () => import('./operator-dash/operator-dash.component').then(m => m.OperatorDashComponent), title: 'Operator Dashboard',
    canActivate: [AuthGuard, RoleGuard], data: { roles: ['ADMIN', 'VOLUNTEER'], permissions: ['view_dashboard_stats'] }},
  {path: 'checklists', loadComponent: () => import('./checklist/checklist-view.component').then(m => m.ChecklistViewComponent), title: 'Checklists',
    canActivate: [AuthGuard, RoleGuard], data: { roles: ['ADMIN', 'VOLUNTEER'], permissions: ['view_checklist'] }},
  {path: 'families', loadComponent: () => import('./family/family-view.component').then(m => m.FamilyViewComponent), title: 'Families',
    canActivate: [AuthGuard, RoleGuard], data: { roles: ['ADMIN', 'VOLUNTEER'], permissions: ['view_families'] }},
  {path: 'families/new', loadComponent: () => import('./family/add-family.component').then(m => m.AddFamilyComponent), title: 'Add Family',
    canActivate: [AuthGuard, RoleGuard], data: { roles: ['ADMIN', 'VOLUNTEER'], permissions: ['add_family'] }},
  {path: 'inventory', loadComponent: () => import('./inventory/inventory-table.component').then(m => m.InventoryTableComponent), title: 'Inventory',
    canActivate: [AuthGuard, RoleGuard], data: { roles: ['ADMIN', 'VOLUNTEER'], permissions: ['view_inventory'] }},
  {path: 'supplylist', loadComponent: () => import('./supplylist/supplylist.component').then(m => m.SupplyListComponent), title: 'Supply List',
    canActivate: [AuthGuard, RoleGuard], data: { roles: ['ADMIN', 'VOLUNTEER'], permissions: ['view_supply_lists'] }},
  {path: 'settings', loadComponent: () => import('./settings/settings.component').then(m => m.SettingsComponent), title: 'Settings',
    canActivate: [AuthGuard, RoleGuard], data: { roles: ['ADMIN', 'VOLUNTEER'], permissions: ['view_settings'] }},
  {path: 'users', loadComponent: () => import('./users/users.component').then(m => m.UsersComponent), title: 'Users',
    canActivate: [AuthGuard, RoleGuard], data: { roles: ['ADMIN'] }},
  {path: 'supplylist/new', loadComponent: () => import('./supplylist/add-supplylist.component').then(m => m.AddSupplyListComponent), title: 'Add Supply List Item',
    canActivate: [AuthGuard, RoleGuard], data: { roles: ['ADMIN', 'VOLUNTEER'], permissions: ['add_supply_list'] }}
];

@NgModule({
  imports: [
    RouterModule.forRoot(routes)
  ],
  exports: [
    RouterModule
  ]
})
export class AppRoutingModule { }
