package com.mindflow.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.mindflow.ui.screens.agent.AgentListScreen
import com.mindflow.ui.screens.agent.AgentWorkspaceScreen
import com.mindflow.ui.screens.chat.ChatListScreen
import com.mindflow.ui.screens.chat.ChatScreen
import com.mindflow.ui.screens.knowledge.KnowledgeScreen
import com.mindflow.ui.screens.settings.ProviderSettingsScreen
import com.mindflow.ui.screens.settings.SettingsScreen

/**
 * Navigation destinations
 */
sealed class Screen(val route: String) {
    object ChatList : Screen("chat_list")
    object Chat : Screen("chat/{conversationId}") {
        fun createRoute(conversationId: String) = "chat/$conversationId"
    }
    object AgentList : Screen("agent_list")
    object AgentWorkspace : Screen("agent/{agentId}") {
        fun createRoute(agentId: String) = "agent/$agentId"
    }
    object Knowledge : Screen("knowledge")
    object Settings : Screen("settings")
    object ProviderSettings : Screen("provider_settings")
}

/**
 * Bottom navigation items
 */
sealed class BottomNavItem(
    val route: String,
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageIcon
) {
    object Chat : BottomNavItem(
        route = Screen.ChatList.route,
        title = "Chat",
        selectedIcon = Icons.Filled.Chat,
        unselectedIcon = Icons.Outlined.Chat
    )
    object Agents : BottomNavItem(
        route = Screen.AgentList.route,
        title = "Agents",
        selectedIcon = Icons.Filled.SmartToy,
        unselectedIcon = Icons.Outlined.SmartToy
    )
    object Knowledge : BottomNavItem(
        route = Screen.Knowledge.route,
        title = "Knowledge",
        selectedIcon = Icons.Filled.LibraryBooks,
        unselectedIcon = Icons.Outlined.LibraryBooks
    )
    object Settings : BottomNavItem(
        route = Screen.Settings.route,
        title = "Settings",
        selectedIcon = Icons.Filled.Settings,
        unselectedIcon = Icons.Outlined.Settings
    )
}

/**
 * Main navigation scaffold with bottom bar
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MindFlowNavigation() {
    val navController = rememberNavController()
    
    val bottomNavItems = listOf(
        BottomNavItem.Chat,
        BottomNavItem.Agents,
        BottomNavItem.Knowledge,
        BottomNavItem.Settings
    )
    
    Scaffold(
        bottomBar = {
            NavigationBar {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination
                
                bottomNavItems.forEach { item ->
                    val selected = currentDestination?.hierarchy?.any { it.route == item.route } == true
                    
                    NavigationBarItem(
                        icon = {
                            Icon(
                                imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                                contentDescription = item.title
                            )
                        },
                        label = { Text(item.title) },
                        selected = selected,
                        onClick = {
                            navController.navigate(item.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.ChatList.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            // Chat screens
            composable(Screen.ChatList.route) {
                ChatListScreen(
                    onConversationClick = { conversationId ->
                        navController.navigate(Screen.Chat.createRoute(conversationId))
                    },
                    onNewChat = { conversationId ->
                        navController.navigate(Screen.Chat.createRoute(conversationId))
                    }
                )
            }
            
            composable(
                route = Screen.Chat.route,
                arguments = listOf(navArgument("conversationId") { type = NavType.StringType })
            ) { backStackEntry ->
                val conversationId = backStackEntry.arguments?.getString("conversationId") ?: return@composable
                ChatScreen(
                    conversationId = conversationId,
                    onBack = { navController.popBackStack() }
                )
            }
            
            // Agent screens
            composable(Screen.AgentList.route) {
                AgentListScreen(
                    onAgentClick = { agentId ->
                        navController.navigate(Screen.AgentWorkspace.createRoute(agentId))
                    }
                )
            }
            
            composable(
                route = Screen.AgentWorkspace.route,
                arguments = listOf(navArgument("agentId") { type = NavType.StringType })
            ) { backStackEntry ->
                val agentId = backStackEntry.arguments?.getString("agentId") ?: return@composable
                AgentWorkspaceScreen(
                    agentId = agentId,
                    onBack = { navController.popBackStack() }
                )
            }
            
            // Knowledge screen
            composable(Screen.Knowledge.route) {
                KnowledgeScreen()
            }
            
            // Settings screens
            composable(Screen.Settings.route) {
                SettingsScreen(
                    onNavigateToProviders = {
                        navController.navigate(Screen.ProviderSettings.route)
                    }
                )
            }
            
            composable(Screen.ProviderSettings.route) {
                ProviderSettingsScreen(
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}

/**
 * Extension for NavigationBar icon
 */
private typealias Icons = androidx.compose.material.icons.Icons
private typealias ImageIcon = androidx.compose.ui.graphics.vector.ImageVector
