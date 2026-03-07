package com.investtrack.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.investtrack.ui.theme.AppTheme
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "investtrack_prefs")

@Singleton
class PreferencesManager @Inject constructor(@ApplicationContext private val context: Context) {

    private object Keys {
        val APP_THEME       = stringPreferencesKey("app_theme")
        val LOCK_ENABLED    = booleanPreferencesKey("lock_enabled")
        val LOCK_TYPE       = stringPreferencesKey("lock_type")  // PIN, BIOMETRIC, PATTERN
        val PIN_HASH        = stringPreferencesKey("pin_hash")
        val PATTERN_HASH    = stringPreferencesKey("pattern_hash")
        val BIOMETRIC_ENABLED = booleanPreferencesKey("biometric_enabled")
        val CURRENCY_SYMBOL = stringPreferencesKey("currency_symbol")
        val HIDE_AMOUNTS    = booleanPreferencesKey("hide_amounts")
        val AUTO_LOCK_MINS  = intPreferencesKey("auto_lock_mins")
    }

    val appTheme: Flow<AppTheme> = context.dataStore.data
        .catch { emit(emptyPreferences()) }
        .map { prefs ->
            val name = prefs[Keys.APP_THEME] ?: AppTheme.DARK.name
            try { AppTheme.valueOf(name) } catch (e: Exception) { AppTheme.DARK }
        }

    val lockEnabled: Flow<Boolean> = context.dataStore.data
        .catch { emit(emptyPreferences()) }
        .map { it[Keys.LOCK_ENABLED] ?: false }

    val lockType: Flow<LockType> = context.dataStore.data
        .catch { emit(emptyPreferences()) }
        .map { prefs ->
            val name = prefs[Keys.LOCK_TYPE] ?: LockType.PIN.name
            try { LockType.valueOf(name) } catch (e: Exception) { LockType.PIN }
        }

    val pinHash: Flow<String?> = context.dataStore.data
        .catch { emit(emptyPreferences()) }
        .map { it[Keys.PIN_HASH] }

    val biometricEnabled: Flow<Boolean> = context.dataStore.data
        .catch { emit(emptyPreferences()) }
        .map { it[Keys.BIOMETRIC_ENABLED] ?: false }

    val hideAmounts: Flow<Boolean> = context.dataStore.data
        .catch { emit(emptyPreferences()) }
        .map { it[Keys.HIDE_AMOUNTS] ?: false }

    val autoLockMins: Flow<Int> = context.dataStore.data
        .catch { emit(emptyPreferences()) }
        .map { it[Keys.AUTO_LOCK_MINS] ?: 5 }

    suspend fun setTheme(theme: AppTheme) {
        context.dataStore.edit { it[Keys.APP_THEME] = theme.name }
    }

    suspend fun setLockEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.LOCK_ENABLED] = enabled }
    }

    suspend fun setLockType(type: LockType) {
        context.dataStore.edit { it[Keys.LOCK_TYPE] = type.name }
    }

    suspend fun setPinHash(hash: String) {
        context.dataStore.edit { it[Keys.PIN_HASH] = hash }
    }

    suspend fun setPatternHash(hash: String) {
        context.dataStore.edit { it[Keys.PATTERN_HASH] = hash }
    }

    suspend fun setBiometricEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.BIOMETRIC_ENABLED] = enabled }
    }

    suspend fun setHideAmounts(hide: Boolean) {
        context.dataStore.edit { it[Keys.HIDE_AMOUNTS] = hide }
    }

    suspend fun setAutoLockMins(mins: Int) {
        context.dataStore.edit { it[Keys.AUTO_LOCK_MINS] = mins }
    }

    suspend fun clearLock() {
        context.dataStore.edit {
            it.remove(Keys.LOCK_ENABLED)
            it.remove(Keys.LOCK_TYPE)
            it.remove(Keys.PIN_HASH)
            it.remove(Keys.PATTERN_HASH)
            it.remove(Keys.BIOMETRIC_ENABLED)
        }
    }
}

enum class LockType(val displayName: String) {
    PIN("PIN"),
    BIOMETRIC("Biometric"),
    PATTERN("Pattern")
}
