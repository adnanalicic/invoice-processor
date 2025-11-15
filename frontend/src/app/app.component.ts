import { Component } from '@angular/core';
import { RouterOutlet, RouterLink } from '@angular/router';
import { MatToolbarModule } from '@angular/material/toolbar';
import { MatButtonModule } from '@angular/material/button';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [RouterOutlet, RouterLink, MatToolbarModule, MatButtonModule],
  template: `
    <mat-toolbar color="primary">
      <span>Invoice Processor</span>
      <span class="spacer"></span>
      <button mat-button routerLink="/stacks">Stacks</button>
      <button mat-button routerLink="/simulate">Simulate</button>
      <button mat-button routerLink="/manual-upload">Manual Upload</button>
       <button mat-button routerLink="/workflow">Workflow</button>
      <button mat-button routerLink="/admin">Admin</button>
    </mat-toolbar>
    <div style="padding: 20px;">
      <router-outlet></router-outlet>
    </div>
  `,
  styles: [`
    .spacer {
      flex: 1 1 auto;
    }
  `]
})
export class AppComponent {
  title = 'Invoice Processor';
}
