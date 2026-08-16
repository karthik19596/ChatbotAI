import { firstValueFrom } from 'rxjs';
import { HttpClient } from '@angular/common/http';
import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';

interface ChatMessage {
  role: 'user' | 'bot';
  content: string;
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

  protected readonly message = signal('');
  protected readonly isLoading = signal(false);
  protected readonly error = signal('');
  protected readonly messages = signal<ChatMessage[]>([
    {
      role: 'bot',
      content: 'Hello! I am your local Ollama chatbot. How can I help?'
    }
  ]);

  protected async sendMessage(): Promise<void> {
    const content = this.message().trim();
    if (!content || this.isLoading()) {
      return;
    }

    this.messages.update((messages) => [...messages, { role: 'user', content }]);
    this.message.set('');
    this.error.set('');
    this.isLoading.set(true);

    try {
      const result = await firstValueFrom(
        this.http.post<ChatResponse>(this.apiUrl, { message: content })
      );

      this.messages.update((messages) => [
        ...messages,
        { role: 'bot', content: result.response }
      ]);
    } catch {
      this.error.set(
        'Unable to reach the chatbot. Check that Spring Boot and Ollama are running.'
      );
    } finally {
      this.isLoading.set(false);
    }
  }

  protected clearChat(): void {
    this.messages.set([]);
    this.error.set('');
  }
}
