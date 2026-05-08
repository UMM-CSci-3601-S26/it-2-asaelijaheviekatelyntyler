import { Component, HostListener, OnInit, inject } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatListModule } from '@angular/material/list';
import { MatSidenavModule } from '@angular/material/sidenav';
import { MatToolbarModule } from '@angular/material/toolbar';
import { ActivatedRouteSnapshot, Route, Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { AuthService } from './auth/auth-service';

@Component({
  selector: 'app-root',
  templateUrl: './app.component.html',
  styleUrls: ['./app.component.scss'],
  imports: [
    MatSidenavModule,
    MatToolbarModule,
    MatListModule,
    RouterLink,
    RouterLinkActive,
    MatIconModule,
    MatButtonModule,
    RouterOutlet
  ]
})
export class AppComponent implements OnInit {
  title = 'Ready For Supplies';
  authService = inject(AuthService);
  router = inject(Router);

  ngOnInit(): void {
    this.syncAccessProfileSilently();
  }

  logout() {
    this.authService.logout().subscribe({
      next: () => this.router.navigate(['/login']),
      error: () => this.router.navigate(['/login'])
    });
  }

  @HostListener('window:focus')
  onWindowFocus() {
    this.syncAccessProfileSilently();
  }

  @HostListener('document:visibilitychange')
  onVisibilityChange() {
    if (document.visibilityState === 'visible') {
      this.syncAccessProfileSilently();
    }
  }

  private syncAccessProfileSilently() {
    this.authService.syncAccessProfile().subscribe({
      next: () => this.enforceRouteAccess(),
      error: () => this.enforceRouteAccess()
    });
  }

  private enforceRouteAccess() {
    const snapshot = this.deepestRoute(this.router.routerState.snapshot.root);
    const access = this.evaluateAccess(snapshot.data);

    if (access === 'login') {
      this.router.navigate(['/login']);
      return;
    }

    if (access === 'deny') {
      this.router.navigate(['/']);
    }
  }

  canAccessPath(path: string): boolean {
    const normalizedPath = path.replace(/^\//, '');
    const route = this.router.config.find(candidate => candidate.path === normalizedPath);
    return this.evaluateAccess(route?.data) === 'allow';
  }

  private evaluateAccess(data?: Route['data']): 'allow' | 'login' | 'deny' {
    const allowed = (data?.['roles'] as string[] | undefined) ?? [];
    const requiredPermissions = (data?.['permissions'] as string[] | undefined) ?? [];

    if (allowed.length === 0 && requiredPermissions.length === 0) {
      return 'allow';
    }

    if (!this.authService.loggedIn) {
      return 'login';
    }

    if (allowed.length > 0 && !allowed.includes(this.authService.systemRole!)) {
      return 'deny';
    }

    if (requiredPermissions.length > 0 && !this.authService.hasAllPermissions(requiredPermissions)) {
      return 'deny';
    }

    return 'allow';
  }

  private deepestRoute(snapshot: ActivatedRouteSnapshot): ActivatedRouteSnapshot {
    let current = snapshot;
    while (current.firstChild) {
      current = current.firstChild;
    }
    return current;
  }
}
