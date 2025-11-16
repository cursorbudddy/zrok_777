package com.multiwhatsapp.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.multiwhatsapp.data.models.MatrixAccount
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import timber.log.Timber

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "accounts")

class AccountRepository(private val context: Context) {

    companion object {
        private val ACCOUNTS_KEY = stringPreferencesKey("matrix_accounts")
        private val ACTIVE_ACCOUNT_KEY = stringPreferencesKey("active_account_id")
    }

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    /**
     * Get all saved accounts
     */
    fun getAccounts(): Flow<List<MatrixAccount>> {
        return context.dataStore.data.map { preferences ->
            try {
                val accountsJson = preferences[ACCOUNTS_KEY] ?: "[]"
                json.decodeFromString<List<MatrixAccount>>(accountsJson)
            } catch (e: Exception) {
                Timber.e(e, "Error decoding accounts")
                emptyList()
            }
        }
    }

    /**
     * Get active account ID
     */
    fun getActiveAccountId(): Flow<String?> {
        return context.dataStore.data.map { preferences ->
            preferences[ACTIVE_ACCOUNT_KEY]
        }
    }

    /**
     * Save account
     */
    suspend fun saveAccount(account: MatrixAccount) {
        context.dataStore.edit { preferences ->
            try {
                val accountsJson = preferences[ACCOUNTS_KEY] ?: "[]"
                val accounts = json.decodeFromString<List<MatrixAccount>>(accountsJson).toMutableList()

                // Update or add account
                val existingIndex = accounts.indexOfFirst { it.id == account.id }
                if (existingIndex >= 0) {
                    accounts[existingIndex] = account
                } else {
                    accounts.add(account)
                }

                preferences[ACCOUNTS_KEY] = json.encodeToString(accounts)

                Timber.d("Account saved: ${account.userId}")
            } catch (e: Exception) {
                Timber.e(e, "Error saving account")
            }
        }
    }

    /**
     * Remove account
     */
    suspend fun removeAccount(accountId: String) {
        context.dataStore.edit { preferences ->
            try {
                val accountsJson = preferences[ACCOUNTS_KEY] ?: "[]"
                val accounts = json.decodeFromString<List<MatrixAccount>>(accountsJson)
                    .filterNot { it.id == accountId }

                preferences[ACCOUNTS_KEY] = json.encodeToString(accounts)

                // Clear active account if it was removed
                if (preferences[ACTIVE_ACCOUNT_KEY] == accountId) {
                    preferences.remove(ACTIVE_ACCOUNT_KEY)
                }

                Timber.d("Account removed: $accountId")
            } catch (e: Exception) {
                Timber.e(e, "Error removing account")
            }
        }
    }

    /**
     * Set active account
     */
    suspend fun setActiveAccount(accountId: String) {
        context.dataStore.edit { preferences ->
            preferences[ACTIVE_ACCOUNT_KEY] = accountId
            Timber.d("Active account set to: $accountId")
        }
    }

    /**
     * Clear all accounts (logout all)
     */
    suspend fun clearAllAccounts() {
        context.dataStore.edit { preferences ->
            preferences.remove(ACCOUNTS_KEY)
            preferences.remove(ACTIVE_ACCOUNT_KEY)
            Timber.d("All accounts cleared")
        }
    }
}
