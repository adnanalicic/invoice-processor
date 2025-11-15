import { Component, Inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatCardModule } from '@angular/material/card';

@Component({
  selector: 'app-invoice-details-dialog',
  standalone: true,
  imports: [CommonModule, MatDialogModule, MatCardModule],
  template: `
    <h2 mat-dialog-title>Invoice Extraction Details</h2>
    <mat-dialog-content>
      <mat-card>
        <mat-card-content>
          <p><strong>Invoice Number:</strong> {{ data.invoiceNumber }}</p>
          <p><strong>Invoice Date:</strong> {{ formatDate(data.invoiceDate) }}</p>
          <p><strong>Supplier Name:</strong> {{ data.supplierName }}</p>
          <p><strong>Total Amount:</strong> {{ data.totalAmount }} {{ data.currency }}</p>
        </mat-card-content>
      </mat-card>
    </mat-dialog-content>
    <mat-dialog-actions>
      <button mat-button (click)="close()">Close</button>
    </mat-dialog-actions>
  `
})
export class InvoiceDetailsDialogComponent {
  constructor(
    public dialogRef: MatDialogRef<InvoiceDetailsDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: any
  ) {}

  close() {
    this.dialogRef.close();
  }

  formatDate(dateString: string): string {
    return new Date(dateString).toLocaleDateString();
  }
}
