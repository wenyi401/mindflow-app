package com.mindflow.ui.screens.settings

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
import androidx.lifecycle.viewModelScope
import com.mindflow.domain.model.AIProvider
import com.mindflow.domain.model.ProviderType
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProviderSettingsScreen(
    onBack: () -> Unit,
    viewModel: ProviderSettingsViewModel = koinViewModel()
) {
    val providers by viewModel.providers.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }
    var providerToDelete by remember { mutableStateOf<AIProvider?>(null) }
    var editingProvider by remember { mutableStateOf<AIProvider?>(null) }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("AI Providers") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showAddDialog = true }) {
                        Icon(Icons.Default.Add, contentDescription = "Add Provider")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Add")
            }
        }
    ) { padding ->
        if (providers.isEmpty()) {
            EmptyProvidersState(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                onAddProvider = { showAddDialog = true }
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                items(providers, key = { it.id }) { provider ->
                    ProviderItem(
                        provider = provider,
                        onToggle = { viewModel.toggleProvider(provider) },
                        onEdit = { editingProvider = provider },
                        onDelete = { providerToDelete = provider }
                    )
                }
            }
        }
    }
    
    if (showAddDialog) {
        ProviderConfigDialog(
            existingProvider = null,
            onDismiss = { showAddDialog = false },
            onConfirm = { provider ->
                viewModel.saveProvider(provider)
                showAddDialog = false
            }
        )
    }
    
    editingProvider?.let { provider ->
        ProviderConfigDialog(
            existingProvider = provider,
            onDismiss = { editingProvider = null },
            onConfirm = { updated ->
                viewModel.saveProvider(updated)
                editingProvider = null
            }
        )
    }
    
    providerToDelete?.let { provider ->
        AlertDialog(
            onDismissRequest = { providerToDelete = null },
            title = { Text("Delete Provider?") },
            text = { Text("This will remove ${provider.name} configuration.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteProvider(provider.id)
                        providerToDelete = null
                    }
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { providerToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun EmptyProvidersState(
    modifier: Modifier = Modifier,
    onAddProvider: () -> Unit
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Settings,
            contentDescription = null,
            modifier = Modifier.size(72.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "No AI Providers",
            style = MaterialTheme.typography.titleLarge
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Add your first AI provider to get started",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(onClick = onAddProvider) {
            Icon(Icons.Default.Add, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Add Provider")
        }
    }
}

@Composable
private fun ProviderItem(
    provider: AIProvider,
    onToggle: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = when (provider.type) {
                    ProviderType.OPENAI_COMPATIBLE -> Icons.Default.AutoAwesome
                    ProviderType.ANTHROPIC -> Icons.Default.Psychology
                    ProviderType.GOOGLE_AI -> Icons.Default.Cloud
                    ProviderType.AZURE_OPENAI -> Icons.Default.CloudQueue
                    ProviderType.CUSTOM -> Icons.Default.Settings
                },
                contentDescription = null,
                modifier = Modifier.size(40.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = provider.name,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = provider.modelId,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            
            Switch(
                checked = provider.isEnabled,
                onCheckedChange = { onToggle() }
            )
            
            Box {
                IconButton(onClick = { showMenu = true }) {
                    Icon(Icons.Default.MoreVert, contentDescription = "More options")
                }
                
                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Edit") },
                        onClick = {
                            showMenu = false
                            onEdit()
                        },
                        leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) }
                    )
                    DropdownMenuItem(
                        text = { Text("Delete") },
                        onClick = {
                            showMenu = false
                            onDelete()
                        },
                        leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null) }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProviderConfigDialog(
    existingProvider: AIProvider? = null,
    onDismiss: () -> Unit,
    onConfirm: (AIProvider) -> Unit
) {
    var name by remember { mutableStateOf(existingProvider?.name ?: "") }
    var type by remember { mutableStateOf(existingProvider?.type ?: ProviderType.OPENAI_COMPATIBLE) }
    var baseUrl by remember { mutableStateOf(existingProvider?.baseUrl ?: "https://api.openai.com/v1") }
    var apiKey by remember { mutableStateOf(existingProvider?.apiKey ?: "") }
    var modelId by remember { mutableStateOf(existingProvider?.modelId ?: "gpt-3.5-turbo") }
    var maxTokens by remember { mutableStateOf(existingProvider?.maxTokens?.toString() ?: "4096") }
    var temperature by remember { mutableStateOf(existingProvider?.temperature?.toString() ?: "0.7") }
    var typeExpanded by remember { mutableStateOf(false) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (existingProvider != null) "Edit Provider" else "Add Provider") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                
                ExposedDropdownMenuBox(
                    expanded = typeExpanded,
                    onExpandedChange = { typeExpanded = it }
                ) {
                    OutlinedTextField(
                        value = type.name.replace("_", " "),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Type") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = typeExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = typeExpanded,
                        onDismissRequest = { typeExpanded = false }
                    ) {
                        ProviderType.entries.forEach { pType ->
                            DropdownMenuItem(
                                text = { Text(pType.name.replace("_", " ")) },
                                onClick = {
                                    type = pType
                                    typeExpanded = false
                                    if (pType == ProviderType.ANTHROPIC) {
                                        baseUrl = "https://api.anthropic.com"
                                        modelId = "claude-3-sonnet-20240229"
                                    } else if (pType == ProviderType.GOOGLE_AI) {
                                        baseUrl = "https://generativelanguage.googleapis.com"
                                        modelId = "gemini-pro"
                                    }
                                }
                            )
                        }
                    }
                }
                
                OutlinedTextField(
                    value = baseUrl,
                    onValueChange = { baseUrl = it },
                    label = { Text("Base URL") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                
                OutlinedTextField(
                    value = apiKey,
                    onValueChange = { apiKey = it },
                    label = { Text("API Key") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                
                OutlinedTextField(
                    value = modelId,
                    onValueChange = { modelId = it },
                    label = { Text("Model ID") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = maxTokens,
                        onValueChange = { maxTokens = it },
                        label = { Text("Max Tokens") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    
                    OutlinedTextField(
                        value = temperature,
                        onValueChange = { temperature = it },
                        label = { Text("Temperature") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (name.isNotBlank() && apiKey.isNotBlank()) {
                        onConfirm(
                            AIProvider(
                                id = existingProvider?.id ?: java.util.UUID.randomUUID().toString(),
                                name = name,
                                type = type,
                                baseUrl = baseUrl,
                                apiKey = apiKey,
                                modelId = modelId,
                                maxTokens = maxTokens.toIntOrNull() ?: 4096,
                                temperature = temperature.toFloatOrNull() ?: 0.7f,
                                isEnabled = existingProvider?.isEnabled ?: true
                            )
                        )
                    }
                }
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

class ProviderSettingsViewModel(
    private val providerRepository: com.mindflow.domain.repository.ProviderRepository
) : androidx.lifecycle.ViewModel() {
    
    val providers: StateFlow<List<AIProvider>> = providerRepository.getAllProviders()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
    
    fun saveProvider(provider: AIProvider) {
        viewModelScope.launch {
            providerRepository.saveProvider(provider)
        }
    }
    
    fun deleteProvider(id: String) {
        viewModelScope.launch {
            providerRepository.deleteProvider(id)
        }
    }
    
    fun toggleProvider(provider: AIProvider) {
        viewModelScope.launch {
            providerRepository.saveProvider(provider.copy(isEnabled = !provider.isEnabled))
        }
    }
}
