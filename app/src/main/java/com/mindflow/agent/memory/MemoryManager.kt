package com.mindflow.agent.memory

import com.mindflow.domain.model.*
import com.mindflow.domain.repository.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.*
import java.util.UUID

/**
 * Memory Manager - Handles short-term and long-term memory for AI conversations
 */
class MemoryManager(
    private val memoryRepository: MemoryRepository,
    private val messageRepository: MessageRepository
) {
    
    companion object {
        private const val MAX_WORKING_MEMORY = 20 // Max messages in working context
        private const val SUMMARIZATION_THRESHOLD = 10 // Messages before summarization
        private const val LONG_TERM_IMPORTANCE_THRESHOLD = 0.6f
    }
    
    /**
     * Working memory context for current conversation
     */
    private val workingMemory = mutableMapOf<String, List<Message>>()
    
    /**
     * Update working memory with new messages
     */
    suspend fun updateWorkingContext(conversationId: String, messages: List<Message>) {
        // Keep only the most recent messages
        val contextMessages = messages.takeLast(MAX_WORKING_MEMORY)
        workingMemory[conversationId] = contextMessages
        
        // Check if we need to summarize older messages
        if (messages.size > SUMMARIZATION_THRESHOLD) {
            val olderMessages = messages.dropLast(MAX_WORKING_MEMORY)
            if (olderMessages.isNotEmpty()) {
                summarizeAndStore(olderMessages, conversationId)
            }
        }
    }
    
    /**
     * Get working context for a conversation
     */
    fun getWorkingContext(conversationId: String): List<Message> {
        return workingMemory[conversationId] ?: emptyList()
    }
    
    /**
     * Clear working memory for a conversation
     */
    fun clearWorkingContext(conversationId: String) {
        workingMemory.remove(conversationId)
    }
    
    /**
     * Store messages to short-term memory
     */
    suspend fun storeShortTerm(conversationId: String, messages: List<Message>) {
        val memoryEntries = messages.map { message ->
            MemoryEntry(
                id = UUID.randomUUID().toString(),
                type = MemoryType.CONVERSATION,
                content = "[${message.role.name}] ${message.content}",
                conversationId = conversationId,
                importance = 0.5f,
                metadata = mapOf(
                    "messageId" to message.id,
                    "role" to message.role.name
                )
            )
        }
        
        memoryEntries.forEach { entry ->
            memoryRepository.storeMemory(entry)
        }
    }
    
    /**
     * Retrieve short-term memories
     */
    suspend fun retrieveShortTerm(query: String, limit: Int): List<MemoryEntry> {
        return memoryRepository.searchMemories(query, limit).getOrDefault(emptyList())
            .filter { it.type == MemoryType.CONVERSATION }
    }
    
    /**
     * Store to long-term memory with importance scoring
     */
    suspend fun storeLongTerm(
        content: String,
        embedding: FloatArray? = null,
        metadata: Map<String, String> = emptyMap(),
        importance: Float = 0.5f
    ): MemoryEntry {
        val entry = MemoryEntry(
            id = UUID.randomUUID().toString(),
            type = MemoryType.FACT,
            content = content,
            importance = importance,
            metadata = metadata
        )
        
        memoryRepository.storeMemory(entry)
        return entry
    }
    
    /**
     * Retrieve from long-term memory
     */
    suspend fun retrieveLongTerm(query: String, limit: Int, threshold: Float): List<MemoryEntry> {
        return memoryRepository.searchMemories(query, limit).getOrDefault(emptyList())
            .filter { it.importance >= threshold }
    }
    
    /**
     * Store user preference
     */
    suspend fun storePreference(key: String, value: String) {
        val entry = MemoryEntry(
            id = UUID.randomUUID().toString(),
            type = MemoryType.PREFERENCE,
            content = "$key: $value",
            importance = 0.8f,
            metadata = mapOf("key" to key)
        )
        memoryRepository.storeMemory(entry)
    }
    
    /**
     * Get user preferences
     */
    suspend fun getPreferences(): List<MemoryEntry> {
        return memoryRepository.getMemoriesByType(MemoryType.PREFERENCE).first()
    }
    
    /**
     * Consolidate memories - merge similar entries and update importance
     */
    suspend fun consolidateMemories(): Int {
        val allMemories = memoryRepository.getAllMemories().first()
        val grouped = allMemories.groupBy { it.type }
        var consolidated = 0
        
        grouped.forEach { (type, memories) ->
            val byContent = memories.groupBy { normalizeContent(it.content) }
            
            byContent.forEach { (_, entries) ->
                if (entries.size > 1) {
                    // Keep the most important one and update
                    val keep = entries.maxByOrNull { it.importance }!!
                    val avgImportance = entries.map { it.importance }.average().toFloat()
                    val totalAccess = entries.sumOf { it.accessCount }
                    
                    memoryRepository.updateMemory(
                        keep.copy(
                            importance = avgImportance,
                            accessCount = totalAccess
                        )
                    )
                    
                    // Delete the rest
                    entries.filter { it.id != keep.id }.forEach { entry ->
                        memoryRepository.deleteMemory(entry.id)
                    }
                    
                    consolidated += entries.size - 1
                }
            }
        }
        
        return consolidated
    }
    
    /**
     * Prune old, low-importance memories
     */
    suspend fun pruneOldMemories(olderThanDays: Int = 30, minImportance: Float = 0.3f): Int {
        return memoryRepository.pruneOldMemories(olderThanDays, minImportance).getOrDefault(0)
    }
    
    /**
     * Extract and store key facts from conversation
     */
    suspend fun extractKeyFacts(conversationId: String, messages: List<Message>) {
        // Simple fact extraction based on patterns
        messages.forEach { message ->
            if (message.role == MessageRole.USER) {
                val facts = extractFacts(message.content)
                facts.forEach { fact ->
                    storeLongTerm(
                        content = fact,
                        importance = 0.7f,
                        metadata = mapOf("conversationId" to conversationId)
                    )
                }
            }
        }
    }
    
    /**
     * Summarize a conversation segment
     */
    private suspend fun summarizeAndStore(messages: List<Message>, conversationId: String) {
        if (messages.isEmpty()) return
        
        val summary = generateSummary(messages)
        val entry = MemoryEntry(
            id = UUID.randomUUID().toString(),
            type = MemoryType.CONVERSATION,
            content = "[Summary] $summary",
            conversationId = conversationId,
            importance = 0.6f,
            metadata = mapOf(
                "messageCount" to messages.size.toString(),
                "type" to "summary"
            )
        )
        
        memoryRepository.storeMemory(entry)
    }
    
    /**
     * Generate a simple summary of messages
     */
    private fun generateSummary(messages: List<Message>): String {
        val userMessages = messages.filter { it.role == MessageRole.USER }
        val assistantMessages = messages.filter { it.role == MessageRole.ASSISTANT }
        
        val topics = extractTopics(userMessages)
        val duration = if (messages.isNotEmpty()) {
            val timeSpan = messages.last().createdAt - messages.first().createdAt
            formatDuration(timeSpan)
        } else ""
        
        return buildString {
            append("Conversation about: ${topics.joinToString(", ")}. ")
            append("${userMessages.size} user messages, ${assistantMessages.size} assistant responses. ")
            append("Duration: $duration.")
        }
    }
    
    /**
     * Extract topics from messages
     */
    private fun extractTopics(messages: List<Message>): List<String> {
        // Simple keyword extraction
        val keywords = mutableSetOf<String>()
        val topicPatterns = listOf(
            "about", "regarding", "concerning", "help with", "question about",
            "explain", "tell me about", "how to", "what is", "why"
        )
        
        messages.forEach { message ->
            topicPatterns.forEach { pattern ->
                if (message.content.contains(pattern, ignoreCase = true)) {
                    val index = message.content.indexOf(pattern, ignoreCase = true)
                    val start = maxOf(0, index - 10)
                    val end = minOf(message.content.length, index + pattern.length + 20)
                    val context = message.content.substring(start, end)
                    keywords.add(context.trim())
                }
            }
        }
        
        return keywords.take(5)
    }
    
    /**
     * Extract facts from text
     */
    private fun extractFacts(text: String): List<String> {
        val facts = mutableListOf<String>()
        
        // Pattern-based extraction (simplified)
        val patterns = listOf(
            """my (name|email|phone|address) is (.+)""" to "User shared personal info",
            """I (like|love|hate|prefer) (.+)""" to "User preference",
            """I am (.+) years old""" to "User age",
            """I work at (.+)""" to "User workplace"
        )
        
        patterns.forEach { (pattern, factType) ->
            val regex = Regex(pattern, RegexOption.IGNORE_CASE)
            regex.findAll(text).forEach { match ->
                facts.add("${match.groupValues[1]}: ${match.groupValues[2]}")
            }
        }
        
        return facts
    }
    
    /**
     * Format duration in milliseconds to human-readable string
     */
    private fun formatDuration(ms: Long): String {
        val seconds = ms / 1000
        val minutes = seconds / 60
        val hours = minutes / 60
        
        return when {
            hours > 0 -> "${hours}h ${minutes % 60}m"
            minutes > 0 -> "${minutes}m"
            else -> "${seconds}s"
        }
    }
    
    /**
     * Normalize content for comparison
     */
    private fun normalizeContent(content: String): String {
        return content
            .lowercase()
            .replace(Regex("[^a-z0-9 ]"), "")
            .replace(Regex("\\s+"), " ")
            .trim()
    }
    
    /**
     * Build context for RAG (Retrieval Augmented Generation)
     */
    suspend fun buildRAGContext(query: String, limit: Int = 5): String {
        val memories = memoryRepository.searchMemories(query, limit).getOrDefault(emptyList())
        return memories.joinToString("\n\n") { memory ->
            "[${memory.type.name}] ${memory.content}"
        }
    }
    
    /**
     * Get memory statistics
     */
    suspend fun getMemoryStats(): MemoryStats {
        val all = memoryRepository.getAllMemories().first()
        
        return MemoryStats(
            totalMemories = all.size,
            byType = all.groupBy { it.type }.mapValues { it.value.size },
            averageImportance = if (all.isNotEmpty()) all.map { it.importance }.average().toFloat() else 0f,
            recentMemories = all.sortedByDescending { it.accessedAt }.take(5).map { it.id }
        )
    }
}

/**
 * Memory statistics
 */
data class MemoryStats(
    val totalMemories: Int,
    val byType: Map<MemoryType, Int>,
    val averageImportance: Float,
    val recentMemories: List<String>
)
