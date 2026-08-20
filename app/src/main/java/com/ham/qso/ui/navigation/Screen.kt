package com.ham.qso.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.FormatListBulleted
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material.icons.filled.Terrain
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(
    val route: String,
    val title: String,
    val icon: ImageVector
) {
    object Logging : Screen("logging", "极速录入", Icons.Default.Radio)
    object Logbook : Screen("logbook", "通联日志", Icons.Default.FormatListBulleted)
    object Sessions : Screen("sessions", "架台会话", Icons.Default.Terrain)
    object Tools : Screen("tools", "工具箱", Icons.Default.Build)

    companion object {
        val items = listOf(Logging, Logbook, Sessions, Tools)
    }
}
