package com.ham.qso.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.ham.qso.QSOApplication
import com.ham.qso.ui.screens.log.LoggingViewModel
import com.ham.qso.ui.screens.logbook.LogbookViewModel
import com.ham.qso.ui.screens.session.SessionViewModel
import com.ham.qso.ui.screens.tools.ToolsViewModel

@Suppress("UNCHECKED_CAST")
class AppViewModelFactory(
    private val app: QSOApplication
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        val repository = app.repository
        return when {
            modelClass.isAssignableFrom(LoggingViewModel::class.java) -> {
                LoggingViewModel(repository) as T
            }
            modelClass.isAssignableFrom(LogbookViewModel::class.java) -> {
                LogbookViewModel(repository) as T
            }
            modelClass.isAssignableFrom(SessionViewModel::class.java) -> {
                SessionViewModel(repository) as T
            }
            modelClass.isAssignableFrom(ToolsViewModel::class.java) -> {
                ToolsViewModel(repository) as T
            }
            else -> throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}
