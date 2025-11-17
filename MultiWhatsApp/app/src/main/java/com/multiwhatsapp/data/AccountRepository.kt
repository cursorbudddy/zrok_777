package com.multiwhatsapp.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "accounts")

class AccountRepository(private val context: Context) {
    private val gson = Gson()
    private val accountsKey = stringPreferencesKey("accounts")

    val accounts: Flow<List<Account>> = context.dataStore.data.map { preferences ->
        val accountsJson = preferences[accountsKey] ?: "[]"
        val type = object : TypeToken<List<Account>>() {}.type
        gson.fromJson(accountsJson, type) ?: emptyList()
    }

    suspend fun addAccount(account: Account) {
        context.dataStore.edit { preferences ->
            val currentAccounts = preferences[accountsKey]?.let { json ->
                val type = object : TypeToken<List<Account>>() {}.type
                gson.fromJson<List<Account>>(json, type)?.toMutableList() ?: mutableListOf()
            } ?: mutableListOf()

            currentAccounts.add(account)
            preferences[accountsKey] = gson.toJson(currentAccounts)
        }
    }

    suspend fun removeAccount(accountId: String) {
        context.dataStore.edit { preferences ->
            val currentAccounts = preferences[accountsKey]?.let { json ->
                val type = object : TypeToken<List<Account>>() {}.type
                gson.fromJson<List<Account>>(json, type)?.toMutableList() ?: mutableListOf()
            } ?: mutableListOf()

            currentAccounts.removeAll { it.id == accountId }
            preferences[accountsKey] = gson.toJson(currentAccounts)
        }
    }

    suspend fun updateAccount(account: Account) {
        context.dataStore.edit { preferences ->
            val currentAccounts = preferences[accountsKey]?.let { json ->
                val type = object : TypeToken<List<Account>>() {}.type
                gson.fromJson<List<Account>>(json, type)?.toMutableList() ?: mutableListOf()
            } ?: mutableListOf()

            val index = currentAccounts.indexOfFirst { it.id == account.id }
            if (index >= 0) {
                currentAccounts[index] = account
                preferences[accountsKey] = gson.toJson(currentAccounts)
            }
        }
    }
}
