package com.mindflow.domain.model

import kotlinx.serialization.Serializable

/**
 * AI Provider types supported by the app
 */
@Serializable
enum class ProviderType {
    OPENAI_COMPATIBLE,
    ANTHROPIC,
    GOOGLE_AI,
    AZURE_OPENAI,
    CUSTOM
}

/**
 * AI Provider configuration
 */
@Serializable
data class AIProvider(
    val id: String,
    val name: String,
    val type: ProviderType,
    val baseUrl: String,
    val apiKey: String,
    val modelId: String,
    val maxTokens: Int = 4096,
    val temperature: Float = 0.7f,
    val supportsVision: Boolean = false,
    val supportsStreaming: Boolean = true,
    val isEnabled: Boolean = true
)

/**
 * Message role in a conversation
 */
@Serializable
enum class MessageRole {
    SYSTEM,
    USER,
    ASSISTANT,
    TOOL
}

/**
 * Message sent/received in a chat
 */
@Serializable
data class Message(
    val id: String,
    val conversationId: String,
    val role: MessageRole,
    val content: String,
    val rawContent: String? = null,
    val modelId: String? = null,
    val tokens: Int? = null,
    val latencyMs: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val attachments: List<Attachment> = emptyList(),
    val toolCalls: List<ToolCall> = emptyList(),
    val error: String? = null
)

/**
 * File attachment in a message
 */
@Serializable
data class Attachment(
    val id: String,
    val type: AttachmentType,
    val name: String,
    val url: String,
    val size: Long? = null,
    val mimeType: String? = null
)

@Serializable
enum class AttachmentType {
    IMAGE,
    FILE,
    AUDIO
}

/**
 * Tool call made by AI
 */
@Serializable
data class ToolCall(
    val id: String,
    val name: String,
    val arguments: String,
    val result: String? = null,
    val success: Boolean = true
)

/**
 * Conversation entity
 */
@Serializable
data class Conversation(
    val id: String,
    val title: String? = null,
    val providerId: String,
    val modelId: String,
    val systemPrompt: String? = null,
    val temperature: Float = 0.7f,
    val maxTokens: Int = 4096,
    val topP: Float? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

/**
 * Memory types for the memory system
 */
@Serializable
enum class MemoryType {
    CONVERSATION,
    FACT,
    PREFERENCE,
    KNOWLEDGE
}

/**
 * Memory entry stored long-term
 */
@Serializable
data class MemoryEntry(
    val id: String,
    val type: MemoryType,
    val content: String,
    val conversationId: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val accessedAt: Long = System.currentTimeMillis(),
    val importance: Float = 0.5f,
    val accessCount: Int = 0,
    val metadata: Map<String, String> = emptyMap()
)

/**
 * Tool definition for agents
 */
@Serializable
data class Tool(
    val id: String,
    val name: String,
    val description: String,
    val inputSchema: String, // JSON schema as string
    val enabled: Boolean = true
)

/**
 * Tool execution result
 */
@Serializable
data class ToolResult(
    val toolCallId: String,
    val success: Boolean,
    val output: String,
    val error: String? = null,
    val executionTimeMs: Long = 0
)

/**
 * Agent specification
 */
@Serializable
data class Agent(
    val id: String,
    val name: String,
    val description: String,
    val systemPrompt: String,
    val tools: List<String> = emptyList(), // Tool IDs
    val memoryStrategy: MemoryStrategy = MemoryStrategy.SHORT_TERM,
    val maxIterations: Int = 10,
    val createdAt: Long = System.currentTimeMillis()
)

@Serializable
enum class MemoryStrategy {
    NO_MEMORY,
    SHORT_TERM,
    FULL
}

/**
 * Knowledge document
 */
@Serializable
data class KnowledgeDocument(
    val id: String,
    val title: String,
    val source: String,
    val sourceType: SourceType,
    val content: String,
    val chunkCount: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val accessedAt: Long = System.currentTimeMillis(),
    val tags: List<String> = emptyList()
)

@Serializable
enum class SourceType {
    FILE,
    WEB_URL,
    NOTE,
    IMPORT
}

/**
 * API request/response models
 */
@Serializable
data class ChatCompletionRequest(
    val model: String,
    val messages: List<ChatMessage>,
    val temperature: Float? = null,
    val maxTokens: Int? = null,
    val topP: Float? = null,
    val stream: Boolean = false,
    val stop: List<String>? = null,
    val tools: List<ToolDefinition>? = null,
    val toolChoice: String? = null
)

@Serializable
data class ChatMessage(
    val role: String,
    val content: String,
    val name: String? = null,
    val toolCalls: List<ToolCallRequest>? = null,
    val toolCallId: String? = null
)

@Serializable
data class ToolCallRequest(
    val id: String,
    val type: String = "function",
    val function: ToolCallFunction
)

@Serializable
data class ToolCallFunction(
    val name: String,
    val arguments: String
)

@Serializable
data class ToolDefinition(
    val type: String = "function",
    val function: ToolFunction
)

@Serializable
data class ToolFunction(
    val name: String,
    val description: String,
    val parameters: String // JSON schema
)

@Serializable
data class ChatCompletionResponse(
    val id: String,
    val model: String,
    val choices: List<ChatChoice>,
    val usage: Usage?,
    val created: Long
)

@Serializable
data class ChatChoice(
    val index: Int,
    val message: ResponseMessage,
    val finishReason: String?
)

@Serializable
data class ResponseMessage(
    val role: String?,
    val content: String?,
    val toolCalls: List<ToolCallRequest>?
)

@Serializable
data class Usage(
    val promptTokens: Int,
    val completionTokens: Int,
    val totalTokens: Int
)

/**
 * Stream response chunk
 */
@Serializable
data class StreamChunk(
    val id: String,
    val model: String,
    val choices: List<StreamChoice>
)

@Serializable
data class StreamChoice(
    val index: Int,
    val delta: DeltaMessage,
    val finishReason: String?
)

@Serializable
data class DeltaMessage(
    val role: String? = null,
    val content: String? = null,
    val toolCalls: List<ToolCallRequest>?
)
