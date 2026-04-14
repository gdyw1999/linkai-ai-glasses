package com.glasses.app

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.compose.rememberNavController
import com.glasses.app.ui.navigation.NavGraph
import com.glasses.app.ui.navigation.NavRoutes
import com.glasses.app.ui.permission.PermissionDeniedDialog
import com.glasses.app.ui.permission.PermissionScreen
import com.glasses.app.ui.theme.GlassesAppTheme
import com.glasses.app.util.PermissionHelper

/**
 * 主Activity
 * 使用Jetpack Compose构建UI
 */
class MainActivity : ComponentActivity() {
    
    private lateinit var permissionHelper: PermissionHelper
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // 初始化权限管理器
        permissionHelper = PermissionHelper(this)
        
        // 延迟初始化SDK，避免阻塞UI
        (application as? GlassesApplication)?.initializeSDK()
        
        setContent {
            GlassesAppTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppContent(permissionHelper)
                }
            }
        }
    }
}

/**
 * 应用内容 - 处理权限检查和主界面显示
 */
@Composable
private fun AppContent(permissionHelper: PermissionHelper) {
    var hasPermissions by remember { 
        mutableStateOf(PermissionHelper.hasAllRequiredPermissions(permissionHelper.activity))
    }
    var showDeniedDialog by remember { mutableStateOf(false) }
    var deniedPermissions by remember { mutableStateOf<List<String>>(emptyList()) }
    
    if (hasPermissions) {
        // 有权限，显示主界面
        MainScreen()
    } else {
        // 没有权限，显示权限请求界面
        PermissionScreen(
            onRequestPermissions = {
                permissionHelper.requestAllPermissions(
                    onGranted = {
                        hasPermissions = true
                    },
                    onDenied = { denied ->
                        deniedPermissions = denied
                        showDeniedDialog = true
                    }
                )
            }
        )
        
        // 权限被拒绝对话框
        if (showDeniedDialog) {
            PermissionDeniedDialog(
                deniedPermissions = deniedPermissions,
                onDismiss = {
                    showDeniedDialog = false
                    // 即使被拒绝也进入主界面，但功能会受限
                    hasPermissions = true
                },
                onOpenSettings = {
                    // 打开应用设置页面
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.fromParts("package", permissionHelper.activity.packageName, null)
                    }
                    permissionHelper.activity.startActivity(intent)
                    showDeniedDialog = false
                }
            )
        }
    }
}

/**
 * 主屏幕 - 包含导航栏和页面内容
 */
@Composable
fun MainScreen() {
    val navController = rememberNavController()
    var currentRoute by remember { mutableStateOf(NavRoutes.HOME) }

    // 监听导航变化
    LaunchedEffect(navController) {
        navController.currentBackStackEntryFlow.collect { backStackEntry ->
            currentRoute = backStackEntry.destination.route ?: NavRoutes.HOME
        }
    }

    // Chat 页面全屏：隐藏底部导航栏
    val showBottomBar = currentRoute != NavRoutes.CHAT

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            AnimatedVisibility(
                visible = showBottomBar,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                BottomNavigationBar(
                    currentRoute = currentRoute,
                    onNavigate = { route ->
                        navController.navigate(route) {
                            popUpTo(navController.graph.startDestinationId) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
        }
    ) { innerPadding ->
        // Chat 页面传零 padding 实现全屏，其他页面正常传 innerPadding
        val chatPadding = if (currentRoute == NavRoutes.CHAT) PaddingValues(0.dp) else innerPadding
        NavGraph(navController, chatPadding)
    }
}

/**
 * 底部导航栏
 */
@Composable
fun BottomNavigationBar(
    currentRoute: String,
    onNavigate: (String) -> Unit
) {
    NavigationBar(
        modifier = Modifier,
        containerColor = Color.White,
        contentColor = Color(0xFF2196F3)
    ) {
        NavigationBarItem(
            icon = {
                Icon(
                    imageVector = Icons.Default.Home,
                    contentDescription = "首页",
                    modifier = Modifier
                )
            },
            label = {
                Text(
                    text = "首页",
                    fontSize = 11.sp
                )
            },
            selected = currentRoute == NavRoutes.HOME,
            onClick = { onNavigate(NavRoutes.HOME) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = Color(0xFF2196F3),
                selectedTextColor = Color(0xFF2196F3),
                unselectedIconColor = Color(0xFFCCCCCC),
                unselectedTextColor = Color(0xFFCCCCCC),
                indicatorColor = Color(0xFFE3F2FD)
            )
        )
        
        NavigationBarItem(
            icon = {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "AI对话",
                    modifier = Modifier
                )
            },
            label = {
                Text(
                    text = "AI对话",
                    fontSize = 11.sp
                )
            },
            selected = currentRoute == NavRoutes.CHAT,
            onClick = { onNavigate(NavRoutes.CHAT) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = Color(0xFF2196F3),
                selectedTextColor = Color(0xFF2196F3),
                unselectedIconColor = Color(0xFFCCCCCC),
                unselectedTextColor = Color(0xFFCCCCCC),
                indicatorColor = Color(0xFFE3F2FD)
            )
        )
        
        NavigationBarItem(
            icon = {
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = "相册",
                    modifier = Modifier
                )
            },
            label = {
                Text(
                    text = "相册",
                    fontSize = 11.sp
                )
            },
            selected = currentRoute == NavRoutes.GALLERY,
            onClick = { onNavigate(NavRoutes.GALLERY) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = Color(0xFF2196F3),
                selectedTextColor = Color(0xFF2196F3),
                unselectedIconColor = Color(0xFFCCCCCC),
                unselectedTextColor = Color(0xFFCCCCCC),
                indicatorColor = Color(0xFFE3F2FD)
            )
        )
        
        NavigationBarItem(
            icon = {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = "我的",
                    modifier = Modifier
                )
            },
            label = {
                Text(
                    text = "我的",
                    fontSize = 11.sp
                )
            },
            selected = currentRoute == NavRoutes.PROFILE,
            onClick = { onNavigate(NavRoutes.PROFILE) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = Color(0xFF2196F3),
                selectedTextColor = Color(0xFF2196F3),
                unselectedIconColor = Color(0xFFCCCCCC),
                unselectedTextColor = Color(0xFFCCCCCC),
                indicatorColor = Color(0xFFE3F2FD)
            )
        )
    }
}
