import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { CommonModule } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatTableModule } from '@angular/material/table';
import { MatButtonModule } from '@angular/material/button';
import { MatChipsModule } from '@angular/material/chips';
import { MatDialogModule, MatDialog } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';
import { DomSanitizer, SafeResourceUrl } from '@angular/platform-browser';
import { ApiService, StackDetailsResponse, DocumentDetails } from '../../services/api.service';
import { InvoiceDetailsDialogComponent } from '../invoice-details-dialog/invoice-details-dialog.component';

@Component({
  selector: 'app-stack-detail',
  standalone: true,
  imports: [
    CommonModule,
    MatCardModule,
    MatTableModule,
    MatButtonModule,
    MatChipsModule,
    MatDialogModule,
    MatIconModule
  ],
  template: `
    <div *ngIf="stack">
      <mat-card>
        <mat-card-header>
          <mat-card-title>Stack Details</mat-card-title>
        </mat-card-header>
        <mat-card-content>
          <p><strong>Subject:</strong> {{ stack.subject }}</p>
          <p><strong>From:</strong> {{ stack.fromAddress }}</p>
          <p><strong>To:</strong> {{ stack.toAddress }}</p>
          <p><strong>Received At:</strong> {{ formatDate(stack.receivedAt) }}</p>
          <p><strong>Status:</strong> 
            <mat-chip [color]="getStatusColor(stack.status)">
              {{ stack.status }}
            </mat-chip>
          </p>
          <div class="status-timeline">
            <div class="timeline-step" *ngFor="let step of statusSteps; let i = index">
              <div 
                class="step-circle" 
                [class.completed]="isStepCompleted(step.key)"
                [class.active]="stack.status === step.key"
              >
                <mat-icon *ngIf="isStepCompleted(step.key)">check</mat-icon>
                <span *ngIf="!isStepCompleted(step.key)">{{ i + 1 }}</span>
              </div>
              <div class="step-label">{{ step.label }}</div>
              <div class="step-connector" *ngIf="i < statusSteps.length - 1"
                [class.completed]="isStepCompleted(statusSteps[i + 1].key)">
              </div>
            </div>
          </div>
        </mat-card-content>
      </mat-card>

      <mat-card style="margin-top: 20px;">
        <mat-card-header>
          <mat-card-title>Documents</mat-card-title>
        </mat-card-header>
        <mat-card-content>
          <div class="documents-layout">
            <div class="documents-table">
              <table mat-table [dataSource]="stack.documents" class="mat-elevation-z8">
                <ng-container matColumnDef="id">
                  <th mat-header-cell *matHeaderCellDef>ID</th>
                  <td mat-cell *matCellDef="let doc">{{ doc.id.substring(0, 8) }}</td>
                </ng-container>

                <ng-container matColumnDef="type">
                  <th mat-header-cell *matHeaderCellDef>Type</th>
                  <td mat-cell *matCellDef="let doc">{{ doc.type }}</td>
                </ng-container>

                <ng-container matColumnDef="filename">
                  <th mat-header-cell *matHeaderCellDef>Filename</th>
                  <td mat-cell *matCellDef="let doc">{{ doc.filename || '-' }}</td>
                </ng-container>

                <ng-container matColumnDef="classification">
                  <th mat-header-cell *matHeaderCellDef>Classification</th>
                  <td mat-cell *matCellDef="let doc">
                    <mat-chip>{{ doc.classification }}</mat-chip>
                  </td>
                </ng-container>

                <ng-container matColumnDef="extractionStatus">
                  <th mat-header-cell *matHeaderCellDef>Extraction Status</th>
                  <td mat-cell *matCellDef="let doc">
                    <mat-chip [color]="getExtractionStatusColor(doc.extractionStatus)">
                      {{ doc.extractionStatus }}
                    </mat-chip>
                  </td>
                </ng-container>

                <ng-container matColumnDef="actions">
                  <th mat-header-cell *matHeaderCellDef>Actions</th>
                  <td mat-cell *matCellDef="let doc">
                    <button mat-button color="primary" 
                            *ngIf="doc.invoice" 
                            (click)="viewInvoice(doc.invoice)">
                      View Extraction
                    </button>
                    <button mat-button color="accent" 
                            (click)="retryExtraction(doc.id)">
                      Retry Extraction
                    </button>
                    <button mat-button 
                            *ngIf="canPreview(doc)" 
                            (click)="previewDocument(doc)">
                      Preview
                    </button>
                  </td>
                </ng-container>

                <tr mat-header-row *matHeaderRowDef="displayedColumns"></tr>
                <tr mat-row *matRowDef="let row; columns: displayedColumns"></tr>
              </table>
            </div>

            <div class="document-preview">
              <ng-container *ngIf="selectedDocument && previewUrl; else noPreview">
                <h3>Preview: {{ selectedDocument.filename || selectedDocument.type }}</h3>
                <div class="preview-meta">
                  <div>
                    <strong>Classification:</strong>
                    {{ selectedDocument.classification || 'UNKNOWN' }}
                  </div>
                  <ng-container *ngIf="selectedDocument.invoice; else notInvoice">
                    <div>
                      <strong>Creditor:</strong>
                      {{ selectedDocument.invoice?.supplierName }}
                    </div>
                    <div>
                      <strong>Amount:</strong>
                      {{ selectedDocument.invoice?.totalAmount }}
                      {{ selectedDocument.invoice?.currency }}
                    </div>
                  </ng-container>
                  <ng-template #notInvoice>
                    <div>
                      <strong>Info:</strong>
                      Not an invoice.
                    </div>
                  </ng-template>
                </div>
                <iframe
                  class="preview-frame"
                  [src]="previewUrl"
                  title="Document preview">
                </iframe>
              </ng-container>
              <ng-template #noPreview>
                <p class="preview-hint">
                  Select a PDF or plain text document and click <strong>Preview</strong> to view it here.
                </p>
              </ng-template>
            </div>
          </div>
        </mat-card-content>
      </mat-card>

      <div style="margin-top: 20px;">
        <button mat-raised-button (click)="goBack()">Back to List</button>
      </div>
    </div>
  `,
  styles: [`
    mat-card {
      max-width: 1400px;
      margin: 20px auto;
    }
    table {
      width: 100%;
    }

    .status-timeline {
      display: flex;
      align-items: center;
      margin-top: 20px;
      overflow-x: auto;
    }

    .timeline-step {
      display: flex;
      align-items: center;
      position: relative;
      flex: 1;
      min-width: 120px;
    }

    .step-circle {
      width: 36px;
      height: 36px;
      border-radius: 50%;
      border: 2px solid #ccc;
      display: flex;
      align-items: center;
      justify-content: center;
      font-weight: bold;
      margin-right: 8px;
      color: #757575;
      background-color: #fff;
      flex-shrink: 0;
    }

    .step-circle.completed {
      border-color: #4caf50;
      background-color: #4caf50;
      color: #fff;
    }

    .step-circle.active {
      border-color: #2196f3;
      color: #2196f3;
    }

    .step-label {
      font-size: 12px;
      font-weight: 600;
      color: #616161;
    }

    .step-connector {
      flex: 1;
      height: 4px;
      background: linear-gradient(90deg, #ccc 50%, transparent 50%);
      margin: 0 12px;
    }

    .step-connector.completed {
      background: #4caf50;
    }

    .documents-layout {
      display: grid;
      grid-template-columns: minmax(0, 1.2fr) minmax(0, 1fr);
      gap: 16px;
      align-items: stretch;
    }

    .documents-table {
      min-width: 0;
      overflow: auto;
    }

    .document-preview {
      min-width: 0;
      border-left: 1px solid #e0e0e0;
      padding-left: 16px;
      display: flex;
      flex-direction: column;
    }

    .preview-frame {
      width: 100%;
      height: 500px;
      border: 1px solid #e0e0e0;
      border-radius: 4px;
    }

    .preview-hint {
      font-size: 13px;
      color: rgba(0, 0, 0, 0.6);
      margin-top: 8px;
    }

    .preview-meta {
      font-size: 13px;
      margin-bottom: 8px;
      display: flex;
      flex-direction: column;
      gap: 4px;
    }

    table {
      width: 100%;
    }

    @media (max-width: 960px) {
      .documents-layout {
        grid-template-columns: 1fr;
      }

      .document-preview {
        border-left: none;
        border-top: 1px solid #e0e0e0;
        padding-left: 0;
        padding-top: 16px;
        margin-top: 16px;
      }
    }
  `]
})
export class StackDetailComponent implements OnInit {
  stack: StackDetailsResponse | null = null;
  displayedColumns: string[] = ['id', 'type', 'filename', 'classification', 'extractionStatus', 'actions'];
  statusSteps = [
    { key: 'RECEIVED', label: 'Received' },
    { key: 'PROCESSING', label: 'Processing' },
    { key: 'PROCESSED', label: 'Processed' },
    { key: 'ERROR', label: 'Error' }
  ];

  selectedDocument: DocumentDetails | null = null;
  previewUrl: SafeResourceUrl | null = null;

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private apiService: ApiService,
    private dialog: MatDialog,
    private sanitizer: DomSanitizer
  ) {}

  ngOnInit() {
    const stackId = this.route.snapshot.paramMap.get('id');
    if (stackId) {
      this.loadStackDetails(stackId);
    }
  }

  loadStackDetails(stackId: string) {
    this.apiService.getStackDetails(stackId).subscribe(response => {
      this.stack = response;
    });
  }

  viewInvoice(invoice: any) {
    this.dialog.open(InvoiceDetailsDialogComponent, {
      width: '500px',
      data: invoice
    });
  }

  retryExtraction(documentId: string) {
    this.apiService.reextractDocument(documentId).subscribe(() => {
      if (this.stack) {
        this.loadStackDetails(this.stack.id);
      }
    });
  }

  goBack() {
    this.router.navigate(['/stacks']);
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

  getExtractionStatusColor(status: string): string {
    switch (status) {
      case 'PROCESSED': return 'primary';
      case 'EXTRACTING': return 'accent';
      case 'ERROR': return 'warn';
      default: return '';
    }
  }

  canPreview(doc: DocumentDetails): boolean {
    return doc.type === 'PDF_ATTACHMENT' || doc.type === 'EMAIL_BODY';
  }

  previewDocument(doc: DocumentDetails): void {
    if (!this.canPreview(doc)) {
      return;
    }
    this.selectedDocument = doc;
    const url = `/api/documents/${doc.id}/content`;
    this.previewUrl = this.sanitizer.bypassSecurityTrustResourceUrl(url);
  }
  isStepCompleted(stepKey: string): boolean {
    if (!this.stack) {
      return false;
    }
    const order = this.statusSteps.map(step => step.key);
    const currentIndex = order.indexOf(this.stack.status);
    const stepIndex = order.indexOf(stepKey);
    if (this.stack.status === 'ERROR') {
      return stepKey === 'ERROR';
    }
    return stepIndex <= currentIndex && this.stack.status !== 'ERROR';
  }
}
