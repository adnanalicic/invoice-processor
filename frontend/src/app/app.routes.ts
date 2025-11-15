import { Routes } from '@angular/router';
import { StacksListComponent } from './components/stacks/stacks-list.component';
import { StackDetailComponent } from './components/stack-detail/stack-detail.component';
import { SimulateEmailComponent } from './components/simulate-email/simulate-email.component';
import { ManualStackComponent } from './components/manual-stack/manual-stack.component';
import { AdminPageComponent } from './components/admin/admin-page.component';

export const routes: Routes = [
  { path: '', redirectTo: '/stacks', pathMatch: 'full' },
  { path: 'stacks', component: StacksListComponent },
  { path: 'stacks/:id', component: StackDetailComponent },
  { path: 'simulate', component: SimulateEmailComponent },
  { path: 'manual-upload', component: ManualStackComponent },
  { path: 'admin', component: AdminPageComponent }
];
