package com.mindflow.ui.screens.agent

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.mindflow.domain.model.Agent
import com.mindflow.domain.model.MemoryStrategy
import org.koin.androidx.compose.koinViewModel

/**
 * Agent list screen
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AgentListScreen(
    viewModel: AgentListViewModel = koinViewModel(),
    onAgentClick: (String) -> Unit
) {
    val agents by viewModel.agents.collectAsState()
    var showCreateDialog by remember { mutableStateOf(false) }
    var agentToDelete by remember { mutableStateOf<Agent?>(null) }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("AI Agents") },
                actions = {
                    IconButton(onClick = { showCreateDialog = true }) {
                        Icon(Icons.Default.Add, contentDescription = "Create agent")
                    }
                }
            )
        }
    ) { padding ->
        if (agents.isEmpty()) {
            EmptyAgentState(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                onCreateAgent = { showCreateDialog = true }
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                items(agents, key = { it.id }) { agent ->
                    AgentItem(
                        agent = agent,
                        onClick = { onAgentClick(agent.id) },
                        onDelete = { agentToDelete = agent }
                    )
                }
            }
        }
    }
    
    // Create agent dialog
    if (showCreateDialog) {
        CreateAgentDialog(
            onDismiss = { showCreateDialog = false },
            onConfirm = { name, description, prompt, strategy ->
                viewModel.createAgent(name, description, prompt, emptyList(), strategy)
                showCreateDialog = false
            }
        )
    }
    
    // Delete confirmation
    agentToDelete?.let { agent ->
        AlertDialog(
            onDismissRequest = { agentToDelete = null },
            title = { Text("Delete agent?") },
            text = { Text("This will permanently delete ${agent.name}.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteAgent(agent.id)
                        agentToDelete = null
                    }
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { agentToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun EmptyAgentState(
    modifier: Modifier = Modifier,
    onCreateAgent: () -> Unit
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.SmartToy,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "No agents yet",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Create an AI agent to automate tasks",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(onClick = onCreateAgent) {
            Icon(Icons.Default.Add, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Create Agent")
        }
    }
}

@Composable
private fun AgentItem(
    agent: Agent,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    
    ListItem(
        headlineContent = {
            Text(
                text = agent.name,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        supportingContent = {
            Text(
                text = agent.description,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodySmall
            )
        },
        leadingContent = {
            Icon(
                imageVector = Icons.Default.SmartToy,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        },
        trailingContent = {
            Box {
                IconButton(onClick = { showMenu = true }) {
                    Icon(Icons.Default.MoreVert, contentDescription = "More options")
                }
                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Delete") },
                        onClick = {
                            showMenu = false
                            onDelete()
                        },
                        leadingIcon = {
                            Icon(Icons.Default.Delete, contentDescription = null)
                        }
                    )
                }
            }
        },
        modifier = Modifier.clickable(onClick = onClick)
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CreateAgentDialog(
    onDismiss: () -> Unit,
    onConfirm: (name: String, description: String, prompt: String, strategy: MemoryStrategy) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var prompt by remember { mutableStateOf("You are a helpful AI assistant.") }
    var strategy by remember { mutableStateOf(MemoryStrategy.SHORT_TERM) }
    var expanded by remember { mutableStateOf(false) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create Agent") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description") },
                    maxLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )
                
                OutlinedTextField(
                    value = prompt,
                    onValueChange = { prompt = it },
                    label = { Text("System Prompt") },
                    maxLines = 4,
                    modifier = Modifier.fillMaxWidth()
                )
                
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = it }
                ) {
                    OutlinedTextField(
                        value = strategy.name,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Memory Strategy") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        MemoryStrategy.entries.forEach { s ->
                            DropdownMenuItem(
                                text = { Text(s.name) },
                                onClick = {
                                    strategy = s
                                    expanded = false
                                }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(name, description, prompt, strategy) },
                enabled = name.isNotBlank()
            ) {
                Text("Create")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
