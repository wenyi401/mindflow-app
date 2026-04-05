package com.mindflow.ui.screens.knowledge

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
import com.mindflow.domain.model.KnowledgeDocument
import com.mindflow.domain.model.SourceType
import org.koin.androidx.compose.koinViewModel
import java.text.SimpleDateFormat
import java.util.*

/**
 * Knowledge base screen
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KnowledgeScreen(
    viewModel: KnowledgeViewModel = koinViewModel()
) {
    val documents by viewModel.documents.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Knowledge Base") },
                actions = {
                    IconButton(onClick = { showAddDialog = true }) {
                        Icon(Icons.Default.Add, contentDescription = "Add document")
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Search bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                placeholder = { Text("Search knowledge base...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true
            )
            
            if (documents.isEmpty()) {
                EmptyKnowledgeState(
                    modifier = Modifier.fillMaxSize(),
                    onAddDocument = { showAddDialog = true }
                )
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(documents, key = { it.id }) { document ->
                        KnowledgeDocumentItem(
                            document = document,
                            onDelete = { viewModel.deleteDocument(document.id) }
                        )
                    }
                }
            }
        }
    }
    
    if (showAddDialog) {
        AddKnowledgeDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { title, source, sourceType, content ->
                viewModel.addDocument(title, source, sourceType, content)
                showAddDialog = false
            }
        )
    }
}

@Composable
private fun EmptyKnowledgeState(
    modifier: Modifier = Modifier,
    onAddDocument: () -> Unit
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.LibraryBooks,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Knowledge base empty",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Add documents, notes, or import web content",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(onClick = onAddDocument) {
            Icon(Icons.Default.Add, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Add Document")
        }
    }
}

@Composable
private fun KnowledgeDocumentItem(
    document: KnowledgeDocument,
    onDelete: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        ListItem(
            headlineContent = {
                Text(
                    text = document.title,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            },
            supportingContent = {
                Column {
                    Text(
                        text = document.source,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        text = "${document.sourceType.name} • ${formatDate(document.createdAt)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            leadingContent = {
                Icon(
                    imageVector = when (document.sourceType) {
                        SourceType.FILE -> Icons.Default.InsertDriveFile
                        SourceType.WEB_URL -> Icons.Default.Language
                        SourceType.NOTE -> Icons.Default.Note
                        SourceType.IMPORT -> Icons.Default.FileOpen
                    },
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            },
            trailingContent = {
                Box {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "More")
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
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddKnowledgeDialog(
    onDismiss: () -> Unit,
    onConfirm: (title: String, source: String, sourceType: SourceType, content: String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var source by remember { mutableStateOf("") }
    var content by remember { mutableStateOf("") }
    var sourceType by remember { mutableStateOf(SourceType.NOTE) }
    var expanded by remember { mutableStateOf(false) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add to Knowledge Base") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                
                OutlinedTextField(
                    value = source,
                    onValueChange = { source = it },
                    label = { Text("Source (URL or file path)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = it }
                ) {
                    OutlinedTextField(
                        value = sourceType.name,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Type") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        SourceType.entries.forEach { type ->
                            DropdownMenuItem(
                                text = { Text(type.name) },
                                onClick = {
                                    sourceType = type
                                    expanded = false
                                }
                            )
                        }
                    }
                }
                
                OutlinedTextField(
                    value = content,
                    onValueChange = { content = it },
                    label = { Text("Content") },
                    maxLines = 6,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(title, source, sourceType, content) },
                enabled = title.isNotBlank() && content.isNotBlank()
            ) {
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

private fun formatDate(timestamp: Long): String {
    return SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(Date(timestamp))
}

/**
 * Knowledge ViewModel
 */
class KnowledgeViewModel(
    private val knowledgeRepository: com.mindflow.domain.repository.KnowledgeRepository
) : androidx.lifecycle.ViewModel() {
    
    val documents = knowledgeRepository.getAllDocuments()
        .collectAsState(initial = emptyList())
    
    fun addDocument(
        title: String,
        source: String,
        sourceType: SourceType,
        content: String
    ) {
        val document = KnowledgeDocument(
            id = UUID.randomUUID().toString(),
            title = title,
            source = source,
            sourceType = sourceType,
            content = content,
            createdAt = System.currentTimeMillis()
        )
        
        kotlinx.coroutines.GlobalScope.launch {
            knowledgeRepository.addDocument(document)
        }
    }
    
    fun deleteDocument(id: String) {
        kotlinx.coroutines.GlobalScope.launch {
            knowledgeRepository.deleteDocument(id)
        }
    }
}

private fun kotlinx.coroutines.GlobalScope.launch(block: suspend () -> Unit) {
    kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch { block() }
}
