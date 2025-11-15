import { Component, OnInit } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { CommonModule } from '@angular/common';
import { MatTableModule } from '@angular/material/table';
import { MatButtonModule } from '@angular/material/button';
import { MatChipsModule } from '@angular/material/chips';
import { MatIconModule } from '@angular/material/icon';
import { MatCardModule } from '@angular/material/card';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { ApiService, StackSummary } from '../../services/api.service';

@Component({
  selector: 'app-stacks-list',
  standalone: true,
  imports: [
    CommonModule,
    RouterLink,
    MatTableModule,
    MatButtonModule,
    MatChipsModule,
    MatIconModule,
    MatCardModule,
    MatSnackBarModule
  ],
  template: `
    <mat-card>
      <mat-card-header>
        <mat-card-title>Stacks</mat-card-title>
      </mat-card-header>
      <mat-card-content>
        <table mat-table [dataSource]="stacks" class="mat-elevation-z8">
          <ng-container matColumnDef="id">
            <th mat-header-cell *matHeaderCellDef>ID</th>
            <td mat-cell *matCellDef="let stack">{{ stack.id.substring(0, 8) }}</td>
          </ng-container>

          <ng-container matColumnDef="subject">
            <th mat-header-cell *matHeaderCellDef>Subject</th>
            <td mat-cell *matCellDef="let stack">{{ stack.subject }}</td>
          </ng-container>

          <ng-container matColumnDef="fromAddress">
            <th mat-header-cell *matHeaderCellDef>From</th>
            <td mat-cell *matCellDef="let stack">{{ stack.fromAddress }}</td>
          </ng-container>

          <ng-container matColumnDef="receivedAt">
            <th mat-header-cell *matHeaderCellDef>Received At</th>
            <td mat-cell *matCellDef="let stack">{{ formatDate(stack.receivedAt) }}</td>
          </ng-container>

          <ng-container matColumnDef="status">
            <th mat-header-cell *matHeaderCellDef>Status</th>
            <td mat-cell *matCellDef="let stack">
              <mat-chip [color]="getStatusColor(stack.status)">
                {{ stack.status }}
              </mat-chip>
            </td>
          </ng-container>

          <ng-container matColumnDef="documents">
            <th mat-header-cell *matHeaderCellDef>Documents</th>
            <td mat-cell *matCellDef="let stack">
              {{ stack.documentCount }} ({{ stack.invoiceCount }} invoices)
            </td>
          </ng-container>

          <ng-container matColumnDef="actions">
            <th mat-header-cell *matHeaderCellDef>Actions</th>
            <td mat-cell *matCellDef="let stack">
              <button mat-button color="primary" (click)="viewDetails(stack.id)">
                View Details
              </button>
            </td>
          </ng-container>

          <tr mat-header-row *matHeaderRowDef="displayedColumns"></tr>
          <tr mat-row *matRowDef="let row; columns: displayedColumns" 
              (click)="viewDetails(row.id)" style="cursor: pointer;"></tr>
        </table>
      </mat-card-content>
      <mat-card-actions>
        <button mat-raised-button color="primary" routerLink="/simulate">
          Simulate Email
        </button>
        <button mat-raised-button color="primary" routerLink="/manual-upload">
          Manual Upload
        </button>
        <button mat-raised-button color="accent" (click)="importEmails()" [disabled]="importing">
          {{ importing ? 'Importing...' : 'Import E-Mails' }}
        </button>
      </mat-card-actions>
    </mat-card>
  `,
  styles: [`
    table {
      width: 100%;
    }
    mat-card {
      max-width: 1400px;
      margin: 20px auto;
    }
  `]
})
export class StacksListComponent implements OnInit {
  stacks: StackSummary[] = [];
  displayedColumns: string[] = ['id', 'subject', 'fromAddress', 'receivedAt', 'status', 'documents', 'actions'];
  importing: boolean = false;

  constructor(
    private apiService: ApiService,
    private router: Router,
    private snackBar: MatSnackBar
  ) {}

  ngOnInit() {
    this.loadStacks();
  }

  loadStacks() {
    this.apiService.getStacks().subscribe(response => {
      this.stacks = response.stacks;
    });
  }

  viewDetails(stackId: string) {
    this.router.navigate(['/stacks', stackId]);
  }

  formatDate(dateString: string): string {
    return new Date(dateString).toLocaleString();
  }

  getStatusColor(status: string): string {
    switch (status) {
      case 'PROCESSED': return 'primary';
      case 'PROCESSING': return 'accent';
      case 'ERROR': return 'warn';
      default: return '';
    }
  }

  importEmails() {
    this.importing = true;
    this.apiService.importEmails().subscribe({
      next: (response) => {
        this.importing = false;
        this.snackBar.open(
          `Imported ${response.stacksCreated} stacks from ${response.emailsFound} emails. Created ${response.documentsCreated} documents.`,
          'Close',
          { duration: 5000 }
        );
        this.loadStacks(); // Refresh the list
      },
      error: (error) => {
        this.importing = false;
        this.snackBar.open(
          `Error importing emails: ${error.error?.message || error.message}`,
          'Close',
          { duration: 5000 }
        );
      }
    });
  }
}
