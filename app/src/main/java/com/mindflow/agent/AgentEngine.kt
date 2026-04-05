package com.mindflow.agent

import android.content.Context
import com.mindflow.agent.tools.*
import com.mindflow.domain.model.*
import com.mindflow.domain.repository.*
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.*
import java.util.UUID

/**
 * Agent Engine - Handles AI agent execution with tools and memory
 */
class AgentEngine(
    private val agentRepository: AgentRepository,
    private val toolRepository: ToolRepository,
    private val memoryRepository: MemoryRepository,
    private val knowledgeRepository: KnowledgeRepository,
    private val aiService: AIService,
    private val context: Context? = null
) {
    
    /**
     * Execute an agent with user input
     */
    suspend fun executeAgent(
        agentId: String,
        userInput: String,
        conversationId: String? = null,
        onChunk: (String) -> Unit,
        onToolCall: (ToolCall) -> Unit,
        onError: (String) -> Unit
    ): Result<String> {
        val agent = agentRepository.getAgentById(agentId)
            ?: return Result.failure(Exception("Agent not found"))
        
        return executeWithAgent(
            agent = agent,
            userInput = userInput,
            conversationId = conversationId,
            onChunk = onChunk,
            onToolCall = onToolCall,
            onError = onError
        )
    }
    
    private suspend fun executeWithAgent(
        agent: Agent,
        userInput: String,
        conversationId: String?,
        onChunk: (String) -> Unit,
        onToolCall: (ToolCall) -> Unit,
        onError: (String) -> Unit
    ): Result<String> {
        val tools = agent.tools.mapNotNull { toolId ->
            toolRepository.getToolById(toolId)
        }
        
        val memoryContext = when (agent.memoryStrategy) {
            MemoryStrategy.NO_MEMORY -> ""
            MemoryStrategy.SHORT_TERM -> {
                conversationId?.let { cid ->
                    memoryRepository.getMemoriesForConversation(cid).first()
                        .joinToString("\n") { it.content }
                } ?: ""
            }
            MemoryStrategy.FULL -> {
                val recentMemories = memoryRepository.searchMemories(userInput, 5).getOrNull() ?: emptyList()
                val knowledgeContext = knowledgeRepository.queryKnowledge(userInput, 3).getOrNull() ?: emptyList()
                
                buildFullContext(recentMemories, knowledgeContext)
            }
        }
        
        val systemPrompt = buildSystemPrompt(agent, tools, memoryContext)
        
        var currentInput = userInput
        var iterations = 0
        val maxIterations = agent.maxIterations
        
        while (iterations < maxIterations) {
            val messages = listOf(
                Message(
                    id = UUID.randomUUID().toString(),
                    conversationId = conversationId ?: "",
                    role = MessageRole.SYSTEM,
                    content = systemPrompt,
                    createdAt = System.currentTimeMillis()
                ),
                Message(
                    id = UUID.randomUUID().toString(),
                    conversationId = conversationId ?: "",
                    role = MessageRole.USER,
                    content = currentInput,
                    createdAt = System.currentTimeMillis()
                )
            )
            
            val result = aiService.sendMessage(
                conversationId = conversationId ?: UUID.randomUUID().toString(),
                providerId = getDefaultProviderId(),
                messages = messages,
                systemPrompt = null,
                temperature = 0.7f,
                maxTokens = 4096
            )
            
            if (result.isFailure) {
                onError(result.exceptionOrNull()?.message ?: "Unknown error")
                return result.map { it.content }
            }
            
            val response = result.getOrNull()!!
            
            // Check for tool calls in response
            val toolCalls = response.toolCalls
            if (toolCalls.isEmpty()) {
                onChunk(response.content)
                return Result.success(response.content)
            }
            
            // Execute tools
            for (toolCall in toolCalls) {
                val tool = tools.find { it.name == toolCall.name }
                if (tool != null) {
                    onToolCall(toolCall)
                    val toolResult = executeTool(tool, toolCall.arguments)
                    
                    // Add tool result to context
                    currentInput += "\n\n[TOOL RESULT: ${toolResult.output}]"
                    
                    // Store tool execution in memory
                    storeToolExecution(toolCall, toolResult, conversationId)
                }
            }
            
            iterations++
        }
        
        return Result.failure(Exception("Max iterations exceeded"))
    }
    
    /**
     * Execute a specific tool
     */
    private suspend fun executeTool(tool: Tool, argumentsJson: String): ToolResult {
        val startTime = System.currentTimeMillis()
        
        return try {
            // Use the tool factory to get the actual tool implementation
            val toolImpl = ToolFactory.createTool(tool.name, context ?: android.content.ContextImpl.getApplicationContext(null), knowledgeRepository)
            
            if (toolImpl == null) {
                return ToolResult(
                    toolCallId = "",
                    success = false,
                    output = "",
                    error = "Tool implementation not found: ${tool.name}",
                    executionTimeMs = System.currentTimeMillis() - startTime
                )
            }
            
            val arguments = Json.parseToJsonElement(argumentsJson).jsonObject
            val output = when (toolImpl) {
                is WebSearchTool -> toolImpl.execute(arguments["query"]?.jsonPrimitive?.content ?: "")
                is CalculatorTool -> toolImpl.execute(arguments["expression"]?.jsonPrimitive?.content ?: "")
                is TextSummarizerTool -> toolImpl.execute(
                    arguments["text"]?.jsonPrimitive?.content ?: "",
                    arguments["maxLength"]?.jsonPrimitive?.intOrNull ?: 200
                )
                is KnowledgeQueryTool -> toolImpl.execute(
                    arguments["query"]?.jsonPrimitive?.content ?: "",
                    arguments["limit"]?.jsonPrimitive?.intOrNull ?: 5
                )
                is DateTimeTool -> toolImpl.execute(arguments["format"]?.jsonPrimitive?.content ?: "readable")
                is UrlFetchTool -> toolImpl.execute(
                    arguments["url"]?.jsonPrimitive?.content ?: "",
                    arguments["maxLength"]?.jsonPrimitive?.intOrNull ?: 2000
                )
                is ConverterTool -> toolImpl.execute(
                    arguments["type"]?.jsonPrimitive?.content ?: "",
                    arguments["value"]?.jsonPrimitive?.doubleOrNull ?: 0.0,
                    arguments["from"]?.jsonPrimitive?.content ?: "",
                    arguments["to"]?.jsonPrimitive?.content ?: ""
                )
                else -> """{"error": "Unknown tool type"}"""
            }
            
            ToolResult(
                toolCallId = "",
                success = true,
                output = output,
                executionTimeMs = System.currentTimeMillis() - startTime
            )
        } catch (e: Exception) {
            ToolResult(
                toolCallId = "",
                success = false,
                output = "",
                error = e.message,
                executionTimeMs = System.currentTimeMillis() - startTime
            )
        }
    }
    
    private suspend fun storeToolExecution(
        toolCall: ToolCall,
        result: ToolResult,
        conversationId: String?
    ) {
        val memoryEntry = MemoryEntry(
            id = UUID.randomUUID().toString(),
            type = MemoryType.FACT,
            content = "Tool ${toolCall.name} was executed with result: ${result.output}",
            conversationId = conversationId,
            importance = 0.5f
        )
        memoryRepository.storeMemory(memoryEntry)
    }
    
    private fun buildSystemPrompt(agent: Agent, tools: List<Tool>, memoryContext: String): String {
        val toolDescriptions = if (tools.isNotEmpty()) {
            "\n\nAvailable tools:\n" + tools.joinToString("\n") { tool ->
                "- ${tool.name}: ${tool.description}"
            }
        } else ""
        
        val memorySection = if (memoryContext.isNotEmpty()) {
            "\n\nRelevant context from memory:\n$memoryContext"
        } else ""
        
        return """
            |${agent.systemPrompt}
            |
            |You are an AI agent with the following capabilities:
            |${agent.description}$toolDescriptions$memorySection
            |
            |Always respond with tool calls when needed to complete complex tasks.
        """.trimMargin()
    }
    
    private fun buildFullContext(
        memories: List<MemoryEntry>,
        knowledge: List<KnowledgeDocument>
    ): String {
        val memorySection = if (memories.isNotEmpty()) {
            "Past relevant information:\n" + memories.joinToString("\n") { "• ${it.content}" }
        } else ""
        
        val knowledgeSection = if (knowledge.isNotEmpty()) {
            "Relevant knowledge:\n" + knowledge.joinToString("\n") { "• ${it.title}: ${it.content.take(200)}" }
        } else ""
        
        return listOf(memorySection, knowledgeSection)
            .filter { it.isNotEmpty() }
            .joinToString("\n\n")
    }
    
    private fun getDefaultProviderId(): String = "default"
    
    /**
     * Create a new agent
     */
    suspend fun createAgent(agent: Agent): Result<Unit> {
        return agentRepository.createAgent(agent)
    }
    
    /**
     * Update an existing agent
     */
    suspend fun updateAgent(agent: Agent): Result<Unit> {
        return agentRepository.updateAgent(agent)
    }
    
    /**
     * Delete an agent
     */
    suspend fun deleteAgent(agentId: String): Result<Unit> {
        return agentRepository.deleteAgent(agentId)
    }
    
    /**
     * Register a new tool
     */
    suspend fun registerTool(tool: Tool): Result<Unit> {
        return toolRepository.registerTool(tool)
    }
}
