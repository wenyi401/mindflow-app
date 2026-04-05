package com.mindflow.agent

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
    private val aiService: AIService
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
            val arguments = Json.parseToJsonElement(argumentsJson).jsonObject
            val output = when (tool.name) {
                "web_search" -> executeWebSearch(arguments)
                "calculator" -> executeCalculator(arguments)
                "knowledge_query" -> executeKnowledgeQuery(arguments)
                "text_summarizer" -> executeSummarizer(arguments)
                else -> """{"error": "Unknown tool: ${tool.name}"}"""
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
    
    private fun executeWebSearch(args: JsonObject): String {
        val query = args["query"]?.jsonPrimitive?.content ?: return """{"error": "No query provided"}"""
        // Web search implementation would go here
        return """{"results": [], "query": "$query"}"""
    }
    
    private fun executeCalculator(args: JsonObject): String {
        val expression = args["expression"]?.jsonPrimitive?.content 
            ?: return """{"error": "No expression provided"}"""
        
        return try {
            val result = evaluateExpression(expression)
            """{"result": $result, "expression": "$expression"}"""
        } catch (e: Exception) {
            """{"error": "${e.message}"}"""
        }
    }
    
    private fun executeKnowledgeQuery(args: JsonObject): String {
        // This is a placeholder - actual implementation would query the knowledge base
        val query = args["query"]?.jsonPrimitive?.content ?: return """{"error": "No query provided"}"""
        return """{"results": [], "query": "$query"}"""
    }
    
    private fun executeSummarizer(args: JsonObject): String {
        val text = args["text"]?.jsonPrimitive?.content ?: return """{"error": "No text provided"}"""
        // Simple summarization - in production would use AI
        val summary = text.take(100) + if (text.length > 100) "..." else ""
        return """{"summary": "$summary", "originalLength": ${text.length}}"""
    }
    
    private fun evaluateExpression(expression: String): Double {
        // Safe math evaluation
        val sanitized = expression.replace("[^0-9+\\-*/.() ]".toRegex(), "")
        return eval(sanitized)
    }
    
    private fun eval(expr: String): Double {
        return object {
            var pos = -1
            var ch = 0
            
            fun nextChar() {
                ch = if (++pos < expr.length) expr[pos].code else -1
            }
            
            fun eat(charToEat: Int): Boolean {
                while (ch == ' '.code) nextChar()
                if (ch == charToEat) {
                    nextChar()
                    return true
                }
                return false
            }
            
            fun parse(): Double {
                nextChar()
                val x = parseExpression()
                if (pos < expr.length) throw RuntimeException("Unexpected: ${ch.toChar()}")
                return x
            }
            
            fun parseExpression(): Double {
                var x = parseTerm()
                while (true) {
                    when {
                        eat('+'.code) -> x += parseTerm()
                        eat('-'.code) -> x -= parseTerm()
                        else -> return x
                    }
                }
            }
            
            fun parseTerm(): Double {
                var x = parseFactor()
                while (true) {
                    when {
                        eat('*'.code) -> x *= parseFactor()
                        eat('/'.code) -> x /= parseFactor()
                        else -> return x
                    }
                }
            }
            
            fun parseFactor(): Double {
                if (eat('+'.code)) return parseFactor()
                if (eat('-'.code)) return -parseFactor()
                
                var x = 0.0
                val startPos = pos
                if (eat('('.code)) {
                    x = parseExpression()
                    eat(')'.code)
                } else if (ch >= '0'.code && ch <= '9'.code || ch == '.'.code) {
                    while (ch >= '0'.code && ch <= '9'.code || ch == '.'.code) nextChar()
                    x = expr.substring(startPos, pos).toDouble()
                }
                return x
            }
        }.parse()
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
