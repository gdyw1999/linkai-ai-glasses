package com.glasses.app.ui.navigation

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.glasses.app.ui.chat.ChatScreen
import com.glasses.app.ui.gallery.GalleryScreen
import com.glasses.app.ui.home.DeviceScanScreen
import com.glasses.app.ui.home.HomeScreen
import com.glasses.app.ui.profile.ProfileScreen

/**
 * 导航路由定义
 */
object NavRoutes {
    const val HOME = "home"
    const val CHAT = "chat"
    const val GALLERY = "gallery"
    const val PROFILE = "profile"
    const val DEVICE_SCAN = "device_scan"
}

/**
 * 导航图
 */
@Composable
fun NavGraph(navController: NavHostController, innerPadding: PaddingValues = PaddingValues()) {
    NavHost(
        navController = navController,
        startDestination = NavRoutes.HOME
    ) {
        composable(NavRoutes.HOME) {
            HomeScreen(
                innerPadding = innerPadding,
                onNavigateToDeviceScan = {
                    navController.navigate(NavRoutes.DEVICE_SCAN)
                }
            )
        }
        composable(NavRoutes.DEVICE_SCAN) {
            DeviceScanScreen(
                onBack = {
                    navController.popBackStack()
                }
            )
        }
        composable(NavRoutes.CHAT) {
            ChatScreen(innerPadding = innerPadding)
        }
        composable(NavRoutes.GALLERY) {
            GalleryScreen(innerPadding = innerPadding)
        }
        composable(NavRoutes.PROFILE) {
            ProfileScreen(innerPadding = innerPadding)
        }
    }
}
