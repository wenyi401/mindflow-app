package com.mindflow.data.local.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        ConversationEntity::class,
        MessageEntity::class,
        ProviderEntity::class,
        MemoryEntity::class,
        KnowledgeDocumentEntity::class,
        AgentEntity::class,
        ToolEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class MindFlowDatabase : RoomDatabase() {
    
    abstract fun conversationDao(): ConversationDao
    abstract fun messageDao(): MessageDao
    abstract fun providerDao(): ProviderDao
    abstract fun memoryDao(): MemoryDao
    abstract fun knowledgeDao(): KnowledgeDao
    abstract fun agentDao(): AgentDao
    abstract fun toolDao(): ToolDao
    
    companion object {
        private const val DATABASE_NAME = "mindflow_db"
        
        @Volatile
        private var INSTANCE: MindFlowDatabase? = null
        
        fun getInstance(context: Context): MindFlowDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    MindFlowDatabase::class.java,
                    DATABASE_NAME
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
