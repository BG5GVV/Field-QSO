package com.ham.qso.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.ham.qso.QSOApplication
import com.ham.qso.ui.screens.log.LoggingScreen
import com.ham.qso.ui.screens.log.LoggingViewModel
import com.ham.qso.ui.screens.logbook.LogbookScreen
import com.ham.qso.ui.screens.logbook.LogbookViewModel
import com.ham.qso.ui.screens.session.SessionScreen
import com.ham.qso.ui.screens.session.SessionViewModel
import com.ham.qso.ui.screens.tools.ToolsScreen
import com.ham.qso.ui.screens.tools.ToolsViewModel
import com.ham.qso.ui.viewmodel.AppViewModelFactory

@Composable
fun NavGraph(
    navController: NavHostController,
    app: QSOApplication,
    modifier: Modifier = Modifier
) {
    val factory = AppViewModelFactory(app)

    NavHost(
        navController = navController,
        startDestination = Screen.Logging.route,
        modifier = modifier
    ) {
        composable(Screen.Logging.route) {
            val viewModel: LoggingViewModel = viewModel(factory = factory)
            LoggingScreen(viewModel = viewModel)
        }

        composable(Screen.Logbook.route) {
            val viewModel: LogbookViewModel = viewModel(factory = factory)
            LogbookScreen(viewModel = viewModel)
        }

        composable(Screen.Sessions.route) {
            val viewModel: SessionViewModel = viewModel(factory = factory)
            SessionScreen(viewModel = viewModel)
        }

        composable(Screen.Tools.route) {
            val viewModel: ToolsViewModel = viewModel(factory = factory)
            ToolsScreen(viewModel = viewModel)
        }
    }
}
