package com.expenseflow.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.expenseflow.app.data.Settings
import com.expenseflow.app.data.SettingsRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(private val repo: SettingsRepository) : ViewModel() {

    val settings: StateFlow<Settings> =
        repo.settings.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), Settings())

    fun setDark(value: Boolean) = viewModelScope.launch { repo.setDark(value) }
    fun setCurrency(code: String) = viewModelScope.launch { repo.setCurrency(code) }
    fun setBudgetAlerts(value: Boolean) = viewModelScope.launch { repo.setBudgetAlerts(value) }
    fun setBillReminders(value: Boolean) = viewModelScope.launch { repo.setBillReminders(value) }
    fun setWeeklySummary(value: Boolean) = viewModelScope.launch { repo.setWeeklySummary(value) }
    fun setGoalMilestones(value: Boolean) = viewModelScope.launch { repo.setGoalMilestones(value) }
    fun setTransactionDisplayMode(mode: String) = viewModelScope.launch { repo.setTransactionDisplayMode(mode) }
    fun setSmsAutoDetect(value: Boolean) = viewModelScope.launch { repo.setSmsAutoDetect(value) }
}

class SettingsViewModelFactory(private val repo: SettingsRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SettingsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return SettingsViewModel(repo) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
