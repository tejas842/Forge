package com.example.ui.screens.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.example.data.remote.GeminiService
import com.example.domain.FileManager
import kotlinx.coroutines.launch
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(
    projectId: String,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val fileManager = remember { FileManager(context) }
    val geminiService = remember { GeminiService(context) }

    val projectDir = remember(projectId) { fileManager.createProjectDir(projectId) }
    var files by remember { mutableStateOf<List<File>>(emptyList()) }
    
    LaunchedEffect(projectDir) {
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            val list = fileManager.listFiles(projectDir)
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                files = list
            }
        }
    }
    
    var selectedFile by remember { mutableStateOf<File?>(null) }
    var fileContent by remember { mutableStateOf("") }
    
    // Auto-save logic
    LaunchedEffect(fileContent) {
        selectedFile?.let { file ->
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                fileManager.writeFile(projectDir, file.name, fileContent)
            }
        }
    }

    var showChat by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(selectedFile?.name ?: "Forge Editor") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Back") }
                },
                actions = {
                    TextButton(onClick = { showChat = !showChat }) {
                        Text(if (showChat) "Code" else "AI Chat", color = MaterialTheme.colorScheme.primary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background.copy(alpha = 0.95f)
                )
            )
        }
    ) { padding ->
        Row(modifier = Modifier.fillMaxSize().padding(padding)) {
            // File Explorer (Left Panel)
            Surface(
                modifier = Modifier.width(100.dp).fillMaxHeight(),
                color = MaterialTheme.colorScheme.surfaceVariant
            ) {
                LazyColumn(contentPadding = PaddingValues(8.dp)) {
                    item { Text("FILES", style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(bottom = 8.dp)) }
                    items(files) { file ->
                        FileItem(
                            file = file,
                            isSelected = file == selectedFile,
                            onClick = {
                                selectedFile = file
                                coroutineScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                                    val content = fileManager.readFile(file)
                                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                                        fileContent = content
                                    }
                                }
                            }
                        )
                    }
                }
            }
            
            Divider(modifier = Modifier.fillMaxHeight().width(1.dp))

            // Main Area
            if (showChat) {
                // AI Chat Panel
                ChatPanel(
                    geminiService = geminiService,
                    files = files,
                    fileManager = fileManager,
                    modifier = Modifier.weight(1f).fillMaxHeight()
                )
            } else {
                // Code Editor
                if (selectedFile != null) {
                    TextField(
                        value = fileContent,
                        onValueChange = { fileContent = it },
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                        textStyle = MaterialTheme.typography.bodyMedium.copy(
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.onSurface
                        ),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Black,
                            unfocusedContainerColor = Color.Black,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            cursorColor = MaterialTheme.colorScheme.primary
                        )
                    )
                } else {
                    Box(modifier = Modifier.weight(1f).fillMaxHeight(), contentAlignment = Alignment.Center) {
                        Text("Select a file to edit", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@Composable
fun FileItem(file: File, isSelected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp, horizontal = 4.dp)
            .background(
                if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else Color.Transparent,
                shape = MaterialTheme.shapes.small
            )
            .padding(vertical = 6.dp, horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = if (file.isDirectory) Icons.Default.Folder else Icons.Default.InsertDriveFile,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = if (file.isDirectory) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(file.name, style = MaterialTheme.typography.bodySmall, maxLines = 1)
    }
}

@Composable
fun ChatPanel(geminiService: GeminiService, files: List<File>, fileManager: FileManager, modifier: Modifier = Modifier) {
    var prompt by remember { mutableStateOf("") }
    var history by remember { mutableStateOf(listOf<Pair<String, Boolean>>()) } // Pair of text and isUser
    var isTyping by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    Column(modifier = modifier) {
        LazyColumn(
            modifier = Modifier.weight(1f).padding(16.dp),
            reverseLayout = true
        ) {
            items(history.reversed()) { (message, isUser) ->
                ChatBubble(message = message, isUser = isUser)
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
        
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = prompt,
                onValueChange = { prompt = it },
                placeholder = { Text("Ask Forge AI...") },
                modifier = Modifier.weight(1f),
                singleLine = true
            )
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(
                onClick = {
                    if (prompt.isNotBlank() && !isTyping) {
                        val currentPrompt = prompt
                        history = history + (currentPrompt to true)
                        prompt = ""
                        isTyping = true
                        
                        coroutineScope.launch {
                            val contextFiles = files.filter { !it.isDirectory }.map { "\${it.name}:\n\${fileManager.readFile(it)}" }
                            val response = geminiService.generateContent(currentPrompt, contextFiles)
                            history = history + (response to false)
                            isTyping = false
                        }
                    }
                },
                enabled = prompt.isNotBlank() && !isTyping
            ) {
                Icon(Icons.Default.Send, contentDescription = "Send", tint = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

@Composable
fun ChatBubble(message: String, isUser: Boolean) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = if (isUser) 32.dp else 0.dp),
        contentAlignment = if (isUser) Alignment.CenterEnd else Alignment.CenterStart
    ) {
        Surface(
            color = if (isUser) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
            shape = MaterialTheme.shapes.medium
        ) {
            Text(
                text = message,
                modifier = Modifier.padding(12.dp),
                color = if (isUser) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
