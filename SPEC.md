# MindFlow - AI Chat & Agent Platform Specification

> Last Updated: 2026-04-05

## Project Overview

**MindFlow** is a comprehensive AI chat and agent platform for Android that supports multiple AI providers, local knowledge bases, and intelligent memory management.

## Features

### 1. AI Chat (✅ Implemented)
- [x] Multi-provider support (OpenAI Compatible, Anthropic, Google AI, Azure, Custom)
- [x] Streaming responses with real-time display
- [x] Conversation history with search
- [x] Markdown rendering for code blocks and formatted text
- [x] Message regeneration capability
- [x] Token usage tracking and latency display

### 2. AI Agent System (✅ Implemented)
- [x] Custom agent creation with system prompts
- [x] Built-in tools:
  - [x] Web Search (DuckDuckGo API)
  - [x] Calculator (Math expression evaluator)
  - [x] Text Summarizer
  - [x] Date/Time
  - [x] Unit Converter (length, weight, temperature)
  - [x] Knowledge Base Query
  - [x] URL Content Fetcher
- [x] Memory strategies: NO_MEMORY, SHORT_TERM, FULL
- [x] ReAct execution pattern (Reasoning + Action)
- [x] Max iteration control
- [x] Tool execution tracking

### 3. Memory System (✅ Implemented)
- [x] Working memory (current conversation context)
- [x] Short-term memory (conversation history)
- [x] Long-term memory (importance-based storage)
- [x] Automatic summarization of old messages
- [x] Memory consolidation
- [x] Memory pruning (old, low-importance entries)
- [x] Key fact extraction from conversations
- [x] Memory statistics

### 4. Knowledge Base (🔄 In Progress)
- [x] Document storage (basic)
- [x] Document search (text-based)
- [x] Source types: FILE, WEB_URL, NOTE, IMPORT
- [x] Web content fetching
- [ ] Document chunking for large files
- [ ] Vector embedding storage
- [ ] RAG (Retrieval Augmented Generation) pipeline

### 5. Provider Management (✅ Implemented)
- [x] Multiple provider configuration
- [x] Provider types:
  - [x] OpenAI Compatible (with custom endpoint)
  - [x] Anthropic (Claude)
  - [x] Google AI (Gemini)
  - [x] Azure OpenAI
  - [x] Custom API
- [x] Provider validation
- [x] Model selection per provider
- [x] Temperature and max tokens configuration

## Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                        UI Layer                              │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌──────────┐   │
│  │  Chat    │  │  Agent   │  │ Knowledge│  │ Settings │   │
│  │  Screen  │  │  Space   │  │  Base    │  │          │   │
│  └──────────┘  └──────────┘  └──────────┘  └──────────┘   │
├─────────────────────────────────────────────────────────────┤
│                      Domain Layer                            │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐       │
│  │ AI Provider  │  │   Memory    │  │    Agent    │       │
│  │   Framework  │  │   System    │  │    Engine   │       │
│  └──────────────┘  └──────────────┘  └──────────────┘       │
│  ┌──────────────┐  ┌──────────────┐                         │
│  │  Knowledge   │  │   Tool      │                         │
│  │   Retrieval  │  │   System    │                         │
│  └──────────────┘  └──────────────┘                         │
├─────────────────────────────────────────────────────────────┤
│                       Data Layer                             │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌──────────┐     │
│  │  Room    │  │Preferences│  │ Vector   │  │  API    │     │
│  │  DB      │  │  Store   │  │  Store   │  │  Client │     │
│  └──────────┘  └──────────┘  └──────────┘  └──────────┘     │
└─────────────────────────────────────────────────────────────┘
```

## Tech Stack

| Category | Technology | Version |
|----------|------------|---------|
| Language | Kotlin | 1.9.22 |
| UI | Jetpack Compose + Material3 | BOM 2024.01.00 |
| Theme | MiKux-inspired (custom) | - |
| DI | Koin | 3.5.3 |
| Database | Room | 2.6.1 |
| Networking | Retrofit + OkHttp | 2.9.0 / 4.12.0 |
| Serialization | Kotlinx Serialization | 1.6.2 |
| Async | Coroutines + Flow | 1.7.3 |
| Markdown | Markwon | 4.6.2 |
| Image Loading | Coil | 2.5.0 |

## Data Models

### AIProvider
```kotlin
- id: String
- name: String
- type: ProviderType (OPENAI_COMPATIBLE, ANTHROPIC, GOOGLE_AI, AZURE_OPENAI, CUSTOM)
- baseUrl: String
- apiKey: String
- modelId: String
- maxTokens: Int (default: 4096)
- temperature: Float (default: 0.7)
- supportsVision: Boolean
- supportsStreaming: Boolean
- isEnabled: Boolean
```

### Message
```kotlin
- id: String
- conversationId: String
- role: MessageRole (SYSTEM, USER, ASSISTANT, TOOL)
- content: String
- rawContent: String?
- modelId: String?
- tokens: Int?
- latencyMs: Long?
- createdAt: Long
- attachments: List<Attachment>
- toolCalls: List<ToolCall>
- error: String?
```

### Agent
```kotlin
- id: String
- name: String
- description: String
- systemPrompt: String
- tools: List<String> (tool IDs)
- memoryStrategy: MemoryStrategy (NO_MEMORY, SHORT_TERM, FULL)
- maxIterations: Int (default: 10)
- createdAt: Long
```

### MemoryEntry
```kotlin
- id: String
- type: MemoryType (CONVERSATION, FACT, PREFERENCE, KNOWLEDGE)
- content: String
- conversationId: String?
- createdAt: Long
- accessedAt: Long
- importance: Float (0.0 - 1.0)
- accessCount: Int
- metadata: Map<String, String>
```

## API Integration

### OpenAI Compatible API
- Endpoint: `POST /chat/completions`
- Supports streaming via SSE
- Tool calling via `tools` and `tool_choice` parameters

### Anthropic API
- Endpoint: `POST /v1/messages`
- Streaming via `stream: true`
- Uses Claude-specific message format

### Google AI (Gemini)
- Endpoint: `POST /v1beta/models/{model}:generateContent`
- Streaming via `alt=sse` query parameter

## Memory Strategy Details

### NO_MEMORY
- No context carried between turns
- Each conversation is independent

### SHORT_TERM
- Keeps conversation history in working memory
- Messages from the last 20 turns included
- Older messages summarized and stored

### FULL
- Full RAG pipeline with memory + knowledge base
- Vector similarity search for relevant context
- Combines conversation history with external knowledge

## Tool System

### Built-in Tools

| Tool | Description | Parameters |
|------|-------------|------------|
| web_search | Search the web via DuckDuckGo | query: string |
| calculator | Evaluate math expressions | expression: string |
| text_summarizer | Summarize long text | text: string, maxLength?: int |
| knowledge_query | Query local knowledge base | query: string, limit?: int |
| datetime | Get current date/time | format?: "iso" \| "readable" \| "unix" |
| url_fetch | Fetch content from URL | url: string, maxLength?: int |
| converter | Unit conversion | type: string, value: number, from: string, to: string |

### Tool Execution Flow
1. Agent receives user input
2. AI decides to call a tool
3. Tool name and arguments extracted from response
4. Tool executed with arguments
5. Tool result added to context
6. Agent continues reasoning with tool result

## UI Screens

### Chat Screen
- Message bubbles (user right-aligned, assistant left-aligned)
- Streaming response indicator
- Error banner for failures
- Input bar with send button
- Regenerate response button

### Agent Workspace Screen
- Agent selection/creation
- Tool configuration
- Memory strategy selection
- Execution with step-by-step display
- Tool call visualization

### Knowledge Base Screen
- Document list with source badges
- Add document FAB
- Search functionality
- Document detail view

### Settings Screen
- Provider management
- Theme toggle (light/dark)
- Memory statistics
- About section

## GitHub Actions CI/CD

### Workflows
1. **build-debug**: Builds debug APK on every push to main
2. **test**: Runs unit tests on pull requests
3. **build-release**: Builds release APK on version tags

### Secrets Required for Release
- `KEYSTORE`: Release keystore file
- `KEY_ALIAS`: Keystore alias
- `KEY_PASSWORD`: Key password
- `STORE_PASSWORD`: Store password

## Future Enhancements

- [ ] Vision support for image attachments
- [ ] Voice input/output
- [ ] Plugin system for custom tools
- [ ] Multi-agent collaboration
- [ ] Cloud sync for memory
- [ ] Advanced RAG with embeddings
- [ ] i18n support

## Development Guidelines

### Adding a New AI Provider
1. Add provider type to `ProviderType` enum in `Models.kt`
2. Implement API interface in `AIProviderApi.kt` (e.g., `NewProviderApi`)
3. Add provider-specific request/response DTOs
4. Update `AIService.sendMessage()` to handle the new provider type
5. Add UI in provider settings screen

### Adding a New Tool
1. Create tool class implementing `Tool` interface in `ToolImplementations.kt`
2. Add tool to `ToolFactory.createTool()` method
3. Register tool in `AgentViewModel` or database
4. Update agent creation UI to show the new tool

### Memory System Configuration
| Strategy | Context Window | Storage | Use Case |
|----------|---------------|---------|----------|
| NO_MEMORY | None | None | Stateless tasks |
| SHORT_TERM | Last 20 messages | Room DB | Simple conversations |
| FULL | All relevant + RAG | Room DB + Vector | Complex agent tasks |

### Database Schema
- **conversations**: Chat sessions
- **messages**: Individual messages
- **providers**: AI provider configurations
- **memory_entries**: Long-term memory storage
- **knowledge_documents**: Knowledge base documents
- **agents**: Custom agent definitions

### API Rate Limits
- Default timeout: 60s connect, 120s read
- Streaming responses supported for all providers
- Retry logic should be implemented at the network layer

## Development Notes

### Building Locally
```bash
./gradlew assembleDebug  # Debug build
./gradlew assembleRelease # Release build (requires signing config)
```

### Running Tests
```bash
./gradlew test           # Unit tests
./gradlew connectedCheck # Instrumented tests
```

### Adding a New Provider
1. Implement `AIProviderApi` interface in `data/remote/api/`
2. Add provider type to `ProviderType` enum
3. Add handling in `AIService.sendMessage()`
4. Add UI in provider settings screen

## License

MIT License - See LICENSE file

---

_MindFlow - Think Deeper, Remember Longer_
