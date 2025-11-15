import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, FormArray, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatSelectModule } from '@angular/material/select';
import { MatIconModule } from '@angular/material/icon';
import { ApiService } from '../../services/api.service';

@Component({
  selector: 'app-simulate-email',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatCardModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatSelectModule,
    MatIconModule
  ],
  template: `
    <mat-card>
      <mat-card-header>
        <mat-card-title>Simulate Email</mat-card-title>
      </mat-card-header>
      <mat-card-content>
        <form [formGroup]="emailForm" (ngSubmit)="onSubmit()">
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
            <textarea matInput formControlName="body" rows="4"></textarea>
          </mat-form-field>

          <div formArrayName="attachments">
            <div *ngFor="let attachment of attachments.controls; let i = index" [formGroupName]="i">
              <mat-card style="margin-bottom: 10px;">
                <mat-card-content>
                  <mat-form-field appearance="outline" style="width: 48%; margin-right: 2%;">
                    <mat-label>Filename</mat-label>
                    <input matInput formControlName="filename" required>
                  </mat-form-field>

                  <mat-form-field appearance="outline" style="width: 48%;">
                    <mat-label>Type</mat-label>
                    <mat-select formControlName="type" required>
                      <mat-option value="PDF_ATTACHMENT">PDF Attachment</mat-option>
                      <mat-option value="IMAGE_ATTACHMENT">Image Attachment</mat-option>
                      <mat-option value="EMAIL_BODY">Email Body</mat-option>
                      <mat-option value="OTHER_ATTACHMENT">Other Attachment</mat-option>
                    </mat-select>
                  </mat-form-field>

                  <mat-form-field appearance="outline" style="width: 100%;">
                    <mat-label>Content Reference</mat-label>
                    <input matInput formControlName="contentReference" required>
                  </mat-form-field>

                  <button mat-icon-button type="button" (click)="removeAttachment(i)">
                    <mat-icon>delete</mat-icon>
                  </button>
                </mat-card-content>
              </mat-card>
            </div>
          </div>

          <button mat-raised-button type="button" color="primary" (click)="addAttachment()">
            Add Attachment
          </button>

          <div style="margin-top: 20px;">
            <button mat-raised-button type="submit" color="primary" [disabled]="!emailForm.valid">
              Simulate Email
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
  `]
})
export class SimulateEmailComponent {
  emailForm: FormGroup;

  constructor(
    private fb: FormBuilder,
    private apiService: ApiService,
    private router: Router
  ) {
    this.emailForm = this.fb.group({
      from: ['supplier@example.com', Validators.required],
      to: ['invoices@mycompany.com', Validators.required],
      subject: ['Invoice 123', Validators.required],
      body: ['Hello, see attached invoice'],
      attachments: this.fb.array([])
    });
  }

  get attachments(): FormArray {
    return this.emailForm.get('attachments') as FormArray;
  }

  addAttachment() {
    const attachmentGroup = this.fb.group({
      filename: ['', Validators.required],
      type: ['PDF_ATTACHMENT', Validators.required],
      contentReference: ['', Validators.required]
    });
    this.attachments.push(attachmentGroup);
  }

  removeAttachment(index: number) {
    this.attachments.removeAt(index);
  }

  onSubmit() {
    if (this.emailForm.valid) {
      const formValue = this.emailForm.value;
      this.apiService.simulateEmail({
        from: formValue.from,
        to: formValue.to,
        subject: formValue.subject,
        body: formValue.body,
        attachments: formValue.attachments
      }).subscribe(response => {
        if (response.stackId) {
          this.router.navigate(['/stacks', response.stackId]);
        }
      });
    }
  }

  goBack() {
    this.router.navigate(['/stacks']);
  }
}
