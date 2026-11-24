package com.example.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.i18n.AppLanguage
import com.example.model.HighScores
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.arcadeDataStore: DataStore<Preferences> by preferencesDataStore(name = "hotspot_arcade_prefs")

class ArcadePreferences(private val context: Context) {

    private object Keys {
        val LANGUAGE = stringPreferencesKey("language")
        val PONG_BEST = intPreferencesKey("pong_best")
        val SNAKE_BEST = intPreferencesKey("snake_best")
        val BRICKS_BEST = intPreferencesKey("bricks_best")
        val TTT_WINS = intPreferencesKey("ttt_wins")
    }

    val languageFlow: Flow<AppLanguage> = context.arcadeDataStore.data.map { prefs ->
        val saved = prefs[Keys.LANGUAGE]
        if (saved == AppLanguage.EN.code) AppLanguage.EN else AppLanguage.BN
    }

    val highScoresFlow: Flow<HighScores> = context.arcadeDataStore.data.map { prefs ->
        HighScores(
            pongBest = prefs[Keys.PONG_BEST] ?: 0,
            snakeBest = prefs[Keys.SNAKE_BEST] ?: 0,
            bricksBest = prefs[Keys.BRICKS_BEST] ?: 0,
            ticTacToeWins = prefs[Keys.TTT_WINS] ?: 0,
        )
    }

    suspend fun setLanguage(language: AppLanguage) {
        context.arcadeDataStore.edit { prefs ->
            prefs[Keys.LANGUAGE] = language.code
        }
    }

    suspend fun recordPongScore(score: Int) {
        context.arcadeDataStore.edit { prefs ->
            val current = prefs[Keys.PONG_BEST] ?: 0
            if (score > current) prefs[Keys.PONG_BEST] = score
        }
    }

    suspend fun recordSnakeScore(score: Int) {
        context.arcadeDataStore.edit { prefs ->
            val current = prefs[Keys.SNAKE_BEST] ?: 0
            if (score > current) prefs[Keys.SNAKE_BEST] = score
        }
    }

    suspend fun recordBricksScore(score: Int) {
        context.arcadeDataStore.edit { prefs ->
            val current = prefs[Keys.BRICKS_BEST] ?: 0
            if (score > current) prefs[Keys.BRICKS_BEST] = score
        }
    }

    suspend fun incrementTicTacToeWin() {
        context.arcadeDataStore.edit { prefs ->
            val current = prefs[Keys.TTT_WINS] ?: 0
            prefs[Keys.TTT_WINS] = current + 1
        }
    }
}
