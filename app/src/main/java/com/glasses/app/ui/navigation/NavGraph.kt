package com.glasses.app.ui.navigation

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.glasses.app.ui.chat.ChatScreen
import com.glasses.app.ui.conversation.ConversationListScreen
import com.glasses.app.ui.content.ContentRenderScreen
import com.glasses.app.ui.gallery.GalleryScreen
import com.glasses.app.ui.home.DeviceScanScreen
import com.glasses.app.ui.home.HomeScreen
import com.glasses.app.ui.profile.ProfileScreen
import com.glasses.app.viewmodel.SharedRenderViewModel

/**
 * 导航路由定义
 */
object NavRoutes {
    const val CONVERSATION_LIST = "conversation_list"
    const val HOME = "home"
    const val CHAT = "chat/{conversationId}"
    const val GALLERY = "gallery"
    const val PROFILE = "profile"
    const val DEVICE_SCAN = "device_scan"
    const val CONTENT_RENDER = "content_render"  // 内容渲染页面

    /** 构建 Chat 路由，传入会话 ID */
    fun chatRoute(conversationId: Long): String = "chat/$conversationId"
}

/**
 * 导航图
 */
@Composable
fun NavGraph(navController: NavHostController, innerPadding: PaddingValues = PaddingValues()) {
    NavHost(
        navController = navController,
        startDestination = NavRoutes.CONVERSATION_LIST
    ) {
        // 对话列表（首页 Tab）
        composable(NavRoutes.CONVERSATION_LIST) {
            ConversationListScreen(
                innerPadding = innerPadding,
                onConversationClick = { conversationId ->
                    navController.navigate(NavRoutes.chatRoute(conversationId))
                },
                onNewConversation = { conversationId ->
                    navController.navigate(NavRoutes.chatRoute(conversationId))
                }
            )
        }

        // AR眼镜（原首页）
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

        // AI对话（全屏，从对话列表进入）
        composable(
            route = NavRoutes.CHAT,
            arguments = listOf(
                navArgument("conversationId") { type = NavType.LongType }
            )
        ) { backStackEntry ->
            val conversationId = backStackEntry.arguments?.getLong("conversationId") ?: 0L
            // 获取共享 ViewModel（在多个页面间共享）
            val sharedViewModel: SharedRenderViewModel = viewModel()

            ChatScreen(
                innerPadding = PaddingValues(),
                onBack = { navController.popBackStack() },
                conversationId = conversationId,
                onNavigateToRender = {
                    // 跳转到内容渲染页面
                    navController.navigate(NavRoutes.CONTENT_RENDER)
                },
                sharedViewModel = sharedViewModel
            )
        }

        // 内容渲染页面（HTML/Markdown）
        composable(NavRoutes.CONTENT_RENDER) {
            val sharedViewModel: SharedRenderViewModel = viewModel()

            ContentRenderScreen(
                sharedViewModel = sharedViewModel,
                onBack = { navController.popBackStack() }
            )
        }

        // AR相册
        composable(NavRoutes.GALLERY) {
            GalleryScreen(innerPadding = innerPadding)
        }

        // 我的
        composable(NavRoutes.PROFILE) {
            ProfileScreen(innerPadding = innerPadding)
        }
    }
}
