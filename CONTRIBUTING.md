# Contributing to MindFlow

Thank you for your interest in contributing to MindFlow!

## Development Setup

1. **Fork the repository** on GitHub
2. **Clone your fork**:
   ```bash
   git clone https://github.com/YOUR_USERNAME/mindflow-app.git
   cd mindflow-app
   ```
3. **Add upstream remote**:
   ```bash
   git remote add upstream https://github.com/nice的心情/mindflow-app.git
   ```
4. **Create a feature branch**:
   ```bash
   git checkout -b feature/your-feature-name
   ```

## Development Workflow

### 1. Code Style

We follow Kotlin coding conventions:
- 4 spaces for indentation (no tabs)
- Max line length: 120 characters
- Use meaningful variable names
- Add KDoc comments for public APIs

### 2. Building

```bash
# Build debug APK
./gradlew assembleDebug

# Build with tests
./gradlew build

# Run specific test
./gradlew test --tests "com.mindflow.*"
```

### 3. Testing

- Write unit tests for new features
- Ensure all tests pass before submitting PR
- Aim for >70% code coverage

### 4. Commit Messages

Follow [Conventional Commits](https://www.conventionalcommits.org/):

```
feat: add new web search tool
fix: handle null provider in chat viewmodel
docs: update README with new features
refactor: simplify memory consolidation logic
test: add unit tests for agent engine
```

## Pull Request Process

1. **Update documentation** if needed
2. **Add tests** for new functionality
3. **Ensure CI passes** (all tests, linting, build)
4. **Update SPEC.md** if adding new features
5. **Request review** from maintainers

## Areas for Contribution

### High Priority
- [ ] Vision/image support for chat
- [ ] Voice input/output
- [ ] Plugin system for custom tools
- [ ] Vector embedding for knowledge base

### Medium Priority
- [ ] i18n support (Chinese, English, etc.)
- [ ] Cloud sync
- [ ] Advanced RAG pipeline
- [ ] Multi-agent collaboration

### Low Priority
- [ ] Widget support
- [ ] Shortcuts integration
- [ ] Wear OS companion app

## Code of Conduct

Please be respectful and constructive in all interactions. We follow the [Contributor Covenant](https://www.contributor-covenant.org/).

## Questions?

Feel free to:
- Open an issue for bugs/feature requests
- Join discussions in GitHub Discussions
- Check the [Wiki](https://github.com/nice的心情/mindflow-app/wiki) for documentation

---

Thank you for making MindFlow better! 🙏
