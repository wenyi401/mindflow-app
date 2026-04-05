# MindFlow - AI Chat & Agent Platform

<p align="center">
  <img src="app/src/main/res/drawable/ic_launcher_foreground.xml" width="120" alt="MindFlow Logo"/>
</p>

<p align="center">
  <strong>MindFlow</strong> - 一个强大的AI聊天和Agent平台，支持多种AI服务商、本地知识库和智能记忆系统。
</p>

<p align="center">
  <a href="https://github.com/yourusername/mindflow-app/actions">
    <img src="https://github.com/yourusername/mindflow-app/actions/workflows/android.yml/badge.svg" alt="CI Status"/>
  </a>
  <a href="https://github.com/yourusername/mindflow-app/releases">
    <img src="https://img.shields.io/github/v/release/yourusername/mindflow-app?color=blue" alt="Release"/>
  </a>
  <a href="https://kotlinlang.org/">
    <img src="https://img.shields.io/badge/Kotlin-1.9.22-blue.svg" alt="Kotlin"/>
  </a>
  <a href="https://developer.android.com/studio/releases/gradle-plugin">
    <img src="https://img.shields.io/badge/AGP-8.2.2-blue.svg" alt="AGP"/>
  </a>
</p>

---

## ✨ Features

### 🤖 AI Chat
- **多AI服务商支持**: OpenAI、Anthropic、Google AI、Azure OpenAI、自定义端点
- **流式响应**: 实时流式输出，流畅的用户体验
- **对话管理**: 多会话支持，搜索历史，自动摘要

### 🧠 智能记忆系统
- **工作记忆**: 当前对话上下文窗口
- **短期记忆**: 最近7天的对话记忆
- **长期记忆**: 向量存储，语义搜索，RAG准备
- **自动重要性评分**: 智能区分重要信息

### 🔧 AI Agent能力
- **内置工具**: 网络搜索、计算器、知识查询、文本摘要
- **可扩展工具系统**: 轻松添加自定义工具
- **多种记忆策略**: 无记忆/短期/完整记忆
- **ReAct执行模式**: 推理+行动的问题解决模式

### 📚 知识库
- **本地文档**: 支持PDF、TXT、Markdown、DOC
- **网页内容**: URL抓取和索引
- **RAG管道**: 检索增强生成

### 🎨 MiKux风格UI
- Material Design 3设计语言
- 暗色/亮色主题
- 流畅的动画和过渡
- 深蓝(#1E88E5)主题色

---

## 🏗️ Architecture

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
│  │  Room    │  │Preferences│  │ Vector   │  │  API    │     │
│  │  DB      │  │  Store   │  │  Store   │  │  Client │     │
│  └──────────┘  └──────────┘  └──────────┘  └──────────┘     │
└─────────────────────────────────────────────────────────────┘
```

---

## 🛠️ Tech Stack

| Category | Technology |
|----------|------------|
| Language | Kotlin 1.9.22 |
| UI | Jetpack Compose + MiKux |
| DI | Koin 3.5.x |
| Database | Room 2.6.x |
| Networking | Retrofit + OkHttp |
| Async | Coroutines + Flow |
| Serialization | Kotlinx Serialization |

---

## 🚀 Getting Started

### Prerequisites
- Android Studio Hedgehog or later
- JDK 17
- Android SDK 34

### Build

```bash
# Clone the repository
git clone https://github.com/yourusername/mindflow-app.git
cd mindflow-app

# Build debug APK
./gradlew assembleDebug

# Run tests
./gradlew test

# Build release APK
./gradlew assembleRelease
```

### CI/CD

GitHub Actions workflow handles:
- **Push to main**: 自动构建debug APK
- **Pull Request**: 运行测试
- **版本Tag**: 构建release APK

---

## 📖 Documentation

- [SPEC.md](./SPEC.md) - 详细的功能规格说明
- [Wiki](https://github.com/yourusername/mindflow-app/wiki) - 项目文档

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
- [Anthropic](https://anthropic.com/) - Claude模型
- [Google AI](https://ai.google.dev/) - Gemini模型
- [MiKux](https://github.com/nice的心情/MiKux) - UI设计灵感

---

<p align="center">
  Made with ❤️ by <a href="https://github.com/yourusername">Your Name</a>
</p>
