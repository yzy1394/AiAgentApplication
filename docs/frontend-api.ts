export interface ChatRequest {
  message: string;
  chatId?: string;
}

export interface LoveReport {
  title: string;
  suggestions: string[];
}

export type StreamHandler = (chunk: string) => void;

export class AiAgentApi {
  constructor(private readonly baseUrl = "http://localhost:8123/api") {}

  async chatSync(body: ChatRequest): Promise<string> {
    return this.postText("/ai/love_app/chat/sync", body);
  }

  async chatRag(body: ChatRequest): Promise<string> {
    return this.postText("/ai/love_app/chat/rag", body);
  }

  async chatTool(body: ChatRequest): Promise<string> {
    return this.postText("/ai/love_app/chat/tool", body);
  }

  async chatMcp(body: ChatRequest): Promise<string> {
    return this.postText("/ai/love_app/chat/mcp", body);
  }

  async chatReport(body: ChatRequest): Promise<LoveReport> {
    const res = await fetch(`${this.baseUrl}/ai/love_app/chat/report`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(body),
    });
    if (!res.ok) {
      throw new Error(`chatReport failed: ${res.status} ${await res.text()}`);
    }
    return (await res.json()) as LoveReport;
  }

  async chatSse(body: ChatRequest, onChunk: StreamHandler): Promise<void> {
    const res = await fetch(`${this.baseUrl}/ai/love_app/chat/sse`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(body),
    });
    if (!res.ok || !res.body) {
      throw new Error(`chatSse failed: ${res.status} ${await res.text()}`);
    }

    const reader = res.body.getReader();
    const decoder = new TextDecoder();
    let buffer = "";

    while (true) {
      const { done, value } = await reader.read();
      if (done) break;

      buffer += decoder.decode(value, { stream: true });
      const lines = buffer.split("\n");
      buffer = lines.pop() ?? "";

      for (const line of lines) {
        const text = line.trim();
        if (!text || !text.startsWith("data:")) continue;
        onChunk(text.slice(5).trimStart());
      }
    }
  }

  connectSseByEventSource(body: ChatRequest, onChunk: StreamHandler): EventSource {
    const message = encodeURIComponent(body.message ?? "");
    const chatId = encodeURIComponent(body.chatId ?? "default");
    const url = `${this.baseUrl}/ai/love_app/chat/sse?message=${message}&chatId=${chatId}`;
    const source = new EventSource(url);

    source.onmessage = (event) => onChunk(event.data);
    source.onerror = () => source.close();
    return source;
  }

  async testHello(): Promise<string> {
    const res = await fetch(`${this.baseUrl}/test/hello`);
    if (!res.ok) {
      throw new Error(`testHello failed: ${res.status} ${await res.text()}`);
    }
    return res.text();
  }

  private async postText(path: string, body: ChatRequest): Promise<string> {
    const res = await fetch(`${this.baseUrl}${path}`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(body),
    });
    if (!res.ok) {
      throw new Error(`${path} failed: ${res.status} ${await res.text()}`);
    }
    return res.text();
  }
}

// Example:
// const api = new AiAgentApi();
// const answer = await api.chatSync({ message: "hello", chatId: "u-1" });
