package com.mindflow.di

import com.mindflow.agent.AgentEngine
import com.mindflow.agent.memory.MemoryManager
import com.mindflow.data.local.database.*
import com.mindflow.domain.repository.*
import com.mindflow.domain.usecase.AIService
import kotlinx.serialization.json.Json
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

/**
 * Koin dependency injection modules
 */

val appModule = module {
    
    // JSON serialization
    single {
        Json {
            ignoreUnknownKeys = true
            isLenient = true
            encodeDefaults = true
            prettyPrint = false
        }
    }
    
    // Database
    single { MindFlowDatabase.getInstance(androidContext()) }
    single { get<MindFlowDatabase>().conversationDao() }
    single { get<MindFlowDatabase>().messageDao() }
    single { get<MindFlowDatabase>().providerDao() }
    single { get<MindFlowDatabase>().memoryDao() }
    single { get<MindFlowDatabase>().knowledgeDao() }
    single { get<MindFlowDatabase>().agentDao() }
    single { get<MindFlowDatabase>().toolDao() }
    
    // Repositories
    single<ConversationRepository> { 
        ConversationRepositoryImpl(get(), get()) 
    }
    single<MessageRepository> { 
        MessageRepositoryImpl(get(), get()) 
    }
    single<ProviderRepository> { 
        ProviderRepositoryImpl(get()) 
    }
    single<MemoryRepository> { 
        MemoryRepositoryImpl(get(), get()) 
    }
    single<KnowledgeRepository> { 
        KnowledgeRepositoryImpl(get(), get()) 
    }
    single<AgentRepository> { 
        AgentRepositoryImpl(get(), get(), get()) 
    }
    single<ToolRepository> { 
        ToolRepositoryImpl(get()) 
    }
    
    // Services
    single { 
        AIService(get(), get(), get()) 
    }
    
    // Agent components
    single { 
        MemoryManager(get(), get()) 
    }
    single {
        AgentEngine(get(), get(), get(), get(), get(), androidContext())
    }
}

/**
 * ViewModel module for Koin
 */
val viewModelModule = module {
    // ViewModels will be added here
}
