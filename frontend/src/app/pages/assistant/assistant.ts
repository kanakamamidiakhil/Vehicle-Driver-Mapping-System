import { Component, ElementRef, inject, signal, viewChild } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ApiService, errorMessage } from '../../core/api.service';
import { AiStatus, AskResponse, ChatTurn } from '../../core/models';

interface Message {
  role: 'user' | 'assistant';
  text: string;
  meta?: AskResponse;
  error?: boolean;
}

@Component({
  selector: 'app-assistant',
  imports: [FormsModule],
  template: `
    <h1>✨ Fleet AI assistant</h1>
    <p class="muted">
      Ask about vehicles, drivers and assignments in plain English. Answers are looked up live from the database by
      a locally hosted LLM ({{ status()?.model ?? '…' }} via Ollama).
      @if (status(); as s) {
        <span class="badge" [class.ACCEPTED]="s.reachable" [class.REJECTED]="!s.reachable">
          {{ s.reachable ? 'model online' : 'model offline – using built-in rules' }}
        </span>
      }
    </p>

    <section class="card chat">
      <div class="messages" #scroller>
        @if (messages().length === 0) {
          <div class="suggestions">
            @for (s of suggestions; track s) {
              <button class="chip" (click)="ask(s)">{{ s }}</button>
            }
          </div>
        }
        @for (m of messages(); track $index) {
          <div class="msg" [class.user]="m.role === 'user'" [class.error]="m.error">
            <div class="bubble">{{ m.text }}</div>
            @if (m.meta; as meta) {
              <div class="meta">
                {{ meta.mode === 'LLM' ? 'Answered by ' + meta.model : 'Built-in rules' }}
                @for (t of meta.toolCalls; track $index) { · looked up <code>{{ t.tool }}</code> }
                @if (meta.notice) { <div class="notice">{{ meta.notice }}</div> }
              </div>
            }
          </div>
        }
        @if (busy()) { <div class="msg"><div class="bubble typing">Thinking…</div></div> }
      </div>
      <form (ngSubmit)="ask(question)" class="composer">
        <input name="question" [(ngModel)]="question" placeholder='e.g. "Who is driving the Alto TS 09 AB 1234?"'
               autocomplete="off" [disabled]="busy()" />
        <button type="submit" [disabled]="busy() || !question.trim()">Ask</button>
      </form>
    </section>
  `,
})
export class AssistantPage {
  private readonly api = inject(ApiService);
  private readonly scroller = viewChild<ElementRef<HTMLElement>>('scroller');
  protected readonly messages = signal<Message[]>([]);
  protected readonly busy = signal(false);
  protected readonly status = signal<AiStatus | null>(null);
  protected question = '';

  protected readonly suggestions = [
    'Who is driving the Alto TS 09 AB 1234?',
    'What is the time assigned to Priya Sharma?',
    'Which vehicles are available right now?',
    'Show me pending assignment requests',
    'Give me a fleet summary',
  ];

  constructor() {
    this.api.aiStatus().subscribe({ next: (s) => this.status.set(s), error: () => this.status.set(null) });
  }

  ask(text: string): void {
    const q = text.trim();
    if (!q || this.busy()) return;
    const history: ChatTurn[] = this.messages()
      .filter((m) => !m.error)
      .map((m) => ({ role: m.role, content: m.text }));
    this.push({ role: 'user', text: q });
    this.question = '';
    this.busy.set(true);
    this.api.ask(q, history).subscribe({
      next: (r) => {
        this.push({ role: 'assistant', text: r.answer, meta: r });
        this.busy.set(false);
      },
      error: (e) => {
        this.push({ role: 'assistant', text: errorMessage(e), error: true });
        this.busy.set(false);
      },
    });
  }

  private push(m: Message): void {
    this.messages.update((list) => [...list, m]);
    setTimeout(() => {
      const el = this.scroller()?.nativeElement;
      if (el) el.scrollTop = el.scrollHeight;
    });
  }
}
