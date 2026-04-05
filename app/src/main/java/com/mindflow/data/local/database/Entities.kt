package com.mindflow.data.local.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow

/**
 * Database entities for Room
 */

@Entity(tableName = "conversations")
data class ConversationEntity(
    @PrimaryKey val id: String,
    val title: String?,
    val providerId: String,
    val modelId: String,
    val systemPrompt: String?,
    val temperature: Float,
    val maxTokens: Int,
    val topP: Float?,
    val createdAt: Long,
    val updatedAt: Long
)

@Entity(
    tableName = "messages",
    foreignKeys = [
        ForeignKey(
            entity = ConversationEntity::class,
            parentColumns = ["id"],
            childColumns = ["conversationId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("conversationId")]
)
data class MessageEntity(
    @PrimaryKey val id: String,
    val conversationId: String,
    val role: String,
    val content: String,
    val rawContent: String?,
    val modelId: String?,
    val tokens: Int?,
    val latencyMs: Long?,
    val createdAt: Long,
    val attachmentsJson: String?, // JSON serialized list
    val toolCallsJson: String?,   // JSON serialized list
    val error: String?
)

@Entity(tableName = "providers")
data class ProviderEntity(
    @PrimaryKey val id: String,
    val name: String,
    val type: String,
    val baseUrl: String,
    val apiKey: String,
    val modelId: String,
    val maxTokens: Int,
    val temperature: Float,
    val supportsVision: Boolean,
    val supportsStreaming: Boolean,
    val isEnabled: Boolean
)

@Entity(tableName = "memory_entries")
data class MemoryEntity(
    @PrimaryKey val id: String,
    val type: String,
    val content: String,
    val embedding: String?, // JSON serialized float array
    val conversationId: String?,
    val createdAt: Long,
    val accessedAt: Long,
    val importance: Float,
    val accessCount: Int,
    val metadataJson: String?
)

@Entity(tableName = "knowledge_documents")
data class KnowledgeDocumentEntity(
    @PrimaryKey val id: String,
    val title: String,
    val source: String,
    val sourceType: String,
    val content: String,
    val chunkCount: Int,
    val createdAt: Long,
    val accessedAt: Long,
    val tagsJson: String?
)

@Entity(tableName = "agents")
data class AgentEntity(
    @PrimaryKey val id: String,
    val name: String,
    val description: String,
    val systemPrompt: String,
    val toolsJson: String?, // JSON serialized list
    val memoryStrategy: String,
    val maxIterations: Int,
    val createdAt: Long
)

@Entity(tableName = "tools")
data class ToolEntity(
    @PrimaryKey val id: String,
    val name: String,
    val description: String,
    val inputSchema: String,
    val enabled: Boolean
)

/**
 * DAOs
 */

@Dao
interface ConversationDao {
    @Query("SELECT * FROM conversations ORDER BY updatedAt DESC")
    fun getAllConversations(): Flow<List<ConversationEntity>>
    
    @Query("SELECT * FROM conversations WHERE id = :id")
    suspend fun getConversationById(id: String): ConversationEntity?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertConversation(conversation: ConversationEntity)
    
    @Update
    suspend fun updateConversation(conversation: ConversationEntity)
    
    @Delete
    suspend fun deleteConversation(conversation: ConversationEntity)
    
    @Query("DELETE FROM conversations WHERE id = :id")
    suspend fun deleteConversationById(id: String)
    
    @Query("UPDATE conversations SET updatedAt = :timestamp WHERE id = :id")
    suspend fun updateTimestamp(id: String, timestamp: Long)
    
    @Query("UPDATE conversations SET title = :title, updatedAt = :timestamp WHERE id = :id")
    suspend fun updateTitle(id: String, title: String, timestamp: Long)
}

@Dao
interface MessageDao {
    @Query("SELECT * FROM messages WHERE conversationId = :conversationId ORDER BY createdAt ASC")
    fun getMessagesForConversation(conversationId: String): Flow<List<MessageEntity>>
    
    @Query("SELECT * FROM messages WHERE conversationId = :conversationId ORDER BY createdAt ASC")
    suspend fun getMessagesListForConversation(conversationId: String): List<MessageEntity>
    
    @Query("SELECT * FROM messages WHERE id = :id")
    suspend fun getMessageById(id: String): MessageEntity?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: MessageEntity)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessages(messages: List<MessageEntity>)
    
    @Update
    suspend fun updateMessage(message: MessageEntity)
    
    @Delete
    suspend fun deleteMessage(message: MessageEntity)
    
    @Query("DELETE FROM messages WHERE conversationId = :conversationId")
    suspend fun deleteMessagesForConversation(conversationId: String)
    
    @Query("SELECT * FROM messages WHERE conversationId = :conversationId ORDER BY createdAt DESC LIMIT :limit")
    suspend fun getRecentMessages(conversationId: String, limit: Int): List<MessageEntity>
}

@Dao
interface ProviderDao {
    @Query("SELECT * FROM providers")
    fun getAllProviders(): Flow<List<ProviderEntity>>
    
    @Query("SELECT * FROM providers WHERE isEnabled = 1")
    fun getEnabledProviders(): Flow<List<ProviderEntity>>
    
    @Query("SELECT * FROM providers WHERE id = :id")
    suspend fun getProviderById(id: String): ProviderEntity?
    
    @Query("SELECT * FROM providers WHERE id = :id")
    fun getProviderByIdFlow(id: String): Flow<ProviderEntity?>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProvider(provider: ProviderEntity)
    
    @Update
    suspend fun updateProvider(provider: ProviderEntity)
    
    @Delete
    suspend fun deleteProvider(provider: ProviderEntity)
}

@Dao
interface MemoryDao {
    @Query("SELECT * FROM memory_entries ORDER BY accessedAt DESC")
    fun getAllMemories(): Flow<List<MemoryEntity>>
    
    @Query("SELECT * FROM memory_entries WHERE type = :type ORDER BY accessedAt DESC")
    fun getMemoriesByType(type: String): Flow<List<MemoryEntity>>
    
    @Query("SELECT * FROM memory_entries WHERE conversationId = :conversationId ORDER BY accessedAt DESC")
    fun getMemoriesForConversation(conversationId: String): Flow<List<MemoryEntity>>
    
    @Query("SELECT * FROM memory_entries WHERE id = :id")
    suspend fun getMemoryById(id: String): MemoryEntity?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMemory(memory: MemoryEntity)
    
    @Update
    suspend fun updateMemory(memory: MemoryEntity)
    
    @Delete
    suspend fun deleteMemory(memory: MemoryEntity)
    
    @Query("DELETE FROM memory_entries WHERE createdAt < :timestamp AND importance < :threshold")
    suspend fun pruneOldMemories(timestamp: Long, threshold: Float)
    
    @Query("UPDATE memory_entries SET accessedAt = :timestamp, accessCount = accessCount + 1 WHERE id = :id")
    suspend fun updateAccess(id: String, timestamp: Long)
    
    @Query("SELECT * FROM memory_entries ORDER BY importance DESC LIMIT :limit")
    suspend fun getTopMemories(limit: Int): List<MemoryEntity>
}

@Dao
interface KnowledgeDao {
    @Query("SELECT * FROM knowledge_documents ORDER BY accessedAt DESC")
    fun getAllDocuments(): Flow<List<KnowledgeDocumentEntity>>
    
    @Query("SELECT * FROM knowledge_documents WHERE id = :id")
    suspend fun getDocumentById(id: String): KnowledgeDocumentEntity?
    
    @Query("SELECT * FROM knowledge_documents WHERE sourceType = :sourceType ORDER BY accessedAt DESC")
    fun getDocumentsBySource(sourceType: String): Flow<List<KnowledgeDocumentEntity>>
    
    @Query("SELECT * FROM knowledge_documents WHERE title LIKE '%' || :query || '%' OR content LIKE '%' || :query || '%'")
    suspend fun searchDocuments(query: String): List<KnowledgeDocumentEntity>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDocument(document: KnowledgeDocumentEntity)
    
    @Update
    suspend fun updateDocument(document: KnowledgeDocumentEntity)
    
    @Delete
    suspend fun deleteDocument(document: KnowledgeDocumentEntity)
    
    @Query("UPDATE knowledge_documents SET accessedAt = :timestamp WHERE id = :id")
    suspend fun updateAccess(id: String, timestamp: Long)
}

@Dao
interface AgentDao {
    @Query("SELECT * FROM agents ORDER BY createdAt DESC")
    fun getAllAgents(): Flow<List<AgentEntity>>
    
    @Query("SELECT * FROM agents WHERE id = :id")
    suspend fun getAgentById(id: String): AgentEntity?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAgent(agent: AgentEntity)
    
    @Update
    suspend fun updateAgent(agent: AgentEntity)
    
    @Delete
    suspend fun deleteAgent(agent: AgentEntity)
}

@Dao
interface ToolDao {
    @Query("SELECT * FROM tools WHERE enabled = 1")
    fun getEnabledTools(): Flow<List<ToolEntity>>
    
    @Query("SELECT * FROM tools WHERE id IN (:ids) AND enabled = 1")
    suspend fun getToolsByIds(ids: List<String>): List<ToolEntity>
    
    @Query("SELECT * FROM tools WHERE id = :id")
    suspend fun getToolById(id: String): ToolEntity?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTool(tool: ToolEntity)
    
    @Update
    suspend fun updateTool(tool: ToolEntity)
    
    @Delete
    suspend fun deleteTool(tool: ToolEntity)
}
