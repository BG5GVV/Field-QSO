package com.ham.qso

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.ham.qso.ui.components.AboutDialog
import com.ham.qso.ui.navigation.NavGraph
import com.ham.qso.ui.navigation.Screen
import com.ham.qso.ui.theme.AppThemeMode
import com.ham.qso.ui.theme.FieldQSOTheme

class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val app = application as QSOApplication

        setContent {
            var themeMode by remember { mutableStateOf(AppThemeMode.SYSTEM) }
            var showThemeMenu by remember { mutableStateOf(false) }

            FieldQSOTheme(themeMode = themeMode) {
                val navController = rememberNavController()
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route
                var showAboutDialog by remember { mutableStateOf(false) }

                Scaffold(
                    topBar = {
                        TopAppBar(
                            title = {
                                Text(
                                    text = "Field QSO",
                                    fontWeight = FontWeight.Bold
                                )
                            },
                            actions = {
                                // 关于软件按钮
                                IconButton(onClick = { showAboutDialog = true }) {
                                    Icon(
                                        imageVector = Icons.Default.Info,
                                        contentDescription = "关于软件 (About)"
                                    )
                                }

                                // 主题切换按钮
                                IconButton(onClick = { showThemeMenu = true }) {
                                    Icon(
                                        imageVector = when (themeMode) {
                                            AppThemeMode.SUNSHINE -> Icons.Default.WbSunny
                                            AppThemeMode.DARK -> Icons.Default.DarkMode
                                            AppThemeMode.LIGHT -> Icons.Default.LightMode
                                            AppThemeMode.SYSTEM -> Icons.Default.BrightnessAuto
                                        },
                                        contentDescription = "Theme Switcher"
                                    )
                                }

                                DropdownMenu(
                                    expanded = showThemeMenu,
                                    onDismissRequest = { showThemeMenu = false }
                                ) {
                                    DropdownMenuItem(
                                        text = { Text("跟随系统 (System)") },
                                        onClick = {
                                            themeMode = AppThemeMode.SYSTEM
                                            showThemeMenu = false
                                        },
                                        leadingIcon = { Icon(Icons.Default.BrightnessAuto, contentDescription = null) }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("日常浅色 (Light)") },
                                        onClick = {
                                            themeMode = AppThemeMode.LIGHT
                                            showThemeMenu = false
                                        },
                                        leadingIcon = { Icon(Icons.Default.LightMode, contentDescription = null) }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("夜间深色 (Dark)") },
                                        onClick = {
                                            themeMode = AppThemeMode.DARK
                                            showThemeMenu = false
                                        },
                                        leadingIcon = { Icon(Icons.Default.DarkMode, contentDescription = null) }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("户外阳光高对比度 (Sunshine)") },
                                        onClick = {
                                            themeMode = AppThemeMode.SUNSHINE
                                            showThemeMenu = false
                                        },
                                        leadingIcon = {
                                            Icon(
                                                Icons.Default.WbSunny,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    )
                                }
                            },
                            colors = TopAppBarDefaults.topAppBarColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            )
                        )
                    },
                    bottomBar = {
                        NavigationBar {
                            Screen.items.forEach { screen ->
                                val selected = currentRoute == screen.route
                                NavigationBarItem(
                                    icon = { Icon(screen.icon, contentDescription = screen.title) },
                                    label = { Text(screen.title) },
                                    selected = selected,
                                    onClick = {
                                        if (currentRoute != screen.route) {
                                            navController.navigate(screen.route) {
                                                popUpTo(navController.graph.findStartDestination().id) {
                                                    saveState = true
                                                }
                                                launchSingleTop = true
                                                restoreState = true
                                            }
                                        }
                                    }
                                )
                            }
                        }
                    }
                ) { innerPadding ->
                    NavGraph(
                        navController = navController,
                        app = app,
                        modifier = Modifier.padding(innerPadding)
                    )
                }

                if (showAboutDialog) {
                    AboutDialog(onDismiss = { showAboutDialog = false })
                }
            }
        }
    }
}
