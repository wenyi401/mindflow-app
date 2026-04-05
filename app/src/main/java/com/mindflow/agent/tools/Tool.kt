package com.mindflow.agent.tools

/**
 * Tool executor interface for agent implementations
 */
interface ToolExecutor {
    val name: String
    val description: String
    val inputSchema: String
}
