import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatListModule } from '@angular/material/list';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { ApiService, ManualStackResponse } from '../../services/api.service';

@Component({
  selector: 'app-manual-stack',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatCardModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatIconModule,
    MatListModule,
    MatSnackBarModule
  ],
  template: `
    <mat-card>
      <mat-card-header>
        <mat-card-title>Create Stack Manually</mat-card-title>
      </mat-card-header>
      <mat-card-content>
        <form [formGroup]="manualForm" (ngSubmit)="onSubmit()">
          <mat-form-field appearance="outline" style="width: 100%;">
            <mat-label>From</mat-label>
            <input matInput formControlName="from" required>
          </mat-form-field>

          <mat-form-field appearance="outline" style="width: 100%;">
            <mat-label>To</mat-label>
            <input matInput formControlName="to" required>
          </mat-form-field>

          <mat-form-field appearance="outline" style="width: 100%;">
            <mat-label>Subject</mat-label>
            <input matInput formControlName="subject" required>
          </mat-form-field>

          <mat-form-field appearance="outline" style="width: 100%;">
            <mat-label>Body</mat-label>
            <textarea matInput rows="5" formControlName="body"></textarea>
          </mat-form-field>

          <div style="margin: 10px 0;">
            <label for="fileInput">Attachments</label>
            <input id="fileInput" type="file" multiple (change)="onFilesSelected($event)">
          </div>

          <mat-list *ngIf="selectedFiles.length > 0">
            <mat-list-item *ngFor="let file of selectedFiles; index as i">
              <span matListItemTitle>{{ file.name }}</span>
              <button mat-icon-button color="warn" type="button" (click)="removeFile(i)">
                <mat-icon>delete</mat-icon>
              </button>
            </mat-list-item>
          </mat-list>

          <div style="margin-top: 20px;">
            <button mat-raised-button color="primary" type="submit" [disabled]="manualForm.invalid || loading">
              {{ loading ? 'Creating...' : 'Create Stack' }}
            </button>
            <button mat-button type="button" (click)="goBack()">Cancel</button>
          </div>
        </form>
      </mat-card-content>
    </mat-card>
  `,
  styles: [`
    mat-card {
      max-width: 800px;
      margin: 20px auto;
    }
    form {
      display: flex;
      flex-direction: column;
    }
    mat-list-item {
      display: flex;
      justify-content: space-between;
      align-items: center;
    }
  `]
})
export class ManualStackComponent {
  manualForm: FormGroup;
  selectedFiles: File[] = [];
  loading = false;

  constructor(
    private fb: FormBuilder,
    private apiService: ApiService,
    private router: Router,
    private snackBar: MatSnackBar
  ) {
    this.manualForm = this.fb.group({
      from: ['supplier@example.com', Validators.required],
      to: ['invoices@mycompany.com', Validators.required],
      subject: ['', Validators.required],
      body: ['']
    });
  }

  onFilesSelected(event: Event) {
    const input = event.target as HTMLInputElement;
    if (input.files) {
      for (let i = 0; i < input.files.length; i++) {
        this.selectedFiles.push(input.files.item(i)!);
      }
      input.value = '';
    }
  }

  removeFile(index: number) {
    this.selectedFiles.splice(index, 1);
  }

  onSubmit() {
    if (this.manualForm.invalid) {
      return;
    }

    const formData = new FormData();
    formData.append('from', this.manualForm.value.from);
    formData.append('to', this.manualForm.value.to);
    formData.append('subject', this.manualForm.value.subject);
    if (this.manualForm.value.body) {
      formData.append('body', this.manualForm.value.body);
    }
    this.selectedFiles.forEach(file => {
      formData.append('attachments', file, file.name);
    });

    this.loading = true;
    this.apiService.createManualStack(formData).subscribe({
      next: (response: ManualStackResponse) => {
        this.loading = false;
        this.snackBar.open('Stack created successfully', 'Close', { duration: 3000 });
        if (response.stackId) {
          this.router.navigate(['/stacks', response.stackId]);
        } else {
          this.router.navigate(['/stacks']);
        }
      },
      error: (error) => {
        this.loading = false;
        this.snackBar.open(
          error.error?.message || 'Failed to create stack',
          'Close',
          { duration: 4000 }
        );
      }
    });
  }

  goBack() {
    this.router.navigate(['/stacks']);
  }
}
