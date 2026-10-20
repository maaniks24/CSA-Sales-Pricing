import { Routes } from '@angular/router';

export const routes: Routes = [
  { path: '', pathMatch: 'full', redirectTo: 'dashboard' },
  { path: 'dashboard', loadComponent: () => import('./pages/dashboard/dashboard').then((m) => m.Dashboard) },
  { path: 'proposals', loadComponent: () => import('./pages/proposals/proposal-list').then((m) => m.ProposalList) },
  { path: 'proposals/new', loadComponent: () => import('./pages/proposals/proposal-editor').then((m) => m.ProposalEditor) },
  { path: 'proposals/:id', loadComponent: () => import('./pages/proposals/proposal-editor').then((m) => m.ProposalEditor) },
  { path: 'customers', loadComponent: () => import('./pages/masters/customers').then((m) => m.Customers) },
  { path: 'software', loadComponent: () => import('./pages/masters/software').then((m) => m.SoftwareMaster) },
  { path: 'services', loadComponent: () => import('./pages/masters/services').then((m) => m.ServicesMaster) },
  { path: 'settings', loadComponent: () => import('./pages/masters/settings').then((m) => m.Settings) },
  { path: '**', redirectTo: 'dashboard' },
];
