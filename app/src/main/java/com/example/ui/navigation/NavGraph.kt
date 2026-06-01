package com.example.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.ui.screens.editor.EditorScreen
import com.example.ui.screens.home.HomeScreen

@Composable
fun ForgeNavGraph() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = "home") {
        composable("home") {
            HomeScreen(
                onNavigateToEditor = { projectId ->
                    navController.navigate("editor/\$projectId")
                }
            )
        }
        composable("editor/{projectId}") { backStackEntry ->
            val projectId = backStackEntry.arguments?.getString("projectId") ?: return@composable
            EditorScreen(
                projectId = projectId,
                onBack = { navController.popBackStack() }
            )
        }
    }
}
