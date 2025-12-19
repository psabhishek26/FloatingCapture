package com.psabhishek26.floatingcapture.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import com.psabhishek26.floatingcapture.domain.model.AppSettings
import com.psabhishek26.floatingcapture.domain.repository.SettingsRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

@Singleton
class SettingsRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : SettingsRepository {

    private object Keys {
        val SCREENSHOT_ENABLED = booleanPreferencesKey("screenshot_enabled")
        val RECORDING_ENABLED = booleanPreferencesKey("recording_enabled")
        val FLOATING_BUTTON_VISIBLE = booleanPreferencesKey("floating_button_visible")
    }

    override val settings: Flow<AppSettings> = context.dataStore.data.map { prefs ->
        AppSettings(
            screenshotEnabled = prefs[Keys.SCREENSHOT_ENABLED] ?: true,
            recordingEnabled = prefs[Keys.RECORDING_ENABLED] ?: true,
            floatingButtonVisible = prefs[Keys.FLOATING_BUTTON_VISIBLE] ?: true
        )
    }

    override suspend fun updateSettings(settings: AppSettings) {
        context.dataStore.edit { prefs ->
            prefs[Keys.SCREENSHOT_ENABLED] = settings.screenshotEnabled
            prefs[Keys.RECORDING_ENABLED] = settings.recordingEnabled
            prefs[Keys.FLOATING_BUTTON_VISIBLE] = settings.floatingButtonVisible
        }
    }

    override fun getSettingsSync(): AppSettings = runBlocking {
        settings.first()
    }
}