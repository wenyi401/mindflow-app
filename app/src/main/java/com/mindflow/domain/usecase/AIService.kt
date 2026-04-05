package com.mindflow.domain.usecase

import com.mindflow.data.remote.api.*
import com.mindflow.domain.model.*
import com.mindflow.domain.repository.MessageRepository
import com.mindflow.domain.repository.ProviderRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.ResponseBody
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.TimeUnit

/**
 * AI Service for handling chat completions with multiple provider support
 */
class AIService(
    private val providerRepository: ProviderRepository,
    private val messageRepository: MessageRepository,
    private val json: Json
) {
    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        })
        .build()
    
    private val apiCache = mutableMapOf<String, AIProviderApi>()
    
    private fun getApiForProvider(provider: AIProvider): AIProviderApi {
        return apiCache.getOrPut(provider.id) {
            val retrofit = Retrofit.Builder()
                .baseUrl(normalizeBaseUrl(provider.baseUrl))
                .client(httpClient)
                .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
                .build()
            retrofit.create(AIProviderApi::class.java)
        }
    }
    
    private fun normalizeBaseUrl(url: String): String {
        return if (url.endsWith("/")) url else "$url/"
    }
    
    /**
     * Send a message and get a streaming response
     */
    suspend fun sendMessageStream(
        conversationId: String,
        providerId: String,
        messages: List<Message>,
        systemPrompt: String?,
        temperature: Float,
        maxTokens: Int,
        onChunk: (String, Boolean) -> Unit, // content, isFinal
        onError: (String) -> Unit
    ): Result<Message> {
        val provider = providerRepository.getProviderById(providerId)
            ?: return Result.failure(Exception("Provider not found"))
        
        return when (provider.type) {
            ProviderType.OPENAI_COMPATIBLE, ProviderType.AZURE_OPENAI, ProviderType.CUSTOM -> 
                sendOpenAICompatibleStream(provider, conversationId, messages, systemPrompt, temperature, maxTokens, onChunk, onError)
            ProviderType.ANTHROPIC -> 
                sendAnthropicStream(provider, conversationId, messages, systemPrompt, temperature, maxTokens, onChunk, onError)
            ProviderType.GOOGLE_AI -> 
                sendGoogleAIStream(provider, conversationId, messages, systemPrompt, temperature, maxTokens, onChunk, onError)
        }
    }
    
    /**
     * Send a message and get a complete response
     */
    suspend fun sendMessage(
        conversationId: String,
        providerId: String,
        messages: List<Message>,
        systemPrompt: String?,
        temperature: Float,
        maxTokens: Int
    ): Result<Message> {
        val provider = providerRepository.getProviderById(providerId)
            ?: return Result.failure(Exception("Provider not found"))
        
        return when (provider.type) {
            ProviderType.OPENAI_COMPATIBLE, ProviderType.AZURE_OPENAI, ProviderType.CUSTOM -> 
                sendOpenAICompatible(provider, conversationId, messages, systemPrompt, temperature, maxTokens)
            ProviderType.ANTHROPIC -> 
                sendAnthropic(provider, conversationId, messages, systemPrompt, temperature, maxTokens)
            ProviderType.GOOGLE_AI -> 
                sendGoogleAI(provider, conversationId, messages, systemPrompt, temperature, maxTokens)
        }
    }
    
    // OpenAI Compatible Implementation
    private suspend fun sendOpenAICompatibleStream(
        provider: AIProvider,
        conversationId: String,
        messages: List<Message>,
        systemPrompt: String?,
        temperature: Float,
        maxTokens: Int,
        onChunk: (String, Boolean) -> Unit,
        onError: (String) -> Unit
    ): Result<Message> {
        val api = getApiForProvider(provider)
        val startTime = System.currentTimeMillis()
        
        val chatMessages = buildChatMessages(messages, systemPrompt)
        val request = ChatCompletionRequest(
            model = provider.modelId,
            messages = chatMessages,
            temperature = temperature,
            max_tokens = maxTokens,
            stream = true
        )
        
        return try {
            val response = api.createStreamingChatCompletion(
                authorization = "Bearer ${provider.apiKey}",
                request = request
            )
            
            if (!response.isSuccessful) {
                val error = "API Error: ${response.code()} - ${response.message()}"
                onError(error)
                return Result.failure(Exception(error))
            }
            
            val body = response.body() ?: return Result.failure(Exception("Empty response"))
            val buffer = StringBuilder()
            
            body.byteStream().bufferedReader().use { reader ->
                var line = reader.readLine()
                while (line != null) {
                    if (line.startsWith("data: ")) {
                        val data = line.removePrefix("data: ").trim()
                        if (data == "[DONE]") {
                            break
                        }
                        parseStreamChunk(data, buffer, onChunk)
                    }
                    line = reader.readLine()
                }
            }
            
            onChunk("", true) // Signal completion
            
            val content = buffer.toString()
            val latency = System.currentTimeMillis() - startTime
            
            Result.success(
                Message(
                    id = generateId(),
                    conversationId = conversationId,
                    role = MessageRole.ASSISTANT,
                    content = content,
                    modelId = provider.modelId,
                    latencyMs = latency,
                    createdAt = System.currentTimeMillis()
                )
            )
        } catch (e: Exception) {
            onError(e.message ?: "Unknown error")
            Result.failure(e)
        }
    }
    
    private fun parseStreamChunk(data: String, buffer: StringBuilder, onChunk: (String, Boolean) -> Unit) {
        runCatching {
            val chunk = json.decodeFromString<StreamChunk>(data)
            chunk.choices.firstOrNull()?.delta?.content?.let { content ->
                if (content.isNotEmpty()) {
                    buffer.append(content)
                    onChunk(content, false)
                }
            }
        }
    }
    
    private suspend fun sendOpenAICompatible(
        provider: AIProvider,
        conversationId: String,
        messages: List<Message>,
        systemPrompt: String?,
        temperature: Float,
        maxTokens: Int
    ): Result<Message> {
        val api = getApiForProvider(provider)
        val startTime = System.currentTimeMillis()
        
        val chatMessages = buildChatMessages(messages, systemPrompt)
        val request = ChatCompletionRequest(
            model = provider.modelId,
            messages = chatMessages,
            temperature = temperature,
            max_tokens = maxTokens,
            stream = false
        )
        
        return try {
            val response = api.createChatCompletion(
                authorization = "Bearer ${provider.apiKey}",
                request = request
            )
            
            if (!response.isSuccessful) {
                return Result.failure(Exception("API Error: ${response.code()} - ${response.message()}"))
            }
            
            val body = response.body() ?: return Result.failure(Exception("Empty response"))
            val content = body.choices.firstOrNull()?.message?.content ?: ""
            val latency = System.currentTimeMillis() - startTime
            
            Result.success(
                Message(
                    id = generateId(),
                    conversationId = conversationId,
                    role = MessageRole.ASSISTANT,
                    content = content,
                    modelId = provider.modelId,
                    tokens = body.usage?.totalTokens,
                    latencyMs = latency,
                    createdAt = System.currentTimeMillis()
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // Anthropic Implementation
    private suspend fun sendAnthropicStream(
        provider: AIProvider,
        conversationId: String,
        messages: List<Message>,
        systemPrompt: String?,
        temperature: Float,
        maxTokens: Int,
        onChunk: (String, Boolean) -> Unit,
        onError: (String) -> Unit
    ): Result<Message> {
        val baseUrl = if (provider.baseUrl.endsWith("/")) provider.baseUrl else "${provider.baseUrl}/"
        val retrofit = Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(httpClient)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
        val api = retrofit.create(AnthropicApi::class.java)
        
        val startTime = System.currentTimeMillis()
        val chatMessages = messages.filter { it.role == MessageRole.USER || it.role == MessageRole.ASSISTANT }
        
        val request = AnthropicMessageRequest(
            model = provider.modelId,
            messages = chatMessages.map { AnthropicMessage(it.role.name.lowercase(), it.content) },
            max_tokens = maxTokens,
            temperature = temperature,
            system = systemPrompt,
            stream = true
        )
        
        return try {
            val response = api.createStreamingMessage(
                apiKey = provider.apiKey,
                request = request
            )
            
            if (!response.isSuccessful) {
                val error = "API Error: ${response.code()} - ${response.message()}"
                onError(error)
                return Result.failure(Exception(error))
            }
            
            val body = response.body() ?: return Result.failure(Exception("Empty response"))
            val buffer = StringBuilder()
            
            body.byteStream().bufferedReader().use { reader ->
                var line = reader.readLine()
                while (line != null) {
                    if (line.startsWith("data: ")) {
                        val data = line.removePrefix("data: ").trim()
                        parseAnthropicStreamChunk(data, buffer, onChunk)
                    }
                    line = reader.readLine()
                }
            }
            
            onChunk("", true)
            
            val content = buffer.toString()
            val latency = System.currentTimeMillis() - startTime
            
            Result.success(
                Message(
                    id = generateId(),
                    conversationId = conversationId,
                    role = MessageRole.ASSISTANT,
                    content = content,
                    modelId = provider.modelId,
                    latencyMs = latency,
                    createdAt = System.currentTimeMillis()
                )
            )
        } catch (e: Exception) {
            onError(e.message ?: "Unknown error")
            Result.failure(e)
        }
    }
    
    private fun parseAnthropicStreamChunk(data: String, buffer: StringBuilder, onChunk: (String, Boolean) -> Unit) {
        if (data == "event: message_stop") return
        
        runCatching {
            val json = Json.parseToJsonElement(data)
            val content = json.jsonObject
                .get("content")?.jsonArray
                ?.firstOrNull()
                ?.jsonObject
                ?.get("text")
                ?.jsonPrimitive
                ?.content
            
            if (!content.isNullOrEmpty()) {
                buffer.append(content)
                onChunk(content, false)
            }
        }
    }
    
    private suspend fun sendAnthropic(
        provider: AIProvider,
        conversationId: String,
        messages: List<Message>,
        systemPrompt: String?,
        temperature: Float,
        maxTokens: Int
    ): Result<Message> {
        val baseUrl = if (provider.baseUrl.endsWith("/")) provider.baseUrl else "${provider.baseUrl}/"
        val retrofit = Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(httpClient)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
        val api = retrofit.create(AnthropicApi::class.java)
        
        val startTime = System.currentTimeMillis()
        val chatMessages = messages.filter { it.role == MessageRole.USER || it.role == MessageRole.ASSISTANT }
        
        val request = AnthropicMessageRequest(
            model = provider.modelId,
            messages = chatMessages.map { AnthropicMessage(it.role.name.lowercase(), it.content) },
            max_tokens = maxTokens,
            temperature = temperature,
            system = systemPrompt,
            stream = false
        )
        
        return try {
            val response = api.createMessage(apiKey = provider.apiKey, request = request)
            
            if (!response.isSuccessful) {
                return Result.failure(Exception("API Error: ${response.code()} - ${response.message()}"))
            }
            
            val body = response.body() ?: return Result.failure(Exception("Empty response"))
            val content = body.content.firstOrNull()?.text ?: ""
            val latency = System.currentTimeMillis() - startTime
            
            Result.success(
                Message(
                    id = generateId(),
                    conversationId = conversationId,
                    role = MessageRole.ASSISTANT,
                    content = content,
                    modelId = provider.modelId,
                    latencyMs = latency,
                    createdAt = System.currentTimeMillis()
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // Google AI Implementation
    private suspend fun sendGoogleAIStream(
        provider: AIProvider,
        conversationId: String,
        messages: List<Message>,
        systemPrompt: String?,
        temperature: Float,
        maxTokens: Int,
        onChunk: (String, Boolean) -> Unit,
        onError: (String) -> Unit
    ): Result<Message> {
        val startTime = System.currentTimeMillis()
        val chatMessages = messages.filter { it.role == MessageRole.USER || it.role == MessageRole.ASSISTANT }
        
        val contents = chatMessages.map { msg ->
            GoogleContent(
                role = if (msg.role == MessageRole.USER) "user" else "model",
                parts = listOf(GooglePart(text = msg.content))
            )
        }
        
        val request = GoogleGenerateRequest(
            contents = contents,
            generationConfig = GoogleGenerationConfig(
                temperature = temperature,
                maxOutputTokens = maxTokens
            )
        )
        
        return try {
            val response = httpClient.newCall(
                okhttp3.Request.Builder()
                    .url("https://generativelanguage.googleapis.com/v1beta/models/${provider.modelId}:streamGenerateContent?key=${provider.apiKey}&alt=sse")
                    .post(okhttp3.RequestBody.create(
                        "application/json".toMediaType(),
                        json.encodeToString(request)
                    ))
                    .build()
            ).execute()
            
            if (!response.isSuccessful) {
                val error = "API Error: ${response.code()} - ${response.message}"
                onError(error)
                return Result.failure(Exception(error))
            }
            
            val buffer = StringBuilder()
            response.body?.byteStream()?.bufferedReader()?.use { reader ->
                var line = reader.readLine()
                while (line != null) {
                    if (line.startsWith("data: ")) {
                        val data = line.removePrefix("data: ").trim()
                        parseGoogleStreamChunk(data, buffer, onChunk)
                    }
                    line = reader.readLine()
                }
            }
            
            onChunk("", true)
            
            val content = buffer.toString()
            val latency = System.currentTimeMillis() - startTime
            
            Result.success(
                Message(
                    id = generateId(),
                    conversationId = conversationId,
                    role = MessageRole.ASSISTANT,
                    content = content,
                    modelId = provider.modelId,
                    latencyMs = latency,
                    createdAt = System.currentTimeMillis()
                )
            )
        } catch (e: Exception) {
            onError(e.message ?: "Unknown error")
            Result.failure(e)
        }
    }
    
    private fun parseGoogleStreamChunk(data: String, buffer: StringBuilder, onChunk: (String, Boolean) -> Unit) {
        runCatching {
            val json = Json.parseToJsonElement(data)
            val content = json.jsonObject
                .get("candidates")?.jsonArray
                ?.firstOrNull()
                ?.jsonObject
                ?.get("content")?.jsonObject
                ?.get("parts")?.jsonArray
                ?.firstOrNull()
                ?.jsonObject
                ?.get("text")
                ?.jsonPrimitive
                ?.content
            
            if (!content.isNullOrEmpty()) {
                buffer.append(content)
                onChunk(content, false)
            }
        }
    }
    
    private suspend fun sendGoogleAI(
        provider: AIProvider,
        conversationId: String,
        messages: List<Message>,
        systemPrompt: String?,
        temperature: Float,
        maxTokens: Int
    ): Result<Message> {
        val startTime = System.currentTimeMillis()
        val chatMessages = messages.filter { it.role == MessageRole.USER || it.role == MessageRole.ASSISTANT }
        
        val contents = chatMessages.map { msg ->
            GoogleContent(
                role = if (msg.role == MessageRole.USER) "user" else "model",
                parts = listOf(GooglePart(text = msg.content))
            )
        }
        
        val request = GoogleGenerateRequest(
            contents = contents,
            generationConfig = GoogleGenerationConfig(
                temperature = temperature,
                maxOutputTokens = maxTokens
            )
        )
        
        val baseUrl = if (provider.baseUrl.endsWith("/")) provider.baseUrl else "${provider.baseUrl}/"
        val retrofit = Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(httpClient)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
        val api = retrofit.create(GoogleAIApi::class.java)
        
        return try {
            val response = api.generateContent(
                model = provider.modelId,
                apiKey = provider.apiKey,
                request = request
            )
            
            if (!response.isSuccessful) {
                return Result.failure(Exception("API Error: ${response.code()} - ${response.message()}"))
            }
            
            val body = response.body() ?: return Result.failure(Exception("Empty response"))
            val content = body.candidates?.firstOrNull()
                ?.content?.parts?.firstOrNull()
                ?.text ?: ""
            val latency = System.currentTimeMillis() - startTime
            
            Result.success(
                Message(
                    id = generateId(),
                    conversationId = conversationId,
                    role = MessageRole.ASSISTANT,
                    content = content,
                    modelId = provider.modelId,
                    latencyMs = latency,
                    createdAt = System.currentTimeMillis()
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    private fun buildChatMessages(messages: List<Message>, systemPrompt: String?): List<ChatMessageDto> {
        val result = mutableListOf<ChatMessageDto>()
        
        systemPrompt?.let {
            result.add(ChatMessageDto("system", it))
        }
        
        messages.forEach { msg ->
            result.add(
                ChatMessageDto(
                    role = when (msg.role) {
                        MessageRole.SYSTEM -> "system"
                        MessageRole.USER -> "user"
                        MessageRole.ASSISTANT -> "assistant"
                        MessageRole.TOOL -> "tool"
                    },
                    content = msg.content
                )
            )
        }
        
        return result
    }
    
    private fun generateId(): String = java.util.UUID.randomUUID().toString()
}

/**
 * Stream chunk model for parsing SSE responses
 */
@kotlinx.serialization.Serializable
data class StreamChunk(
    val id: String,
    val model: String,
    val choices: List<StreamChoice>
)

@kotlinx.serialization.Serializable
data class StreamChoice(
    val index: Int,
    val delta: DeltaMessage,
    val finish_reason: String?
)

@kotlinx.serialization.Serializable
data class DeltaMessage(
    val role: String? = null,
    val content: String? = null,
    val tool_calls: List<ToolCallDto>? = null
)

@kotlinx.serialization.Serializable
data class ToolCallDto(
    val id: String,
    val type: String = "function",
    val function: ToolCallFunctionDto
)

@kotlinx.serialization.Serializable
data class ToolCallFunctionDto(
    val name: String,
    val arguments: String
)
