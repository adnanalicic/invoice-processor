import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface StackSummary {
  id: string;
  subject: string;
  fromAddress: string;
  receivedAt: string;
  status: string;
  documentCount: number;
  invoiceCount: number;
}

export interface StackListResponse {
  stacks: StackSummary[];
  total: number;
  page: number;
  size: number;
}

export interface InvoiceDetails {
  id: string;
  invoiceNumber: string;
  invoiceDate: string;
  supplierName: string;
  totalAmount: number;
  currency: string;
}

export interface DocumentDetails {
  id: string;
  type: string;
  filename: string | null;
  classification: string;
  extractionStatus: string;
  invoice: InvoiceDetails | null;
}

export interface StackDetailsResponse {
  id: string;
  subject: string;
  fromAddress: string;
  toAddress: string;
  receivedAt: string;
  status: string;
  documents: DocumentDetails[];
}

export interface SimulateEmailRequest {
  from: string;
  to: string;
  subject: string;
  body: string;
  attachments: AttachmentRequest[];
}

export interface AttachmentRequest {
  filename: string;
  type: string;
  contentReference: string;
}

@Injectable({
  providedIn: 'root'
})
export class ApiService {
  // Use relative path - nginx will proxy /api to backend:8080
  private baseUrl = '/api';

  constructor(private http: HttpClient) {}

  getStacks(page: number = 0, size: number = 20): Observable<StackListResponse> {
    return this.http.get<StackListResponse>(`${this.baseUrl}/stacks?page=${page}&size=${size}`);
  }

  getStackDetails(stackId: string): Observable<StackDetailsResponse> {
    return this.http.get<StackDetailsResponse>(`${this.baseUrl}/stacks/${stackId}`);
  }

  simulateEmail(request: SimulateEmailRequest): Observable<any> {
    return this.http.post(`${this.baseUrl}/stacks/simulateEmail`, request);
  }

  reextractDocument(documentId: string): Observable<any> {
    return this.http.post(`${this.baseUrl}/documents/${documentId}/reextract`, {});
  }

  importEmails(): Observable<ImportEmailsResponse> {
    return this.http.post<ImportEmailsResponse>(`${this.baseUrl}/stacks/importEmails`, {});
  }

  createManualStack(formData: FormData): Observable<ManualStackResponse> {
    return this.http.post<ManualStackResponse>(`${this.baseUrl}/stacks/manualUpload`, formData);
  }

  getIntegrationEndpoints(): Observable<IntegrationEndpoint[]> {
    return this.http.get<IntegrationEndpoint[]>(`${this.baseUrl}/admin/endpoints`);
  }

  getEmailSources(): Observable<IntegrationEndpoint[]> {
    return this.http.get<IntegrationEndpoint[]>(`${this.baseUrl}/admin/email-sources`);
  }

  createEmailSource(payload: { name: string; settings: Record<string, string> }): Observable<IntegrationEndpoint> {
    return this.http.post<IntegrationEndpoint>(`${this.baseUrl}/admin/email-sources`, payload);
  }

  updateEmailSource(
    id: string,
    payload: { name: string; settings: Record<string, string> }
  ): Observable<IntegrationEndpoint> {
    return this.http.put<IntegrationEndpoint>(`${this.baseUrl}/admin/email-sources/${id}`, payload);
  }

  deleteEmailSource(id: string): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/admin/email-sources/${id}`);
  }

  saveIntegrationEndpoint(
    type: string,
    payload: { name: string; settings: Record<string, string> }
  ): Observable<IntegrationEndpoint> {
    return this.http.put<IntegrationEndpoint>(`${this.baseUrl}/admin/endpoints/${type}`, payload);
  }
}

export interface ImportEmailsResponse {
  emailsFound: number;
  stacksCreated: number;
  documentsCreated: number;
  errors: number;
  message: string;
}

export interface ManualStackResponse {
  stackId: string;
  message: string;
}

export interface IntegrationEndpoint {
  id: string;
  name: string;
  type: string;
  settings: Record<string, string>;
}
