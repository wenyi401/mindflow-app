package com.mindflow.ui.screens.agent

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mindflow.agent.AgentEngine
import com.mindflow.domain.model.*
import com.mindflow.domain.repository.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * Agent List ViewModel
 */
class AgentListViewModel(
    private val agentRepository: AgentRepository
) : ViewModel() {
    
    val agents: StateFlow<List<Agent>> = agentRepository
        .getAllAgents()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
    
    fun createAgent(
        name: String,
        description: String,
        systemPrompt: String,
        tools: List<String>,
        memoryStrategy: MemoryStrategy
    ): Result<String> {
        val id = UUID.randomUUID().toString()
        val agent = Agent(
            id = id,
            name = name,
            description = description,
            systemPrompt = systemPrompt,
            tools = tools,
            memoryStrategy = memoryStrategy,
            createdAt = System.currentTimeMillis()
        )
        
        return runCatching {
            // Default tools
            val defaultTools = listOf(
                Tool(
                    id = "web_search",
                    name = "web_search",
                    description = "Search the web for information",
                    inputSchema = """{"type": "object", "properties": {"query": {"type": "string"}}, "required": ["query"]}"""
                ),
                Tool(
                    id = "calculator",
                    name = "calculator",
                    description = "Perform mathematical calculations",
                    inputSchema = """{"type": "object", "properties": {"expression": {"type": "string"}}, "required": ["expression"]}"""
                ),
                Tool(
                    id = "knowledge_query",
                    name = "knowledge_query",
                    description = "Query the local knowledge base",
                    inputSchema = """{"type": "object", "properties": {"query": {"type": "string"}}, "required": ["query"]}"""
                )
            )
            
            defaultTools.forEach { tool ->
                // Register default tools would go here
            }
            
            agentRepository.createAgent(agent)
            id
        }
    }
    
    fun deleteAgent(id: String) {
        viewModelScope.launch {
            agentRepository.deleteAgent(id)
        }
    }
}

/**
 * Agent Workspace ViewModel
 */
class AgentWorkspaceViewModel(
    private val agentRepository: AgentRepository,
    private val agentEngine: AgentEngine
) : ViewModel() {
    
    private val _agent = MutableStateFlow<Agent?>(null)
    val agent: StateFlow<Agent?> = _agent.asStateFlow()
    
    private val _messages = MutableStateFlow<List<Message>>(emptyList())
    val messages: StateFlow<List<Message>> = _messages.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _streamingContent = MutableStateFlow("")
    val streamingContent: StateFlow<String> = _streamingContent.asStateFlow()
    
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()
    
    private val _toolCalls = MutableStateFlow<List<ToolCall>>(emptyList())
    val toolCalls: StateFlow<List<ToolCall>> = _toolCalls.asStateFlow()
    
    fun loadAgent(agentId: String) {
        viewModelScope.launch {
            _agent.value = agentRepository.getAgentById(agentId)
        }
    }
    
    fun executeTask(task: String) {
        val currentAgent = _agent.value ?: return
        
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            _streamingContent.value = ""
            _toolCalls.value = emptyList()
            
            val conversationId = UUID.randomUUID().toString()
            
            agentEngine.executeAgent(
                agentId = currentAgent.id,
                userInput = task,
                conversationId = conversationId,
                onChunk = { chunk ->
                    _streamingContent.value += chunk
                },
                onToolCall = { toolCall ->
                    _toolCalls.value = _toolCalls.value + toolCall
                },
                onError = { error ->
                    _error.value = error
                }
            ).onSuccess { result ->
                // Add to messages
                _messages.value = _messages.value + Message(
                    id = UUID.randomUUID().toString(),
                    conversationId = conversationId,
                    role = MessageRole.ASSISTANT,
                    content = result,
                    createdAt = System.currentTimeMillis()
                )
            }
            
            // Add user message
            _messages.value = _messages.value + Message(
                id = UUID.randomUUID().toString(),
                conversationId = conversationId,
                role = MessageRole.USER,
                content = task,
                createdAt = System.currentTimeMillis()
            )
            
            _streamingContent.value = ""
            _isLoading.value = false
        }
    }
    
    fun clearError() {
        _error.value = null
    }
}
