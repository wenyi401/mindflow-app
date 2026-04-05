# MindFlow - AI Chat & Agent Platform

## Project Overview

**Project Name:** MindFlow (心灵流)
**Type:** Android Native Application
**Core Functionality:** An AI-powered chat application with customizable AI provider support, intelligent agent capabilities, and a hybrid memory system that combines local knowledge bases with real-time web search for context-aware conversations.

## Architecture Overview

```
┌─────────────────────────────────────────────────────────────┐
│                        UI Layer (MiKux)                      │
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
│  │  Room    │  │Preferences│  │ Vector   │  │ File    │     │
│  │  DB      │  │  Store   │  │  Store   │  │  System │     │
│  └──────────┘  └──────────┘  └──────────┘  └──────────┘     │
└─────────────────────────────────────────────────────────────┘
```

## Technology Stack

### Framework & Language
- **Language:** Kotlin 1.9.x
- **Min SDK:** 26 (Android 8.0)
- **Target SDK:** 34 (Android 14)
- **UI Framework:** MiKux (Compose-based)

### Key Libraries
| Category | Library | Version |
|----------|---------|---------|
| DI | Koin | 3.5.x |
| Networking | Retrofit + OkHttp | 2.9.x / 4.12.x |
| Database | Room | 2.6.x |
| Async | Kotlin Coroutines + Flow | 1.7.x |
| Serialization | Kotlinx Serialization | 1.6.x |
| Vector Store | ChromaDB / Local LMDB | - |
| Embeddings | TensorFlow Lite / ONNX Runtime | - |
| Navigation | Compose Navigation | 2.7.x |
| Image Loading | Coil | 2.5.x |
| Markdown | Markwon | 4.6.x |

## Feature Specification

### 1. AI Chat Module

#### 1.1 Chat Interface
- Real-time streaming responses
- Markdown code rendering with syntax highlighting
- Image attachment support
- Message copy/retry/edit/delete
- Conversation search and history
- Export conversations (JSON/Markdown)

#### 1.2 AI Provider Support
Implement unified provider interface supporting:

| Provider | API Format | Auth Method |
|----------|------------|-------------|
| OpenAI Compatible | OpenAI Chat Completions | API Key |
| Anthropic | Anthropic Messages | API Key |
| Google AI (Gemini) | Gemini REST | API Key |
| Azure OpenAI | Azure AD / Key | API Key + Endpoint |
| Custom Endpoint | OpenAI Compatible | API Key + Base URL |

#### 1.3 Provider Configuration
```kotlin
data class AIProviderConfig(
    val id: String,
    val name: String,
    val type: ProviderType,
    val baseUrl: String,
    val apiKey: String,
    val modelId: String,
    val maxTokens: Int = 4096,
    val temperature: Float = 0.7f,
    val supportsVision: Boolean = false,
    val supportsStreaming: Boolean = true
)
```

### 2. Memory System

#### 2.1 Memory Architecture
```
┌─────────────────────────────────────────┐
│           Working Memory                │
│  (Current conversation context window)  │
├─────────────────────────────────────────┤
│          Short-Term Memory              │
│  (Recent conversations, 7 days)          │
├─────────────────────────────────────────┤
│          Long-Term Memory               │
│  (Vector embeddings, semantic search)   │
└─────────────────────────────────────────┘
```

#### 2.2 Memory Types

**Conversation Memory:**
- Stores chat messages with metadata
- Automatic summarization for long threads
- Key information extraction

**Semantic Memory:**
- Vector embeddings of important facts
- Concept relationships graph
- RAG (Retrieval Augmented Generation) ready

**User Preferences Memory:**
- Cached user settings and preferences
- AI behavior customization
- Per-conversation settings

#### 2.3 Memory Management
```kotlin
interface MemoryManager {
    // Working memory operations
    suspend fun updateWorkingContext(conversationId: String, messages: List<Message>)
    suspend fun getWorkingContext(conversationId: String): Context
    
    // Short-term memory operations
    suspend fun storeShortTerm(conversationId: String, messages: List<Message>)
    suspend fun retrieveShortTerm(query: String, limit: Int): List<MemoryEntry>
    
    // Long-term memory operations  
    suspend fun storeLongTerm(embedding: Embedding, content: String, metadata: Map<String, Any>)
    suspend fun retrieveLongTerm(query: String, limit: Int, threshold: Float): List<MemoryEntry>
    
    // Memory consolidation
    suspend fun consolidateMemories()
    suspend fun pruneOldMemories()
}
```

### 3. Agent Engine

#### 3.1 Agent Framework
Implements a ReAct (Reasoning + Acting) pattern:

```kotlin
data class AgentSpec(
    val id: String,
    val name: String,
    val description: String,
    val systemPrompt: String,
    val tools: List<Tool>,
    val memoryStrategy: MemoryStrategy,
    val maxIterations: Int = 10
)

enum class MemoryStrategy {
    NO_MEMORY,        // Stateless
    SHORT_TERM,       // Current session only
    FULL              // Full memory integration
}
```

#### 3.2 Built-in Tools
| Tool | Capability |
|------|------------|
| WebSearch | Search the web for information |
| KnowledgeQuery | Query local knowledge base |
| Calculator | Mathematical computations |
| FileReader | Read local files |
| CommandExecutor | Execute shell commands |
| HTTPRequest | Make HTTP requests |
| ImageAnalyzer | Analyze images |
| TextSummarizer | Summarize long text |

#### 3.3 Tool Interface
```kotlin
interface Tool {
    val name: String
    val description: String
    val inputSchema: JsonObject
    
    suspend fun execute(input: JSONObject): ToolResult
    suspend fun validate(input: JSONObject): ValidationResult
}
```

### 4. Knowledge Base

#### 4.1 Knowledge Sources
- **Local Documents:** PDF, TXT, Markdown, DOC files
- **Web Content:** URL scraping and indexing
- **Notes:** User-created notes
- **Custom Data:** Structured data import

#### 4.2 Knowledge Pipeline
```
Document → Chunking → Embedding → Vector Store → Retrieval → Context
```

#### 4.3 Knowledge Configuration
```kotlin
data class KnowledgeConfig(
    val enabled: Boolean = true,
    val chunkSize: Int = 512,
    val chunkOverlap: Int = 64,
    val embeddingModel: String = "local-embeddings",
    val maxResults: Int = 5,
    val similarityThreshold: Float = 0.7f
)
```

### 5. Settings & Configuration

#### 5.1 AI Settings
- Provider selection and configuration
- Model parameters (temperature, max tokens, top-p)
- System prompt customization
- Response format preferences

#### 5.2 Memory Settings
- Memory retention period
- Auto-summarization threshold
- Memory consolidation schedule
- Privacy controls

#### 5.3 App Settings
- Theme (Light/Dark/System)
- Notification preferences
- Data export/import
- Cache management

## UI/UX Design

### Screen Structure
```
MainActivity
├── ChatScreen (default)
│   ├── ConversationList
│   └── ChatView
├── AgentScreen
│   ├── AgentList
│   └── AgentWorkspace
├── KnowledgeScreen
│   ├── DocumentList
│   ├── WebImport
│   └── Search
└── SettingsScreen
    ├── AIProviders
    ├── MemoryConfig
    └── AppSettings
```

### Navigation
- Bottom navigation bar with 4 tabs
- Each tab maintains its own navigation stack
- Deep linking support for conversations

### Theme: MiKux Inspired
- Clean, modern aesthetic
- Rounded corners (16dp default)
- Soft shadows and elevation
- Smooth transitions
- Dynamic color support (Material You)

### Color Scheme
```kotlin
// Primary: Deep Blue (#1E88E5)
// Secondary: Teal (#26A69A)
// Surface: White/Dark Grey adaptive
// Error: Red (#EF5350)
// On colors follow Material 3 guidelines
```

## Data Models

### Conversation
```kotlin
@Entity
data class Conversation(
    @PrimaryKey val id: String,
    val title: String?,
    val createdAt: Long,
    val updatedAt: Long,
    val providerId: String,
    val modelId: String,
    val systemPrompt: String?,
    val settings: ConversationSettings?
)

data class ConversationSettings(
    val temperature: Float = 0.7f,
    val maxTokens: Int = 4096,
    val topP: Float? = null,
    val stopSequences: List<String> = emptyList()
)
```

### Message
```kotlin
@Entity
data class Message(
    @PrimaryKey val id: String,
    @ForeignKey val conversationId: String,
    val role: Role, // USER, ASSISTANT, SYSTEM
    val content: String,
    val rawContent: String?, // Original for streaming
    val modelId: String?,
    val tokens: Int?,
    val latencyMs: Long?,
    val createdAt: Long,
    val metadata: MessageMetadata?
)

data class MessageMetadata(
    val attachments: List<Attachment> = emptyList(),
    val toolCalls: List<ToolCall> = emptyList(),
    val error: String? = null
)
```

### Memory Entry
```kotlin
@Entity
data class MemoryEntry(
    @PrimaryKey val id: String,
    val type: MemoryType, // CONVERSATION, FACT, PREFERENCE
    val content: String,
    val embedding: FloatArray?, // For vector search
    val conversationId: String?,
    val createdAt: Long,
    val accessedAt: Long,
    val importance: Float, // 0-1, auto-calculated
    val accessCount: Int = 0
)
```

## GitHub Actions CI/CD

### Workflow Triggers
- Push to `main` branch: Build debug APK
- PR: Build and run tests
- Release tag: Build release APK

### Build Configuration
- Gradle 8.x with Kotlin DSL
- Java 17
- Debug APK: Unaligned, no ProGuard
- Release APK: ProGuard minification, signing config

### Build Artifacts
- Debug APK: `app/build/outputs/apk/debug/`
- Release APK: `app/build/outputs/apk/release/`

## Security Considerations

1. **API Key Storage:** Encrypted SharedPreferences or KeyChain
2. **Data at Rest:** Room database encryption (SQLCipher)
3. **Network:** Certificate pinning for API endpoints
4. **User Data:** Local-only by default, explicit export
5. **Memory Privacy:** User control over what gets remembered

## Non-Goals (Explicit Scope Boundaries)

- Not a general-purpose Android automation tool
- No direct system-level integrations (contacts, SMS, etc.)
- No social features or sharing to external platforms
- Not a replacement for specialized research tools

## Implementation Phases

### Phase 1: Foundation (MVP)
- [ ] Project setup with MiKux
- [ ] Basic chat UI with streaming
- [ ] Single AI provider (OpenAI compatible)
- [ ] Local message storage
- [ ] GitHub workflow setup

### Phase 2: Multi-Provider
- [ ] Provider abstraction layer
- [ ] Anthropic, Google, Azure support
- [ ] Provider testing/validation

### Phase 3: Memory System
- [ ] Short-term memory implementation
- [ ] Vector embedding integration
- [ ] Memory retrieval in chat context

### Phase 4: Agent Capabilities
- [ ] Tool system framework
- [ ] Built-in tools (search, calculator, etc.)
- [ ] Agent workspace UI

### Phase 5: Knowledge Base
- [ ] Document indexing
- [ ] Web content import
- [ ] RAG pipeline

### Phase 6: Polish
- [ ] Settings and customization
- [ ] Performance optimization
- [ ] Testing and bug fixes
