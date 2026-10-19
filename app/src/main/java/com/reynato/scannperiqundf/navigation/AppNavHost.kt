package com.reynato.scannperiqundf.navigation

import androidx.compose.material3.Text
import com.reynato.scannperiqundf.camera.CropScreen
import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.reynato.scannperiqundf.ui.editor.EditorScreen
import androidx.navigation.navArgument
import com.reynato.scannperiqundf.ui.home.HomeScreen
import com.reynato.scannperiqundf.camera.CameraScreen

@Composable
fun AppNavHost() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = Routes.HOME) {

        composable(Routes.HOME) {
            HomeScreen(
                onOpenDocument = { id -> navController.navigate(Routes.editor(id)) },
                onNewScan = { id -> navController.navigate(Routes.camera(id)) }
            )
        }

        composable(
            route = Routes.EDITOR,
            arguments = listOf(navArgument("docId") { type = NavType.StringType })
        ) { entry ->
            val docId = entry.arguments?.getString("docId") ?: return@composable
            EditorScreen(
                docId = docId,
                onTakePhotos = { navController.navigate(Routes.camera(docId)) },
                onEditPage = { pageId -> navController.navigate(Routes.editPage(docId, pageId)) },
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Routes.CAMERA,
            arguments = listOf(navArgument("docId") { type = NavType.StringType })
        ) { entry ->
            val docId = entry.arguments?.getString("docId") ?: return@composable
            CameraScreen(
                onPhotoTaken = { photoName ->
                    navController.navigate(Routes.crop(docId, photoName))
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Routes.CROP,
            arguments = listOf(
                navArgument("docId") { type = NavType.StringType },
                navArgument("photoName") { type = NavType.StringType }
            )
        ) { entry ->
            val docId = entry.arguments?.getString("docId") ?: return@composable
            val photoName = entry.arguments?.getString("photoName") ?: return@composable
            CropScreen(
                docId = docId,
                photoName = photoName,
                pageId = null,
                onAccepted = {
                    val volvio = navController.popBackStack(Routes.EDITOR, false)
                    if (!volvio) {
                        navController.navigate(Routes.editor(docId)) {
                            popUpTo(Routes.HOME)
                        }
                    }
                },
                onCancel = { navController.popBackStack() }
            )
        }

        composable(
            route = Routes.EDIT_PAGE,
            arguments = listOf(
                navArgument("docId") { type = NavType.StringType },
                navArgument("pageId") { type = NavType.StringType }
            )
        ) { entry ->
            val docId = entry.arguments?.getString("docId") ?: return@composable
            val pageId = entry.arguments?.getString("pageId") ?: return@composable
            CropScreen(
                docId = docId,
                photoName = null,
                pageId = pageId,
                onAccepted = { navController.popBackStack() },
                onCancel = { navController.popBackStack() }
            )
        }
    }
}