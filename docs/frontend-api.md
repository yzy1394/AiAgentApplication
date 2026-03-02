# Frontend API Reference

Base URL: `http://localhost:8123/api`

Request body for chat APIs:

```json
{
  "message": "your input",
  "chatId": "session-id"
}
```

## Endpoints

1. `POST /ai/love_app/chat/sync`
- Description: sync chat response
- Response: plain text

2. `POST /ai/love_app/chat/sse`
- Description: streaming chat response (SSE)
- Response: `text/event-stream`

3. `POST /ai/love_app/chat/rag`
- Description: chat with RAG enhancement
- Response: plain text

4. `POST /ai/love_app/chat/tool`
- Description: chat with local tool calling
- Response: plain text

5. `POST /ai/love_app/chat/mcp`
- Description: chat with MCP tool provider
- Response: plain text

6. `POST /ai/love_app/chat/report`
- Description: structured love report
- Response:

```json
{
  "title": "string",
  "suggestions": ["string"]
}
```

7. `GET /ai/love_app/chat/sync?message=...&chatId=...`
- Description: legacy GET sync endpoint
- Response: plain text

8. `GET /ai/love_app/chat/sse?message=...&chatId=...`
- Description: legacy GET SSE endpoint (for EventSource)
- Response: `text/event-stream`

9. `GET /ai/love_app/chat/sse_emitter?message=...&chatId=...`
- Description: legacy SSE emitter endpoint
- Response: `text/event-stream`

10. `GET /test/hello`
- Description: test endpoint
- Response: `Hello, Knife4j!`

Use `docs/frontend-api.ts` as a ready-to-use TypeScript client.
