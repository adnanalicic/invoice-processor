import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { ApiService, IntegrationEndpoint } from '../../services/api.service';

@Component({
  selector: 'app-workflow-page',
  standalone: true,
  imports: [
    CommonModule,
    RouterLink,
    MatCardModule,
    MatButtonModule,
    MatIconModule
  ],
  template: `
    <mat-card>
      <mat-card-header>
        <mat-card-title>Workflow Designer</mat-card-title>
        <mat-card-subtitle>
          Visual overview of how email inboxes feed the processor and where results are stored.
        </mat-card-subtitle>
      </mat-card-header>
      <mat-card-content>
        <div class="workflow-grid">
          <div class="column">
            <h3>Email Inputs</h3>
            <p class="hint">
              Each node represents an <code>EMAIL_SOURCE</code> inbox configured in the Admin page.
            </p>
            <button mat-stroked-button color="primary" routerLink="/admin">
              Manage Inputs
            </button>

            <div class="nodes">
              <mat-card *ngFor="let source of emailSources" class="node-card">
                <mat-card-header>
                  <mat-card-title>
                    <mat-icon class="node-icon">mail</mat-icon>
                    {{ source.settings['folder'] || 'INBOX' }}
                  </mat-card-title>
                  <mat-card-subtitle>
                    {{ (source.settings['username'] || 'user') + '@' + (source.settings['host'] || 'host') }}
                  </mat-card-subtitle>
                </mat-card-header>
              </mat-card>
              <div *ngIf="emailSources.length === 0" class="empty-hint">
                No email inputs configured yet. Use <strong>Manage Inputs</strong> to add inboxes.
              </div>
            </div>
          </div>

          <div class="column center-column">
            <h3>Processor</h3>
            <mat-card class="node-card central-node">
              <mat-card-header>
                <mat-card-title>
                  <mat-icon class="node-icon">settings</mat-icon>
                  Invoice Processor
                </mat-card-title>
              </mat-card-header>
              <mat-card-content>
                <ul>
                  <li>Receives emails from mail-import.</li>
                  <li>Stores bodies and attachments in object storage.</li>
                  <li>Runs extraction to build invoice data.</li>
                </ul>
              </mat-card-content>
            </mat-card>
          </div>

          <div class="column">
            <h3>Outputs</h3>
            <p class="hint">
              Output nodes represent where processed documents are persisted.
            </p>
            <button mat-stroked-button color="primary" routerLink="/admin">
              Configure Storage
            </button>

            <div class="nodes">
              <mat-card *ngIf="storageTarget" class="node-card">
                <mat-card-header>
                  <mat-card-title>
                    <mat-icon class="node-icon">cloud_upload</mat-icon>
                    S3 / MinIO
                  </mat-card-title>
                  <mat-card-subtitle>
                    Bucket: {{ storageTarget.settings['bucket'] || 'not set' }}
                  </mat-card-subtitle>
                </mat-card-header>
                <mat-card-content>
                  <div>Endpoint: {{ storageTarget.settings['endpoint'] || 'not set' }}</div>
                </mat-card-content>
              </mat-card>
              <div *ngIf="!storageTarget" class="empty-hint">
                No storage target configured. Use <strong>Configure Storage</strong> to add an output node.
              </div>
            </div>
          </div>
        </div>
      </mat-card-content>
    </mat-card>
  `,
  styles: [`
    .workflow-grid {
      display: grid;
      grid-template-columns: repeat(auto-fit, minmax(260px, 1fr));
      gap: 24px;
      align-items: stretch;
    }

    .column h3 {
      margin-top: 0;
      margin-bottom: 8px;
    }

    .hint {
      font-size: 12px;
      color: rgba(0, 0, 0, 0.6);
      margin-bottom: 8px;
    }

    .nodes {
      margin-top: 12px;
      display: flex;
      flex-direction: column;
      gap: 8px;
    }

    .node-card {
      border-left: 4px solid #2196f3;
    }

    .central-node {
      border-left-color: #9c27b0;
    }

    .node-icon {
      font-size: 20px;
      margin-right: 6px;
      vertical-align: middle;
    }

    .center-column {
      display: flex;
      flex-direction: column;
      justify-content: center;
    }

    .empty-hint {
      font-size: 13px;
      color: rgba(0, 0, 0, 0.6);
      padding-top: 8px;
    }
  `]
})
export class WorkflowPageComponent implements OnInit {
  emailSources: IntegrationEndpoint[] = [];
  storageTarget: IntegrationEndpoint | undefined;

  constructor(private apiService: ApiService) {}

  ngOnInit(): void {
    this.loadEmailSources();
    this.loadStorageTarget();
  }

  private loadEmailSources(): void {
    this.apiService.getEmailSources().subscribe(sources => {
      this.emailSources = sources || [];
    });
  }

  private loadStorageTarget(): void {
    this.apiService.getIntegrationEndpoints().subscribe(endpoints => {
      this.storageTarget = (endpoints || []).find(e => e.type === 'STORAGE_TARGET');
    });
  }
}
