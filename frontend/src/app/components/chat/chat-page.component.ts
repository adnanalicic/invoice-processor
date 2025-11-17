import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatInputModule } from '@angular/material/input';
import { MatIconModule } from '@angular/material/icon';
import { ApiService } from '../../services/api.service';
import { StackDetailsResponse } from '../../services/api.service';

interface ChatMessage {
  role: 'user' | 'assistant';
  content: string;
}

@Component({
  selector: 'app-chat-page',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    MatCardModule,
    MatButtonModule,
    MatInputModule,
    MatIconModule
  ],
  template: `
    <mat-card class="chat-card">
      <mat-card-header>
        <mat-card-title>LLM Chat</mat-card-title>
        <mat-card-subtitle>
          Chat with the locally running model (OpenAI-compatible endpoint).
        </mat-card-subtitle>
      </mat-card-header>
      <mat-card-content>
        <div class="pdf-upload">
          <label class="pdf-label">PDF for Q&A (optional):</label>
          <input
            type="file"
            accept="application/pdf"
            (change)="onFileSelected($event)"
            [disabled]="uploading"
          />
          <span class="pdf-status" *ngIf="currentDocumentName">
            Using: {{ currentDocumentName }}
          </span>
          <span class="pdf-status" *ngIf="uploading">
            Uploading PDF...
          </span>
        </div>
        <div class="chat-window">
          <div *ngFor="let msg of messages" class="chat-row" [class.user]="msg.role === 'user'" [class.assistant]="msg.role === 'assistant'">
            <div class="chat-bubble">
              <div class="chat-meta">
                <mat-icon class="avatar" *ngIf="msg.role === 'assistant'">smart_toy</mat-icon>
                <mat-icon class="avatar" *ngIf="msg.role === 'user'">person</mat-icon>
                <span class="role-label">{{ msg.role === 'user' ? 'You' : 'Assistant' }}</span>
              </div>
              <div class="chat-content">
                {{ msg.content }}
              </div>
            </div>
          </div>
          <div *ngIf="messages.length === 0" class="empty-hint">
            Start chatting by sending a message below.
          </div>
        </div>
        <div class="input-row">
          <textarea
            matInput
            [(ngModel)]="input"
            placeholder="Type your message..."
            rows="3"
            (keydown.enter)="onEnter($event)"></textarea>
          <button mat-raised-button color="primary" (click)="send()" [disabled]="!input.trim() || sending">
            {{ sending ? 'Sending...' : 'Send' }}
          </button>
        </div>
      </mat-card-content>
    </mat-card>
  `,
  styles: [`
    .chat-card {
      max-width: 900px;
      margin: 0 auto;
    }

    .pdf-upload {
      display: flex;
      align-items: center;
      gap: 8px;
      margin-bottom: 12px;
      font-size: 13px;
    }

    .pdf-label {
      font-weight: 500;
    }

    .pdf-status {
      color: rgba(0,0,0,0.7);
      font-style: italic;
    }

    .chat-window {
      border: 1px solid #e0e0e0;
      border-radius: 4px;
      padding: 12px;
      max-height: 500px;
      overflow-y: auto;
      background-color: #fafafa;
    }

    .chat-row {
      display: flex;
      margin-bottom: 8px;
    }

    .chat-row.user {
      justify-content: flex-end;
    }

    .chat-row.assistant {
      justify-content: flex-start;
    }

    .chat-bubble {
      max-width: 75%;
      background-color: #fff;
      border-radius: 8px;
      padding: 8px 10px;
      box-shadow: 0 1px 2px rgba(0,0,0,0.1);
    }

    .chat-row.user .chat-bubble {
      background-color: #e3f2fd;
    }

    .chat-meta {
      display: flex;
      align-items: center;
      margin-bottom: 4px;
      font-size: 12px;
      color: rgba(0,0,0,0.6);
    }

    .avatar {
      font-size: 16px;
      margin-right: 4px;
    }

    .chat-content {
      white-space: pre-wrap;
      font-size: 14px;
    }

    .input-row {
      margin-top: 12px;
      display: flex;
      gap: 8px;
      align-items: flex-end;
    }

    textarea[matInput] {
      flex: 1 1 auto;
      resize: vertical;
    }

    .empty-hint {
      font-size: 13px;
      color: rgba(0,0,0,0.6);
      text-align: center;
      padding: 16px 0;
    }
  `]
})
export class ChatPageComponent {
  messages: ChatMessage[] = [];
  input = '';
  sending = false;
  uploading = false;
  currentDocumentId: string | null = null;
  currentDocumentName: string | null = null;

  constructor(private apiService: ApiService) {}

  onEnter(event: KeyboardEvent | Event): void {
    const keyboardEvent = event as KeyboardEvent;
    if (!keyboardEvent.shiftKey) {
      event.preventDefault();
      this.send();
    }
  }

  send(): void {
    const trimmed = this.input.trim();
    if (!trimmed) {
      return;
    }
    const userMessage: ChatMessage = { role: 'user', content: trimmed };
    this.messages.push(userMessage);
    this.input = '';
    this.sending = true;

    const payload: { documentId?: string; messages: { role: string; content: string }[] } = {
      messages: this.messages.map(m => ({ role: m.role, content: m.content }))
    };

    if (this.currentDocumentId) {
      payload.documentId = this.currentDocumentId;
    }

    this.apiService.sendChatMessage(payload).subscribe({
      next: (res) => {
        if (res && res.reply) {
          this.messages.push({ role: 'assistant', content: res.reply });
        }
        this.sending = false;
      },
      error: (err) => {
        this.messages.push({ role: 'assistant', content: err.error?.reply || 'Error talking to model.' });
        this.sending = false;
      }
    });
  }

  onFileSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    if (!input.files || input.files.length === 0) {
      return;
    }

    const file = input.files[0];
    input.value = '';
    this.uploadPdfForChat(file);
  }

  private uploadPdfForChat(file: File): void {
    this.uploading = true;
    this.currentDocumentId = null;
    this.currentDocumentName = file.name;

    const formData = new FormData();
    formData.append('from', 'chat-upload@example.com');
    formData.append('to', 'chat-upload@example.com');
    formData.append('subject', file.name);
    formData.append('attachments', file, file.name);

    this.apiService.createManualStack(formData).subscribe({
      next: (response) => {
        if (!response.stackId) {
          this.addSystemMessage('Failed to create stack for uploaded PDF.');
          this.uploading = false;
          return;
        }

        this.apiService.getStackDetails(response.stackId).subscribe({
          next: (details: StackDetailsResponse) => {
            const document =
              details.documents.find(d => d.filename === file.name) ||
              details.documents.find(d => d.type === 'PDF_ATTACHMENT') ||
              details.documents[0];

            if (!document) {
              this.addSystemMessage('No document found for uploaded PDF.');
              this.uploading = false;
              return;
            }

            this.currentDocumentId = document.id;
            this.addSystemMessage(`PDF "${file.name}" is ready for questions.`);
            this.uploading = false;
          },
          error: () => {
            this.addSystemMessage('Failed to load details for uploaded PDF.');
            this.uploading = false;
          }
        });
      },
      error: () => {
        this.addSystemMessage('Error uploading PDF for chat.');
        this.uploading = false;
      }
    });
  }

  private addSystemMessage(text: string): void {
    this.messages.push({ role: 'assistant', content: text });
  }
}
