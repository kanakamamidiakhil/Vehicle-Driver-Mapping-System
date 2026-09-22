import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { DriverSession } from '../../core/driver-session.service';

export const driverGuard: CanActivateFn = () =>
  inject(DriverSession).loggedIn() || inject(Router).parseUrl('/driver/login');
