package com.psabhishek26.floatingcapture.domain.repository

import com.psabhishek26.floatingcapture.domain.model.AppSettings
import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    val settings: Flow<AppSettings>
    suspend fun updateSettings(settings: AppSettings)
    fun getSettingsSync(): AppSettings
}