package com.mindflow.domain.repository

import com.mindflow.domain.model.*
import kotlinx.coroutines.flow.Flow

/**
 * Repository interfaces for the domain layer
 */

interface ConversationRepository {
    fun getAllConversations(): Flow<List<Conversation>>
    suspend fun getConversationById(id: String): Conversation?
    suspend fun createConversation(conversation: Conversation): Result<Conversation>
    suspend fun updateConversation(conversation: Conversation): Result<Unit>
    suspend fun deleteConversation(id: String): Result<Unit>
    suspend fun updateConversationTitle(id: String, title: String): Result<Unit>
}

interface MessageRepository {
    fun getMessagesForConversation(conversationId: String): Flow<List<Message>>
    suspend fun getRecentMessages(conversationId: String, limit: Int): List<Message>
    suspend fun sendMessage(message: Message): Result<Message>
    suspend fun updateMessage(message: Message): Result<Unit>
    suspend fun deleteMessage(id: String): Result<Unit>
    suspend fun deleteMessagesForConversation(conversationId: String): Result<Unit>
}

interface ProviderRepository {
    fun getAllProviders(): Flow<List<AIProvider>>
    fun getEnabledProviders(): Flow<List<AIProvider>>
    suspend fun getProviderById(id: String): AIProvider?
    fun getProviderByIdFlow(id: String): Flow<AIProvider?>
    suspend fun saveProvider(provider: AIProvider): Result<Unit>
    suspend fun deleteProvider(id: String): Result<Unit>
    suspend fun validateProvider(provider: AIProvider): Result<Boolean>
}

interface MemoryRepository {
    fun getAllMemories(): Flow<List<MemoryEntry>>
    fun getMemoriesByType(type: MemoryType): Flow<List<MemoryEntry>>
    fun getMemoriesForConversation(conversationId: String): Flow<List<MemoryEntry>>
    suspend fun getMemoryById(id: String): MemoryEntry?
    suspend fun storeMemory(entry: MemoryEntry): Result<Unit>
    suspend fun updateMemory(entry: MemoryEntry): Result<Unit>
    suspend fun deleteMemory(id: String): Result<Unit>
    suspend fun searchMemories(query: String, limit: Int = 10): Result<List<MemoryEntry>>
    suspend fun pruneOldMemories(olderThanDays: Int = 30, minImportance: Float = 0.3f): Result<Int>
    suspend fun consolidateMemories(): Result<Int>
}

interface KnowledgeRepository {
    fun getAllDocuments(): Flow<List<KnowledgeDocument>>
    suspend fun getDocumentById(id: String): KnowledgeDocument?
    fun getDocumentsBySource(sourceType: SourceType): Flow<List<KnowledgeDocument>>
    suspend fun searchDocuments(query: String): Result<List<KnowledgeDocument>>
    suspend fun addDocument(document: KnowledgeDocument): Result<Unit>
    suspend fun updateDocument(document: KnowledgeDocument): Result<Unit>
    suspend fun deleteDocument(id: String): Result<Unit>
    suspend fun queryKnowledge(query: String, limit: Int = 5): Result<List<KnowledgeDocument>>
}

interface AgentRepository {
    fun getAllAgents(): Flow<List<Agent>>
    suspend fun getAgentById(id: String): Agent?
    suspend fun createAgent(agent: Agent): Result<Unit>
    suspend fun updateAgent(agent: Agent): Result<Unit>
    suspend fun deleteAgent(id: String): Result<Unit>
    suspend fun getToolsForAgent(agentId: String): List<Tool>
}

interface ToolRepository {
    fun getEnabledTools(): Flow<List<Tool>>
    suspend fun getToolsByIds(ids: List<String>): List<Tool>
    suspend fun getToolById(id: String): Tool?
    suspend fun registerTool(tool: Tool): Result<Unit>
    suspend fun unregisterTool(id: String): Result<Unit>
    suspend fun executeTool(toolId: String, input: String): Result<ToolResult>
}
