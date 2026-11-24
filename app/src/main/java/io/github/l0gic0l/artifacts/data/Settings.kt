package io.github.l0gic0l.artifacts.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first

enum class InstallerChoice(val label: String) {
    APP_MANAGER("App Manager"),
    SYSTEM("System installer"),
    ASK("Ask every time"),
}

data class Settings(
    val token: String,
    val installer: InstallerChoice,
    val hideEmpty: Boolean,
)

private val Context.dataStore by preferencesDataStore("settings")

/** Stored in app-private storage; backups are disabled in the manifest. */
class SettingsStore(private val context: Context) {
    private val tokenKey = stringPreferencesKey("token")
    private val installerKey = stringPreferencesKey("installer")
    private val hideEmptyKey = booleanPreferencesKey("hide_empty")

    suspend fun load(): Settings {
        val p = context.dataStore.data.first()
        return Settings(
            token = p[tokenKey].orEmpty(),
            installer = runCatching { InstallerChoice.valueOf(p[installerKey].orEmpty()) }
                .getOrDefault(InstallerChoice.APP_MANAGER),
            hideEmpty = p[hideEmptyKey] ?: true,
        )
    }

    suspend fun setToken(value: String) = context.dataStore.edit { it[tokenKey] = value }
    suspend fun setInstaller(value: InstallerChoice) = context.dataStore.edit { it[installerKey] = value.name }
    suspend fun setHideEmpty(value: Boolean) = context.dataStore.edit { it[hideEmptyKey] = value }
}
