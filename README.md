# MindFlow - AI Chat & Agent Platform

<p align="center">
  <img src="app/src/main/res/drawable/ic_launcher_foreground.xml" width="120" alt="MindFlow Logo"/>
</p>

<p align="center">
  <strong>MindFlow</strong> - 一个强大的AI聊天和Agent平台，支持多种AI服务商、本地知识库和智能记忆系统。
</p>

<p align="center">
  <a href="https://github.com/nice的心情/mindflow-app/actions">
    <img src="https://github.com/nice的心情/mindflow-app/workflows/Android%20CI/CD/badge.svg" alt="CI Status"/>
  </a>
  <a href="https://github.com/nice的心情/mindflow-app/releases">
    <img src="https://img.shields.io/github/v/release/nice的心情/mindflow-app?color=blue" alt="Release"/>
  </a>
  <a href="https://kotlinlang.org/">
    <img src="https://img.shields.io/badge/Kotlin-1.9.22-blue.svg" alt="Kotlin"/>
  </a>
  <a href="https://developer.android.com/studio/releases/gradle-plugin">
    <img src="https://img.shields.io/badge/AGP-8.2.2-blue.svg" alt="AGP"/>
  </a>
  <img src="https://img.shields.io/badge/Android-Min%20SDK%2026-yellow.svg" alt="Min SDK"/>
  <img src="https://img.shields.io/badge/Android-Target%20SDK%2034-blue.svg" alt="Target SDK"/>
</p>

---

## ✨ Features

### 🤖 AI Chat
- **多AI服务商支持**: OpenAI、Anthropic、Google AI、Azure OpenAI、自定义端点
- **流式响应**: 实时流式输出，流畅的用户体验
- **对话管理**: 多会话支持，搜索历史，自动摘要
- **Markdown渲染**: 代码高亮、表格、格式化文本

### 🧠 智能记忆系统
- **工作记忆**: 当前对话上下文窗口
- **短期记忆**: 最近7天的对话记忆
- **长期记忆**: 基于重要性评分，语义搜索准备
- **自动重要性评分**: 智能区分重要信息
- **记忆整合**: 自动合并相似记忆，清理低价值记忆

### 🔧 AI Agent能力
- **内置工具**: 网络搜索、计算器、知识查询、文本摘要、单位转换
- **可扩展工具系统**: 轻松添加自定义工具
- **多种记忆策略**: 无记忆/短期/完整记忆
- **ReAct执行模式**: 推理+行动的问题解决模式

### 📚 知识库
- **本地文档**: 支持PDF、TXT、Markdown、DOC
- **网页内容**: URL抓取和索引
- **RAG管道**: 检索增强生成（开发中）

### 🎨 MiKux风格UI
- Material Design 3设计语言
- 暗色/亮色主题
- 流畅的动画和过渡
- 深蓝(#1E88E5)主题色

---

## 🏗️ Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                        UI Layer (Compose + MiKux)            │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌──────────┐   │
│  │  Chat    │  │  Agent   │  │ Knowledge│  │ Settings │   │
│  │  Screen  │  │  Space   │  │  Base    │  │          │   │
│  └──────────┘  └──────────┘  └──────────┘  └──────────┘   │
├─────────────────────────────────────────────────────────────┤
│                      Domain Layer                            │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐       │
│  │ AI Provider  │  │   Memory    │  │    Agent    │       │
│  │   Framework  │  │   Manager   │  │    Engine   │       │
│  └──────────────┘  └──────────────┘  └──────────────┘       │
│  ┌──────────────┐  ┌──────────────┐                         │
│  │  Knowledge   │  │   Tool     │                         │
│  │   Retrieval  │  │ Implementations│                      │
│  └──────────────┘  └──────────────┘                         │
├─────────────────────────────────────────────────────────────┤
│                       Data Layer                             │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌──────────┐     │
│  │  Room    │  │Preferences│  │  Cache   │  │  API    │     │
│  │  DB      │  │  Store   │  │          │  │  Client │     │
│  └──────────┘  └──────────┘  └──────────┘  └──────────┘     │
└─────────────────────────────────────────────────────────────┘
```

---

## 🛠️ Tech Stack

| Category | Technology |
|----------|------------|
| Language | Kotlin 1.9.22 |
| UI | Jetpack Compose + Material3 |
| DI | Koin 3.5.x |
| Database | Room 2.6.x |
| Networking | Retrofit + OkHttp |
| Async | Coroutines + Flow |
| Serialization | Kotlinx Serialization |
| Markdown | Markwon |

---

## 🚀 Getting Started

### Prerequisites
- Android Studio Hedgehog or later
- JDK 17
- Android SDK 34

### Build

```bash
# Clone the repository
git clone https://github.com/nice的心情/mindflow-app.git
cd mindflow-app

# Build debug APK
./gradlew assembleDebug

# Run tests
./gradlew test

# Build release APK (requires signing config)
./gradlew assembleRelease
```

### CI/CD

GitHub Actions workflow handles:
- **Push to main**: 自动构建debug APK
- **Pull Request**: 运行测试
- **版本Tag**: 构建release APK

---

## 🔧 Configuration

### Adding an AI Provider

1. Open the app and go to **Settings**
2. Tap **Provider Management**
3. Tap **Add Provider**
4. Select provider type:
   - **OpenAI Compatible**: For OpenAI, local models, etc.
   - **Anthropic**: For Claude models
   - **Google AI**: For Gemini models
   - **Azure OpenAI**: For Microsoft Azure
   - **Custom**: For any OpenAI-compatible API
5. Enter the required information:
   - Name: Display name
   - Base URL: API endpoint
   - API Key: Your API key
   - Model ID: Model to use (e.g., gpt-4, claude-3)
6. Tap **Save** and **Validate**

### Creating an Agent

1. Go to the **Agent** tab
2. Tap **+** to create a new agent
3. Configure:
   - **Name**: Agent name
   - **Description**: What the agent does
   - **System Prompt**: Agent's personality and instructions
   - **Tools**: Select tools to enable
   - **Memory Strategy**: Choose how much context to maintain
   - **Max Iterations**: Limit tool call loops
4. Tap **Save**

---

## 📖 Documentation

- [SPEC.md](./SPEC.md) - 详细的功能规格说明
- [Wiki](https://github.com/nice的心情/mindflow-app/wiki) - 项目文档

---

## 🤝 Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

1. Fork the repository
2. Create your feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add some amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

---

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

---

## 🙏 Acknowledgments

- [OpenAI](https://openai.com/) - GPT models
- [Anthropic](https://anthropic.com/) - Claude models
- [Google AI](https://ai.google.dev/) - Gemini models
- [MiKux](https://github.com/nice的心情/MiKux) - UI设计灵感
- [DuckDuckGo](https://duckduckgo.com/) - 免费搜索API

---

## 🗂️ Project Structure

```
mindflow-app/
├── app/
│   └── src/main/
│       ├── java/com/mindflow/
│       │   ├── agent/           # AI Agent engine and tools
│       │   │   ├── tools/      # Built-in tool implementations
│       │   │   └── memory/      # Memory management
│       │   ├── data/            # Data layer
│       │   │   ├── local/       # Room database
│       │   │   └── remote/      # API clients
│       │   ├── domain/          # Domain layer
│       │   │   ├── model/      # Data models
│       │   │   ├── repository/ # Repository interfaces
│       │   │   └── usecase/     # Use cases
│       │   ├── ui/             # UI layer
│       │   │   ├── screens/    # Screen composables
│       │   │   ├── navigation/ # Navigation
│       │   │   └── theme/      # Theme configuration
│       │   └── di/              # Dependency injection
│       └── res/                 # Resources
├── .github/workflows/           # GitHub Actions
├── build.gradle.kts            # Root build config
└── settings.gradle.kts         # Project settings
```

## 🔌 Supported AI Providers

| Provider | Models | Streaming | Vision | Tools |
|----------|--------|-----------|--------|-------|
| OpenAI Compatible | GPT-4, GPT-3.5, etc. | ✅ | ✅ | ✅ |
| Anthropic | Claude 3, Claude 2 | ✅ | ✅ | ✅ |
| Google AI | Gemini Pro, Gemini Ultra | ✅ | ✅ | ❌ |
| Azure OpenAI | GPT-4, GPT-3.5 | ✅ | ✅ | ✅ |
| Custom (Ollama, LM Studio, etc.) | Any OpenAI-compatible | ✅ | ✅ | ✅ |

## 📝 Memory Architecture

```
┌─────────────────────────────────────────────────────┐
│                  Memory Hierarchy                    │
├─────────────────────────────────────────────────────┤
│  Working Memory (In-Context)                        │
│  └── Last 20 messages in current conversation       │
├─────────────────────────────────────────────────────┤
│  Short-Term Memory (Session)                        │
│  └── Conversation history with auto-summarization  │
├─────────────────────────────────────────────────────┤
│  Long-Term Memory (Persistent)                      │
│  └── Importance-based storage, semantic search      │
└─────────────────────────────────────────────────────┘
```

## 🤖 Agent Tool System

The agent can use these built-in tools:
- 🌐 **Web Search** - DuckDuckGo instant answers
- 🧮 **Calculator** - Math expression evaluation
- 📝 **Text Summarizer** - Summarize long text
- 📚 **Knowledge Query** - Query local knowledge base
- 📅 **DateTime** - Get current date/time
- 🔗 **URL Fetcher** - Fetch web page content
- 🔄 **Unit Converter** - Length, weight, temperature

## ⚙️ Configuration Examples

### OpenAI Compatible (e.g., OpenAI, local models)
```
Base URL: https://api.openai.com/v1
Model ID: gpt-4
API Key: sk-...
```

### Anthropic
```
Base URL: https://api.anthropic.com
Model ID: claude-3-opus-20240229
API Key: sk-ant-...
```

### Google AI (Gemini)
```
Base URL: https://generativelanguage.googleapis.com
Model ID: gemini-pro
API Key: AIza...
```

### Custom (Ollama, LM Studio, etc.)
```
Base URL: http://localhost:11434/v1 (Ollama)
Model ID: llama2, mistral, etc.
API Key: ollama (or empty for local)
```

## 📦 Building

### Debug Build
```bash
./gradlew assembleDebug
```

### Release Build
```bash
./gradlew assembleRelease
```

### CI/CD
All builds are handled by GitHub Actions:
- **Push to main**: Automatic debug APK build
- **Pull Request**: Unit tests run
- **Version tag (v*)**: Release APK build

## 📄 License

MIT License - See [LICENSE](LICENSE) file

---

<p align="center">
  Made with ❤️ by <a href="https://github.com/nice的心情">nice的心情</a>
</p>
