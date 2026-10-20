import { Component } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { AppBarModule } from '@progress/kendo-angular-navigation';

@Component({
  selector: 'app-root',
  imports: [RouterOutlet, RouterLink, RouterLinkActive, AppBarModule],
  templateUrl: './app.html',
  styleUrl: './app.scss',
})
export class App {
  nav = [
    { path: '/dashboard', label: 'Dashboard', icon: 'k-i-chart-column-clustered' },
    { path: '/proposals', label: 'Proposals', icon: 'k-i-file-txt' },
    { path: '/customers', label: 'Customers', icon: 'k-i-user' },
    { path: '/software', label: 'Software', icon: 'k-i-cloud' },
    { path: '/services', label: 'Services', icon: 'k-i-gears' },
    { path: '/settings', label: 'Pricing Settings', icon: 'k-i-dollar' },
  ];
}
