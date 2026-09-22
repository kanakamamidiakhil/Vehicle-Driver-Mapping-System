import { Routes } from '@angular/router';
import { Dashboard } from './pages/dashboard';
import { DriversPage } from './pages/admin/drivers';
import { VehiclesPage } from './pages/admin/vehicles';
import { AssignmentsPage } from './pages/admin/assignments';
import { AssignVehiclePage } from './pages/admin/assign-vehicle';
import { SearchDriversPage } from './pages/admin/search-drivers';
import { DriverAssignmentSearchPage } from './pages/admin/driver-assignment-search';
import { NearbyDriversPage } from './pages/admin/nearby-drivers';
import { DriverLoginPage } from './pages/driver/login';
import { DriverRegisterPage } from './pages/driver/register';
import { MyAssignmentsPage } from './pages/driver/my-assignments';
import { AssistantPage } from './pages/assistant/assistant';
import { driverGuard } from './pages/driver/driver.guard';

export const routes: Routes = [
  { path: '', component: Dashboard, title: 'Fleet dashboard' },
  { path: 'assistant', component: AssistantPage, title: 'AI assistant' },
  { path: 'admin/drivers', component: DriversPage, title: 'Drivers' },
  { path: 'admin/vehicles', component: VehiclesPage, title: 'Vehicles' },
  { path: 'admin/assignments', component: AssignmentsPage, title: 'Assignments' },
  { path: 'admin/assign', component: AssignVehiclePage, title: 'Assign vehicle' },
  { path: 'admin/search-drivers', component: SearchDriversPage, title: 'Search drivers' },
  { path: 'admin/driver-assignments', component: DriverAssignmentSearchPage, title: 'Driver assignments' },
  { path: 'admin/nearby', component: NearbyDriversPage, title: 'Nearby drivers' },
  { path: 'driver/login', component: DriverLoginPage, title: 'Driver login' },
  { path: 'driver/register', component: DriverRegisterPage, title: 'Driver registration' },
  { path: 'driver/assignments', component: MyAssignmentsPage, canActivate: [driverGuard], title: 'My assignments' },
  { path: '**', redirectTo: '' },
];
