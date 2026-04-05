package com.mindflow.data.remote.api


import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.*

/**
 * OpenAI Compatible API interface
 */
interface AIProviderApi {
    
    @POST("chat/completions")
    suspend fun createChatCompletion(
        @Header("Authorization") authorization: String,
        @Header("Content-Type") contentType: String = "application/json",
        @Body request: ChatCompletionRequest
    ): Response<ChatCompletionResponse>
    
    @POST("chat/completions")
    suspend fun createStreamingChatCompletion(
        @Header("Authorization") authorization: String,
        @Header("Content-Type") contentType: String = "application/json",
        @Body request: ChatCompletionRequest
    ): Response<ResponseBody>
    
    @GET("models")
    suspend fun getModels(
        @Header("Authorization") authorization: String
    ): Response<ModelsResponse>
    
    @GET("models/{model}")
    suspend fun getModel(
        @Header("Authorization") authorization: String,
        @Path("model") model: String
    ): Response<ModelInfo>
}

/**
 * Anthropic API interface
 */
interface AnthropicApi {
    
    @POST("v1/messages")
    suspend fun createMessage(
        @Header("x-api-key") apiKey: String,
        @Header("anthropic-version") version: String = "2023-06-01",
        @Header("Content-Type") contentType: String = "application/json",
        @Body request: AnthropicMessageRequest
    ): Response<AnthropicMessageResponse>
    
    @POST("v1/messages-stream")
    suspend fun createStreamingMessage(
        @Header("x-api-key") apiKey: String,
        @Header("anthropic-version") version: String = "2023-06-01",
        @Header("Content-Type") contentType: String = "application/json",
        @Body request: AnthropicMessageRequest
    ): Response<ResponseBody>
}

/**
 * Google AI (Gemini) API interface
 */
interface GoogleAIApi {
    
    @POST("v1beta/models/{model}:generateContent?key={apiKey}")
    suspend fun generateContent(
        @Path("model") model: String,
        @Query("apiKey") apiKey: String,
        @Body request: GoogleGenerateRequest
    ): Response<GoogleGenerateResponse>
    
    @POST("v1beta/models/{model}:streamGenerateContent?key={apiKey}&alt=sse")
    suspend fun streamGenerateContent(
        @Path("model") model: String,
        @Query("apiKey") apiKey: String,
        @Body request: GoogleGenerateRequest
    ): Response<ResponseBody>
}

/**
 * DTO classes for API requests/responses
 */

// OpenAI Compatible
@kotlinx.serialization.Serializable
data class ChatCompletionRequest(
    val model: String,
    val messages: List<ChatMessageDto>,
    val temperature: Float? = null,
    val max_tokens: Int? = null,
    val top_p: Float? = null,
    val stream: Boolean = false,
    val stop: List<String>? = null,
    val tools: List<ToolDefinitionDto>? = null,
    val tool_choice: String? = null
)

@kotlinx.serialization.Serializable
data class ChatMessageDto(
    val role: String,
    val content: String,
    val name: String? = null,
    val tool_calls: List<ToolCallDto>? = null,
    val tool_call_id: String? = null
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

@kotlinx.serialization.Serializable
data class ToolDefinitionDto(
    val type: String = "function",
    val function: ToolFunctionDto
)

@kotlinx.serialization.Serializable
data class ToolFunctionDto(
    val name: String,
    val description: String,
    val parameters: String
)

@kotlinx.serialization.Serializable
data class ChatCompletionResponse(
    val id: String,
    val model: String,
    val choices: List<ChatChoiceDto>,
    val usage: UsageDto?,
    val created: Long
)

@kotlinx.serialization.Serializable
data class ChatChoiceDto(
    val index: Int,
    val message: ResponseMessageDto,
    val finish_reason: String?
)

@kotlinx.serialization.Serializable
data class ResponseMessageDto(
    val role: String? = null,
    val content: String? = null,
    val tool_calls: List<ToolCallDto>?
)

@kotlinx.serialization.Serializable
data class UsageDto(
    val prompt_tokens: Int,
    val completion_tokens: Int,
    val total_tokens: Int
)

@kotlinx.serialization.Serializable
data class ModelsResponse(
    val `object`: String = "list",
    val data: List<ModelInfoDto>
)

@kotlinx.serialization.Serializable
data class ModelInfoDto(
    val id: String,
    val `object`: String = "model",
    val created: Long? = null,
    val owned_by: String? = null
)

@kotlinx.serialization.Serializable
data class ModelInfo(
    val id: String
)

// Anthropic
@kotlinx.serialization.Serializable
data class AnthropicMessageRequest(
    val model: String,
    val messages: List<AnthropicMessage>,
    val max_tokens: Int,
    val temperature: Float? = null,
    val system: String? = null,
    val stream: Boolean = false,
    val tools: List<AnthropicTool>? = null
)

@kotlinx.serialization.Serializable
data class AnthropicMessage(
    val role: String,
    val content: String
)

@kotlinx.serialization.Serializable
data class AnthropicTool(
    val name: String,
    val description: String,
    val input_schema: String
)

@kotlinx.serialization.Serializable
data class AnthropicMessageResponse(
    val id: String,
    val type: String,
    val role: String,
    val content: List<AnthropicContent>,
    val model: String,
    val stop_reason: String?,
    val stop_sequence: String?,
    val usage: AnthropicUsage
)

@kotlinx.serialization.Serializable
data class AnthropicContent(
    val type: String,
    val text: String? = null
)

@kotlinx.serialization.Serializable
data class AnthropicUsage(
    val input_tokens: Int,
    val output_tokens: Int
)

// Google AI
@kotlinx.serialization.Serializable
data class GoogleGenerateRequest(
    val contents: List<GoogleContent>,
    val generationConfig: GoogleGenerationConfig? = null,
    val safetySettings: List<GoogleSafetySetting>? = null
)

@kotlinx.serialization.Serializable
data class GoogleContent(
    val role: String,
    val parts: List<GooglePart>
)

@kotlinx.serialization.Serializable
data class GooglePart(
    val text: String? = null,
    val inlineData: GoogleInlineData? = null
)

@kotlinx.serialization.Serializable
data class GoogleInlineData(
    val mimeType: String,
    val data: String
)

@kotlinx.serialization.Serializable
data class GoogleGenerationConfig(
    val temperature: Float? = null,
    val maxOutputTokens: Int? = null,
    val topP: Float? = null,
    val topK: Int? = null
)

@kotlinx.serialization.Serializable
data class GoogleSafetySetting(
    val category: String,
    val threshold: String
)

@kotlinx.serialization.Serializable
data class GoogleGenerateResponse(
    val candidates: List<GoogleCandidate>?,
    val promptFeedback: GooglePromptFeedback?
)

@kotlinx.serialization.Serializable
data class GoogleCandidate(
    val content: GoogleContent,
    val finishReason: String?,
    val safetyRatings: List<GoogleSafetyRating>?
)

@kotlinx.serialization.Serializable
data class GoogleSafetyRating(
    val category: String,
    val probability: String
)

@kotlinx.serialization.Serializable
data class GooglePromptFeedback(
    val safetyRatings: List<GoogleSafetyRating>?
)
