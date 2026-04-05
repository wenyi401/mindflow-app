package com.mindflow.ui.screens.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mindflow.domain.model.*
import com.mindflow.domain.repository.*
import com.mindflow.domain.usecase.AIService
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * Chat List ViewModel
 */
class ChatListViewModel(
    private val conversationRepository: ConversationRepository
) : ViewModel() {
    
    val conversations: StateFlow<List<Conversation>> = conversationRepository
        .getAllConversations()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
    
    fun createConversation(providerId: String, modelId: String): String {
        val id = UUID.randomUUID().toString()
        val conversation = Conversation(
            id = id,
            title = null,
            providerId = providerId,
            modelId = modelId,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        
        viewModelScope.launch {
            conversationRepository.createConversation(conversation)
        }
        
        return id
    }
    
    fun deleteConversation(id: String) {
        viewModelScope.launch {
            conversationRepository.deleteConversation(id)
        }
    }
    
    fun updateTitle(id: String, title: String) {
        viewModelScope.launch {
            conversationRepository.updateConversationTitle(id, title)
        }
    }
}

/**
 * Chat ViewModel for a single conversation
 */
class ChatViewModel(
    private val conversationRepository: ConversationRepository,
    private val messageRepository: MessageRepository,
    private val providerRepository: ProviderRepository,
    private val aiService: AIService
) : ViewModel() {
    
    private val _messages = MutableStateFlow<List<Message>>(emptyList())
    val messages: StateFlow<List<Message>> = _messages.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _streamingContent = MutableStateFlow("")
    val streamingContent: StateFlow<String> = _streamingContent.asStateFlow()
    
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()
    
    private var conversationId: String = ""
    private var provider: AIProvider? = null
    
    fun loadConversation(id: String) {
        conversationId = id
        viewModelScope.launch {
            val conversation = conversationRepository.getConversationById(id)
            provider = conversation?.let { providerRepository.getProviderById(it.providerId) }
            
            messageRepository.getMessagesForConversation(id).collect { messages ->
                _messages.value = messages
            }
        }
    }
    
    fun sendMessage(content: String) {
        if (content.isBlank() || provider == null) return
        
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            _streamingContent.value = ""
            
            val userMessage = Message(
                id = UUID.randomUUID().toString(),
                conversationId = conversationId,
                role = MessageRole.USER,
                content = content,
                createdAt = System.currentTimeMillis()
            )
            
            messageRepository.sendMessage(userMessage)
            
            val conversation = conversationRepository.getConversationById(conversationId)
            val messages = messageRepository.getRecentMessages(conversationId, 50)
            
            aiService.sendMessageStream(
                conversationId = conversationId,
                providerId = provider!!.id,
                messages = messages,
                systemPrompt = conversation?.systemPrompt,
                temperature = conversation?.temperature ?: 0.7f,
                maxTokens = conversation?.maxTokens ?: 4096,
                onChunk = { chunk, isFinal ->
                    if (isFinal) {
                        _streamingContent.value = ""
                    } else {
                        _streamingContent.value += chunk
                    }
                },
                onError = { error ->
                    _error.value = error
                }
            ).onSuccess { assistantMessage ->
                messageRepository.sendMessage(assistantMessage)
                conversationRepository.updateConversationTitle(
                    conversationId,
                    content.take(30).ifEmpty { "New conversation" }
                )
            }.onFailure { e ->
                _error.value = e.message
            }
            
            _isLoading.value = false
        }
    }
    
    fun regenerateLastResponse() {
        val lastUserMessage = _messages.value.lastOrNull { it.role == MessageRole.USER }
        if (lastUserMessage != null) {
            viewModelScope.launch {
                // Delete the last assistant response if exists
                val lastAssistant = _messages.value.lastOrNull { it.role == MessageRole.ASSISTANT }
                lastAssistant?.let { messageRepository.deleteMessage(it.id) }
                
                // Resend the user message
                sendMessage(lastUserMessage.content)
            }
        }
    }
    
    fun clearError() {
        _error.value = null
    }
}
