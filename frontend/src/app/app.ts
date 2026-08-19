import { Subject, takeUntil } from 'rxjs';
import { HttpClient } from '@angular/common/http';
import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';

interface ChatMessage {
  role: 'user' | 'bot';
  content: string;
  fileName?: string;
}

interface ChatRequest {
  message: string;
  fileName?: string;
  fileContent?: string;
}

interface ChatResponse {
  response: string;
}

@Component({
  selector: 'app-root',
  imports: [FormsModule],
  templateUrl: './app.html',
  styleUrl: './app.css'
})
export class App {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = 'http://localhost:8080/api/chat';
  private readonly cancel$ = new Subject<void>();

  protected readonly message = signal('');
  protected readonly isLoading = signal(false);
  protected readonly error = signal('');
  protected readonly selectedFile = signal<File | null>(null);
  protected readonly selectedFileContent = signal<string | null>(null);
  protected readonly messages = signal<ChatMessage[]>([
    {
      role: 'bot',
      content: 'Hello! I am your local Ollama chatbot. How can I help?'
    }
  ]);

  protected onFileSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0];
    if (!file) {
      return;
    }

    if (!this.isAllowedFile(file.name)) {
      this.error.set('Unsupported file type. Use .txt, .md, .csv, or .json.');
      input.value = '';
      return;
    }

    this.error.set('');
    const reader = new FileReader();
    reader.onload = () => {
      this.selectedFile.set(file);
      this.selectedFileContent.set(String(reader.result ?? ''));
    };
    reader.onerror = () => {
      this.error.set('Could not read the selected file.');
    };
    reader.readAsText(file);
    input.value = '';
  }

  protected removeSelectedFile(): void {
    this.selectedFile.set(null);
    this.selectedFileContent.set(null);
  }

  protected sendMessage(): void {
    const content = this.message().trim();
    const file = this.selectedFile();
    const fileContent = this.selectedFileContent();

    if ((!content && !file) || this.isLoading()) {
      return;
    }

    const payload: ChatRequest = { message: content };
    if (file && fileContent) {
      payload.fileName = file.name;
      payload.fileContent = fileContent;
    }

    this.messages.update((messages) => [
      ...messages,
      {
        role: 'user',
        content: content || '(file attached)',
        fileName: file?.name
      }
    ]);

    this.message.set('');
    this.removeSelectedFile();
    this.error.set('');
    this.isLoading.set(true);

    this.http
      .post<ChatResponse>(this.apiUrl, payload)
      .pipe(takeUntil(this.cancel$))
      .subscribe({
        next: (result) => {
          this.messages.update((messages) => [
            ...messages,
            { role: 'bot', content: result.response }
          ]);
          this.isLoading.set(false);
        },
        error: (err) => {
          // Aborted/cancelled requests surface here too; ignore those.
          if (err?.status === 0 || err?.name === 'AbortError') {
            this.isLoading.set(false);
            return;
          }
          this.error.set(
            'Unable to reach the chatbot. Check that Spring Boot and Ollama are running.'
          );
          this.isLoading.set(false);
        }
      });
  }

  protected cancelRequest(): void {
    this.cancel$.next();
    this.isLoading.set(false);
    this.messages.update((messages) => [
      ...messages,
      { role: 'bot', content: '(Request cancelled.)' }
    ]);
  }

  protected clearChat(): void {
    this.messages.set([]);
    this.error.set('');
    this.removeSelectedFile();
  }

  private isAllowedFile(name: string): boolean {
    const allowed = ['.txt', '.md', '.csv', '.json'];
    const lower = name.toLowerCase();
    return allowed.some((ext) => lower.endsWith(ext));
  }
}
