package com.example.ui.screens.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.local.AppDatabase
import com.example.data.local.Project
import com.example.domain.FileManager
import kotlinx.coroutines.launch
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToEditor: (String) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val db = remember { AppDatabase.getDatabase(context) }
    val projectDao = db.projectDao()
    val fileManager = remember { FileManager(context) }

    val projects by projectDao.getAllProjects().collectAsState(initial = emptyList())
    var showCreateDialog by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Forge", fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onBackground) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background.copy(alpha = 0.95f)
                ),
                actions = {
                    IconButton(onClick = { showSettingsDialog = true }) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                    Button(
                        onClick = { showCreateDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        shape = MaterialTheme.shapes.small,
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier.padding(end = 8.dp).height(32.dp)
                    ) {
                        Text("New Project", style = MaterialTheme.typography.labelMedium)
                    }
                }
            )
        },
        floatingActionButton = {
            // Replaced by TopAppBar button
        }
    ) { padding ->
        if (projects.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("No projects yet. Create one to get started.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(projects) { project ->
                    ProjectCard(project, onClick = { onNavigateToEditor(project.id) })
                }
            }
        }

        if (showCreateDialog) {
            CreateProjectDialog(
                onDismiss = { showCreateDialog = false },
                onCreate = { name, type ->
                    coroutineScope.launch {
                        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                            val id = UUID.randomUUID().toString()
                            val dir = fileManager.createProjectDir(id)
                            
                            // Seed basic files depending on type
                            if (type == "React") {
                                fileManager.writeFile(dir, "package.json", "{\n  \"name\": \"\$name\",\n  \"version\": \"1.0.0\"\n}")
                                fileManager.writeFile(dir, "src/App.jsx", "export default function App() {\n  return <div>Hello React</div>;\n}")
                            } else {
                                fileManager.writeFile(dir, "index.html", "<!DOCTYPE html>\n<html>\n<body>\n  <h1>Hello Forge</h1>\n</body>\n</html>")
                                fileManager.writeFile(dir, "style.css", "body { margin: 0; }")
                                fileManager.writeFile(dir, "script.js", "console.log('App started');")
                            }

                            val newProject = Project(id = id, name = name, type = type, path = dir.absolutePath)
                            projectDao.insertProject(newProject)
                            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                                showCreateDialog = false
                                onNavigateToEditor(id)
                            }
                        }
                    }
                }
            )
        }

        if (showSettingsDialog) {
            SettingsDialog(
                onDismiss = { showSettingsDialog = false }
            )
        }
    }
}

@Composable
fun ProjectCard(project: Project, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        shape = MaterialTheme.shapes.medium,
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Code, contentDescription = null, modifier = Modifier.size(40.dp), tint = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(project.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(project.type, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
fun CreateProjectDialog(onDismiss: () -> Unit, onCreate: (String, String) -> Unit) {
    var name by remember { mutableStateOf("") }
    var type by remember { mutableStateOf("HTML/CSS/JS") }
    val types = listOf("HTML/CSS/JS", "React", "Node.js", "Custom")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New Project") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Project Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Text("Template", modifier = Modifier.padding(top = 8.dp), style = MaterialTheme.typography.labelMedium)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    types.forEach { t ->
                        FilterChip(
                            selected = type == t,
                            onClick = { type = t },
                            label = { Text(t) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { if (name.isNotBlank()) onCreate(name, type) },
                enabled = name.isNotBlank()
            ) {
                Text("Create")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun SettingsDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val sharedPrefs = remember { context.getSharedPreferences("forge_settings", android.content.Context.MODE_PRIVATE) }
    var apiKey by remember { mutableStateOf(sharedPrefs.getString("api_key", "") ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Settings") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("AI Provider Settings", style = MaterialTheme.typography.titleMedium)
                OutlinedTextField(
                    value = apiKey,
                    onValueChange = { apiKey = it },
                    label = { Text("Gemini API Key") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    "Your key is stored locally on this device.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    sharedPrefs.edit().putString("api_key", apiKey.trim()).apply()
                    onDismiss()
                }
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
