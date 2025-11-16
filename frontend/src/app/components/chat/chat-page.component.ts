import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatInputModule } from '@angular/material/input';
import { MatIconModule } from '@angular/material/icon';
import { ApiService } from '../../services/api.service';

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

    const payload = {
      messages: this.messages.map(m => ({ role: m.role, content: m.content }))
    };

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
}
