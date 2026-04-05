package com.mindflow.data.local.database

import com.mindflow.domain.model.*
import com.mindflow.domain.repository.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Repository implementations using Room database
 */

class ConversationRepositoryImpl(
    private val conversationDao: ConversationDao,
    private val json: Json
) : ConversationRepository {
    
    override fun getAllConversations(): Flow<List<Conversation>> {
        return conversationDao.getAllConversations().map { entities ->
            entities.map { it.toDomain() }
        }
    }
    
    override suspend fun getConversationById(id: String): Conversation? {
        return conversationDao.getConversationById(id)?.toDomain()
    }
    
    override suspend fun createConversation(conversation: Conversation): Result<Conversation> {
        return runCatching {
            conversationDao.insertConversation(conversation.toEntity())
            conversation
        }
    }
    
    override suspend fun updateConversation(conversation: Conversation): Result<Unit> {
        return runCatching {
            conversationDao.updateConversation(conversation.toEntity())
        }
    }
    
    override suspend fun deleteConversation(id: String): Result<Unit> {
        return runCatching {
            conversationDao.deleteConversationById(id)
        }
    }
    
    override suspend fun updateConversationTitle(id: String, title: String): Result<Unit> {
        return runCatching {
            conversationDao.updateTitle(id, title, System.currentTimeMillis())
        }
    }
}

class MessageRepositoryImpl(
    private val messageDao: MessageDao,
    private val json: Json
) : MessageRepository {
    
    override fun getMessagesForConversation(conversationId: String): Flow<List<Message>> {
        return messageDao.getMessagesForConversation(conversationId).map { entities ->
            entities.map { it.toDomain(json) }
        }
    }
    
    override suspend fun getRecentMessages(conversationId: String, limit: Int): List<Message> {
        return messageDao.getRecentMessages(conversationId, limit).map { it.toDomain(json) }
    }
    
    override suspend fun sendMessage(message: Message): Result<Message> {
        return runCatching {
            messageDao.insertMessage(message.toEntity(json))
            message
        }
    }
    
    override suspend fun updateMessage(message: Message): Result<Unit> {
        return runCatching {
            messageDao.updateMessage(message.toEntity(json))
        }
    }
    
    override suspend fun deleteMessage(id: String): Result<Unit> {
        return runCatching {
            messageDao.getMessageById(id)?.let { messageDao.deleteMessage(it) }
        }
    }
    
    override suspend fun deleteMessagesForConversation(conversationId: String): Result<Unit> {
        return runCatching {
            messageDao.deleteMessagesForConversation(conversationId)
        }
    }
}

class ProviderRepositoryImpl(
    private val providerDao: ProviderDao
) : ProviderRepository {
    
    override fun getAllProviders(): Flow<List<AIProvider>> {
        return providerDao.getAllProviders().map { entities ->
            entities.map { it.toDomain() }
        }
    }
    
    override fun getEnabledProviders(): Flow<List<AIProvider>> {
        return providerDao.getEnabledProviders().map { entities ->
            entities.map { it.toDomain() }
        }
    }
    
    override suspend fun getProviderById(id: String): AIProvider? {
        return providerDao.getProviderById(id)?.toDomain()
    }
    
    override fun getProviderByIdFlow(id: String): Flow<AIProvider?> {
        return providerDao.getProviderByIdFlow(id).map { it?.toDomain() }
    }
    
    override suspend fun saveProvider(provider: AIProvider): Result<Unit> {
        return runCatching {
            providerDao.insertProvider(provider.toEntity())
        }
    }
    
    override suspend fun deleteProvider(id: String): Result<Unit> {
        return runCatching {
            providerDao.getProviderById(id)?.let { providerDao.deleteProvider(it) }
        }
    }
    
    override suspend fun validateProvider(provider: AIProvider): Result<Boolean> {
        // Validation logic would make an API call to test the provider
        return Result.success(true)
    }
}

class MemoryRepositoryImpl(
    private val memoryDao: MemoryDao,
    private val json: Json
) : MemoryRepository {
    
    override fun getAllMemories(): Flow<List<MemoryEntry>> {
        return memoryDao.getAllMemories().map { entities ->
            entities.map { it.toDomain(json) }
        }
    }
    
    override fun getMemoriesByType(type: MemoryType): Flow<List<MemoryEntry>> {
        return memoryDao.getMemoriesByType(type.name).map { entities ->
            entities.map { it.toDomain(json) }
        }
    }
    
    override fun getMemoriesForConversation(conversationId: String): Flow<List<MemoryEntry>> {
        return memoryDao.getMemoriesForConversation(conversationId).map { entities ->
            entities.map { it.toDomain(json) }
        }
    }
    
    override suspend fun getMemoryById(id: String): MemoryEntry? {
        return memoryDao.getMemoryById(id)?.toDomain(json)
    }
    
    override suspend fun storeMemory(entry: MemoryEntry): Result<Unit> {
        return runCatching {
            memoryDao.insertMemory(entry.toEntity(json))
        }
    }
    
    override suspend fun updateMemory(entry: MemoryEntry): Result<Unit> {
        return runCatching {
            memoryDao.updateMemory(entry.toEntity(json))
        }
    }
    
    override suspend fun deleteMemory(id: String): Result<Unit> {
        return runCatching {
            memoryDao.getMemoryById(id)?.let { memoryDao.deleteMemory(it) }
        }
    }
    
    override suspend fun searchMemories(query: String, limit: Int): Result<List<MemoryEntry>> {
        return runCatching {
            // Simple text search - vector search would use embeddings
            memoryDao.getTopMemories(limit).map { it.toDomain(json) }
        }
    }
    
    override suspend fun pruneOldMemories(olderThanDays: Int, minImportance: Float): Result<Int> {
        val cutoffTime = System.currentTimeMillis() - (olderThanDays * 24 * 60 * 60 * 1000L)
        return runCatching {
            memoryDao.pruneOldMemories(cutoffTime, minImportance)
        }
    }
    
    override suspend fun consolidateMemories(): Result<Int> {
        // Memory consolidation logic - summarize and merge similar memories
        return Result.success(0)
    }
}

class KnowledgeRepositoryImpl(
    private val knowledgeDao: KnowledgeDao,
    private val json: Json
) : KnowledgeRepository {
    
    override fun getAllDocuments(): Flow<List<KnowledgeDocument>> {
        return knowledgeDao.getAllDocuments().map { entities ->
            entities.map { it.toDomain(json) }
        }
    }
    
    override suspend fun getDocumentById(id: String): KnowledgeDocument? {
        return knowledgeDao.getDocumentById(id)?.toDomain(json)
    }
    
    override fun getDocumentsBySource(sourceType: SourceType): Flow<List<KnowledgeDocument>> {
        return knowledgeDao.getDocumentsBySource(sourceType.name).map { entities ->
            entities.map { it.toDomain(json) }
        }
    }
    
    override suspend fun searchDocuments(query: String): Result<List<KnowledgeDocument>> {
        return runCatching {
            knowledgeDao.searchDocuments(query).map { it.toDomain(json) }
        }
    }
    
    override suspend fun addDocument(document: KnowledgeDocument): Result<Unit> {
        return runCatching {
            knowledgeDao.insertDocument(document.toEntity(json))
        }
    }
    
    override suspend fun updateDocument(document: KnowledgeDocument): Result<Unit> {
        return runCatching {
            knowledgeDao.updateDocument(document.toEntity(json))
        }
    }
    
    override suspend fun deleteDocument(id: String): Result<Unit> {
        return runCatching {
            knowledgeDao.getDocumentById(id)?.let { knowledgeDao.deleteDocument(it) }
        }
    }
    
    override suspend fun queryKnowledge(query: String, limit: Int): Result<List<KnowledgeDocument>> {
        return runCatching {
            knowledgeDao.searchDocuments(query).map { it.toDomain(json) }.take(limit)
        }
    }
}

class AgentRepositoryImpl(
    private val agentDao: AgentDao,
    private val toolDao: ToolDao,
    private val json: Json
) : AgentRepository {
    
    override fun getAllAgents(): Flow<List<Agent>> {
        return agentDao.getAllAgents().map { entities ->
            entities.map { it.toDomain(json) }
        }
    }
    
    override suspend fun getAgentById(id: String): Agent? {
        return agentDao.getAgentById(id)?.toDomain(json)
    }
    
    override suspend fun createAgent(agent: Agent): Result<Unit> {
        return runCatching {
            agentDao.insertAgent(agent.toEntity(json))
        }
    }
    
    override suspend fun updateAgent(agent: Agent): Result<Unit> {
        return runCatching {
            agentDao.updateAgent(agent.toEntity(json))
        }
    }
    
    override suspend fun deleteAgent(id: String): Result<Unit> {
        return runCatching {
            agentDao.getAgentById(id)?.let { agentDao.deleteAgent(it) }
        }
    }
    
    override suspend fun getToolsForAgent(agentId: String): List<Tool> {
        val agent = agentDao.getAgentById(agentId)?.toDomain(json)
        return agent?.tools?.let { toolDao.getToolsByIds(it) }?.map { it.toDomain() } ?: emptyList()
    }
}

class ToolRepositoryImpl(
    private val toolDao: ToolDao
) : ToolRepository {
    
    override fun getEnabledTools(): Flow<List<Tool>> {
        return toolDao.getEnabledTools().map { entities ->
            entities.map { it.toDomain() }
        }
    }
    
    override suspend fun getToolsByIds(ids: List<String>): List<Tool> {
        return toolDao.getToolsByIds(ids).map { it.toDomain() }
    }
    
    override suspend fun getToolById(id: String): Tool? {
        return toolDao.getToolById(id)?.toDomain()
    }
    
    override suspend fun registerTool(tool: Tool): Result<Unit> {
        return runCatching {
            toolDao.insertTool(tool.toEntity())
        }
    }
    
    override suspend fun unregisterTool(id: String): Result<Unit> {
        return runCatching {
            toolDao.getToolById(id)?.let { toolDao.deleteTool(it) }
        }
    }
    
    override suspend fun executeTool(toolId: String, input: String): Result<ToolResult> {
        // Tool execution would be handled by the tool system
        return Result.success(ToolResult(toolId, true, ""))
    }
}

// Extension functions for mapping

private fun ConversationEntity.toDomain() = Conversation(
    id = id,
    title = title,
    providerId = providerId,
    modelId = modelId,
    systemPrompt = systemPrompt,
    temperature = temperature,
    maxTokens = maxTokens,
    topP = topP,
    createdAt = createdAt,
    updatedAt = updatedAt
)

private fun Conversation.toEntity() = ConversationEntity(
    id = id,
    title = title,
    providerId = providerId,
    modelId = modelId,
    systemPrompt = systemPrompt,
    temperature = temperature,
    maxTokens = maxTokens,
    topP = topP,
    createdAt = createdAt,
    updatedAt = updatedAt
)

private fun MessageEntity.toDomain(json: Json): Message {
    val attachments = attachmentsJson?.let {
        runCatching { json.decodeFromString<List<Attachment>>(it) }.getOrNull()
    } ?: emptyList()
    val toolCalls = toolCallsJson?.let {
        runCatching { json.decodeFromString<List<ToolCall>>(it) }.getOrNull()
    } ?: emptyList()
    
    return Message(
        id = id,
        conversationId = conversationId,
        role = MessageRole.valueOf(role),
        content = content,
        rawContent = rawContent,
        modelId = modelId,
        tokens = tokens,
        latencyMs = latencyMs,
        createdAt = createdAt,
        attachments = attachments,
        toolCalls = toolCalls,
        error = error
    )
}

private fun Message.toEntity(json: Json) = MessageEntity(
    id = id,
    conversationId = conversationId,
    role = role.name,
    content = content,
    rawContent = rawContent,
    modelId = modelId,
    tokens = tokens,
    latencyMs = latencyMs,
    createdAt = createdAt,
    attachmentsJson = json.encodeToString(attachments).takeIf { attachments.isNotEmpty() },
    toolCallsJson = json.encodeToString(toolCalls).takeIf { toolCalls.isNotEmpty() },
    error = error
)

private fun ProviderEntity.toDomain() = AIProvider(
    id = id,
    name = name,
    type = ProviderType.valueOf(type),
    baseUrl = baseUrl,
    apiKey = apiKey,
    modelId = modelId,
    maxTokens = maxTokens,
    temperature = temperature,
    supportsVision = supportsVision,
    supportsStreaming = supportsStreaming,
    isEnabled = isEnabled
)

private fun AIProvider.toEntity() = ProviderEntity(
    id = id,
    name = name,
    type = type.name,
    baseUrl = baseUrl,
    apiKey = apiKey,
    modelId = modelId,
    maxTokens = maxTokens,
    temperature = temperature,
    supportsVision = supportsVision,
    supportsStreaming = supportsStreaming,
    isEnabled = isEnabled
)

private fun MemoryEntity.toDomain(json: Json): MemoryEntry {
    val metadata = metadataJson?.let {
        runCatching { json.decodeFromString<Map<String, String>>(it) }.getOrNull()
    } ?: emptyMap()
    
    return MemoryEntry(
        id = id,
        type = MemoryType.valueOf(type),
        content = content,
        conversationId = conversationId,
        createdAt = createdAt,
        accessedAt = accessedAt,
        importance = importance,
        accessCount = accessCount,
        metadata = metadata
    )
}

private fun MemoryEntry.toEntity(json: Json) = MemoryEntity(
    id = id,
    type = type.name,
    content = content,
    embedding = null,
    conversationId = conversationId,
    createdAt = createdAt,
    accessedAt = accessedAt,
    importance = importance,
    accessCount = accessCount,
    metadataJson = json.encodeToString(metadata).takeIf { metadata.isNotEmpty() }
)

private fun KnowledgeDocumentEntity.toDomain(json: Json): KnowledgeDocument {
    val tags = tagsJson?.let {
        runCatching { json.decodeFromString<List<String>>(it) }.getOrNull()
    } ?: emptyList()
    
    return KnowledgeDocument(
        id = id,
        title = title,
        source = source,
        sourceType = SourceType.valueOf(sourceType),
        content = content,
        chunkCount = chunkCount,
        createdAt = createdAt,
        accessedAt = accessedAt,
        tags = tags
    )
}

private fun KnowledgeDocument.toEntity(json: Json) = KnowledgeDocumentEntity(
    id = id,
    title = title,
    source = source,
    sourceType = sourceType.name,
    content = content,
    chunkCount = chunkCount,
    createdAt = createdAt,
    accessedAt = accessedAt,
    tagsJson = json.encodeToString(tags).takeIf { tags.isNotEmpty() }
)

private fun AgentEntity.toDomain(json: Json): Agent {
    val tools = toolsJson?.let {
        runCatching { json.decodeFromString<List<String>>(it) }.getOrNull()
    } ?: emptyList()
    
    return Agent(
        id = id,
        name = name,
        description = description,
        systemPrompt = systemPrompt,
        tools = tools,
        memoryStrategy = MemoryStrategy.valueOf(memoryStrategy),
        maxIterations = maxIterations,
        createdAt = createdAt
    )
}

private fun Agent.toEntity(json: Json) = AgentEntity(
    id = id,
    name = name,
    description = description,
    systemPrompt = systemPrompt,
    toolsJson = json.encodeToString(tools).takeIf { tools.isNotEmpty() },
    memoryStrategy = memoryStrategy.name,
    maxIterations = maxIterations,
    createdAt = createdAt
)

private fun ToolEntity.toDomain() = Tool(
    id = id,
    name = name,
    description = description,
    inputSchema = inputSchema,
    enabled = enabled
)

private fun Tool.toEntity() = ToolEntity(
    id = id,
    name = name,
    description = description,
    inputSchema = inputSchema,
    enabled = enabled
)
