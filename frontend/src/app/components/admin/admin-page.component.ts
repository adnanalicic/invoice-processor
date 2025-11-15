import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { MatButtonModule } from '@angular/material/button';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { ApiService, IntegrationEndpoint } from '../../services/api.service';

@Component({
  selector: 'app-admin-page',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatCardModule,
    MatFormFieldModule,
    MatInputModule,
    MatSlideToggleModule,
    MatButtonModule,
    MatSnackBarModule
  ],
  template: `
    <div class="admin-container">
      <mat-card>
        <mat-card-header>
          <mat-card-title>Email Source Settings</mat-card-title>
        </mat-card-header>
        <mat-card-content>
          <form [formGroup]="emailForm" (ngSubmit)="saveEmailSettings()">
            <div class="grid">
              <mat-form-field appearance="outline">
                <mat-label>Host</mat-label>
                <input matInput formControlName="host" required>
              </mat-form-field>
              <mat-form-field appearance="outline">
                <mat-label>Port</mat-label>
                <input matInput formControlName="port" required type="number">
              </mat-form-field>
              <mat-form-field appearance="outline">
                <mat-label>Username</mat-label>
                <input matInput formControlName="username" required>
              </mat-form-field>
              <mat-form-field appearance="outline">
                <mat-label>Password</mat-label>
                <input matInput formControlName="password" type="password" required>
              </mat-form-field>
              <mat-form-field appearance="outline">
                <mat-label>Folder</mat-label>
                <input matInput formControlName="folder">
              </mat-form-field>
            </div>
            <mat-slide-toggle formControlName="ssl">Use SSL</mat-slide-toggle>
            <div class="actions">
              <button mat-raised-button color="primary" type="submit" [disabled]="emailForm.invalid || savingEmail">
                {{ savingEmail ? 'Saving...' : 'Save Email Settings' }}
              </button>
            </div>
          </form>
        </mat-card-content>
      </mat-card>

      <mat-card>
        <mat-card-header>
          <mat-card-title>Storage Target Settings</mat-card-title>
        </mat-card-header>
        <mat-card-content>
          <form [formGroup]="storageForm" (ngSubmit)="saveStorageSettings()">
            <div class="grid">
              <mat-form-field appearance="outline">
                <mat-label>Endpoint</mat-label>
                <input matInput formControlName="endpoint" required>
              </mat-form-field>
              <mat-form-field appearance="outline">
                <mat-label>Access Key</mat-label>
                <input matInput formControlName="accessKey" required>
              </mat-form-field>
              <mat-form-field appearance="outline">
                <mat-label>Secret Key</mat-label>
                <input matInput formControlName="secretKey" type="password" required>
              </mat-form-field>
              <mat-form-field appearance="outline">
                <mat-label>Bucket</mat-label>
                <input matInput formControlName="bucket" required>
              </mat-form-field>
            </div>
            <div class="actions">
              <button mat-raised-button color="primary" type="submit" [disabled]="storageForm.invalid || savingStorage">
                {{ savingStorage ? 'Saving...' : 'Save Storage Settings' }}
              </button>
            </div>
          </form>
        </mat-card-content>
      </mat-card>
    </div>
  `,
  styles: [`
    .admin-container {
      max-width: 900px;
      margin: 20px auto;
      display: flex;
      flex-direction: column;
      gap: 20px;
    }

    .grid {
      display: grid;
      grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
      gap: 16px;
    }

    .actions {
      margin-top: 16px;
      display: flex;
      justify-content: flex-end;
    }
  `]
})
export class AdminPageComponent implements OnInit {
  emailForm: FormGroup;
  storageForm: FormGroup;
  savingEmail = false;
  savingStorage = false;

  constructor(
    private fb: FormBuilder,
    private apiService: ApiService,
    private snackBar: MatSnackBar
  ) {
    this.emailForm = this.fb.group({
      host: ['', Validators.required],
      port: [993, Validators.required],
      username: ['', Validators.required],
      password: ['', Validators.required],
      folder: ['INBOX'],
      ssl: [true]
    });

    this.storageForm = this.fb.group({
      endpoint: ['', Validators.required],
      accessKey: ['', Validators.required],
      secretKey: ['', Validators.required],
      bucket: ['', Validators.required]
    });
  }

  ngOnInit(): void {
    this.apiService.getIntegrationEndpoints().subscribe(endpoints => {
      const byType = this.mapEndpoints(endpoints);
      this.patchEmailForm(byType['EMAIL_SOURCE']);
      this.patchStorageForm(byType['STORAGE_TARGET']);
    });
  }

  private mapEndpoints(endpoints: IntegrationEndpoint[]): Record<string, IntegrationEndpoint> {
    return endpoints.reduce((acc, endpoint) => {
      acc[endpoint.type] = endpoint;
      return acc;
    }, {} as Record<string, IntegrationEndpoint>);
  }

  private patchEmailForm(endpoint?: IntegrationEndpoint) {
    if (!endpoint) {
      return;
    }
    this.emailForm.patchValue({
      host: endpoint.settings['host'] || '',
      port: Number(endpoint.settings['port'] || 993),
      username: endpoint.settings['username'] || '',
      password: endpoint.settings['password'] || '',
      folder: endpoint.settings['folder'] || 'INBOX',
      ssl: endpoint.settings['ssl'] !== 'false'
    });
  }

  private patchStorageForm(endpoint?: IntegrationEndpoint) {
    if (!endpoint) {
      return;
    }
    this.storageForm.patchValue({
      endpoint: endpoint.settings['endpoint'] || '',
      accessKey: endpoint.settings['accessKey'] || '',
      secretKey: endpoint.settings['secretKey'] || '',
      bucket: endpoint.settings['bucket'] || ''
    });
  }

  saveEmailSettings() {
    if (this.emailForm.invalid) {
      return;
    }
    this.savingEmail = true;
    const settings = {
      host: this.emailForm.value.host,
      port: String(this.emailForm.value.port),
      username: this.emailForm.value.username,
      password: this.emailForm.value.password,
      folder: this.emailForm.value.folder,
      ssl: String(this.emailForm.value.ssl)
    };

    this.apiService.saveIntegrationEndpoint('EMAIL_SOURCE', {
      name: 'Email Source',
      settings
    }).subscribe({
      next: () => {
        this.savingEmail = false;
        this.snackBar.open('Email settings saved', 'Close', { duration: 2500 });
      },
      error: (err) => {
        this.savingEmail = false;
        this.snackBar.open(err.error?.message || 'Failed to save email settings', 'Close', { duration: 3000 });
      }
    });
  }

  saveStorageSettings() {
    if (this.storageForm.invalid) {
      return;
    }
    this.savingStorage = true;
    const settings = {
      endpoint: this.storageForm.value.endpoint,
      accessKey: this.storageForm.value.accessKey,
      secretKey: this.storageForm.value.secretKey,
      bucket: this.storageForm.value.bucket
    };

    this.apiService.saveIntegrationEndpoint('STORAGE_TARGET', {
      name: 'Storage Target',
      settings
    }).subscribe({
      next: () => {
        this.savingStorage = false;
        this.snackBar.open('Storage settings saved', 'Close', { duration: 2500 });
      },
      error: (err) => {
        this.savingStorage = false;
        this.snackBar.open(err.error?.message || 'Failed to save storage settings', 'Close', { duration: 3000 });
      }
    });
  }
}
