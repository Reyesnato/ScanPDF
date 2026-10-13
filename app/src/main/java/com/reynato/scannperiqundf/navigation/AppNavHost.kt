package com.reynato.scannperiqundf.navigation

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument

@Composable
fun AppNavHost() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = Routes.HOME) {

        composable(Routes.HOME) {
            Text("Pantalla de inicio")
        }

        composable(
            route = Routes.EDITOR,
            arguments = listOf(navArgument("docId") { type = NavType.StringType })
        ) { entry ->
            val docId = entry.arguments?.getString("docId") ?: return@composable
            Text("Editor de $docId")
        }

        composable(
            route = Routes.CAMERA,
            arguments = listOf(navArgument("docId") { type = NavType.StringType })
        ) { entry ->
            val docId = entry.arguments?.getString("docId") ?: return@composable
            Text("Cámara de $docId")
        }
    }
}