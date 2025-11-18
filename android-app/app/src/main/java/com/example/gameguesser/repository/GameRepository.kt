package com.example.gameguesser.repository

import android.content.Context
import com.example.gameguesser.DAOs.GameDAO.GameDao
import com.example.gameguesser.data.ApiService
import com.example.gameguesser.data.Game
import com.example.gameguesser.data.RetrofitClient
import com.example.gameguesser.models.CompareRequest
import com.example.gameguesser.models.ComparisonResponse
import com.example.gameguesser.models.GuessResponse
import com.example.gameguesser.models.RandomGameResponse
import com.example.gameguesser.utils.NetworkUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class GameRepository(
    private val dao: GameDao,
    private val api: ApiService,
    private val context: Context
) {

    // Map API response → local Game
    private fun mapRandomGameResponseToGame(resp: RandomGameResponse): Game {
        return Game(
            id = resp.id,
            name = resp.name,
            keywords = resp.keywords,
            coverImageUrl = resp.coverImageUrl ?: "",
            genre = "",
            platforms = emptyList(),
            releaseYear = 0,
            developer = "",
            publisher = "",
            description = "",
            budget = "",
            saga = "",
            pov = "",
            clues = emptyList()
        )
    }



    // Get random game (offline)
    suspend fun getRandomGame(): Game? = withContext(Dispatchers.IO) {
        try {
            if (NetworkUtils.isOnline(context)) {
                val response = api.getRandomGame().execute()
                val apiGame = response.body()
                val game = apiGame?.let { mapRandomGameResponseToGame(it) }

                // Insert mapped Game into Room
                game?.let { dao.insertGame(it) }

                return@withContext game ?: dao.getAllGames().randomOrNull()
            } else {
                dao.getAllGames().randomOrNull()
            }
        } catch (_: Exception) {
            dao.getAllGames().randomOrNull()
        }
    }


    // Get game by ID (offline-safe)
    suspend fun getGameByIdOfflineSafe(id: String): Game? = withContext(Dispatchers.IO) {
        val localGame = dao.getGameById(id)
        if (localGame != null) return@withContext localGame

        if (NetworkUtils.isOnline(context)) {
            try {
                val response = api.getRandomGame().execute()
                val apiGame = response.body()
                val game = apiGame?.let { mapRandomGameResponseToGame(it) }
                game?.let { dao.insertGame(it) }
                return@withContext game ?: dao.getAllGames().randomOrNull()

            } catch (_: Exception) {
                return@withContext dao.getGameById(id)
            }
        } else {
            dao.getGameById(id)
        }
    }


    // Sync all games from API
    suspend fun syncFromApi() = withContext(Dispatchers.IO) {
        if (!NetworkUtils.isOnline(context)) return@withContext
        try {
            val response = api.getRandomGame().execute()
            val apiGame = response.body()
            val game = apiGame?.let { mapRandomGameResponseToGame(it) }
            game?.let { dao.insertGame(it) }
        } catch (_: Exception) {}
    }



    // Other functions remain offline-safe
    suspend fun getAllGames(): List<Game> = withContext(Dispatchers.IO) {
        val localGames = dao.getAllGames()
        if (localGames.isNotEmpty()) return@withContext localGames

        return@withContext try {
            if (NetworkUtils.isOnline(context)) {
                val response = api.getRandomGame().execute()
                val apiGame = response.body()
                val game = apiGame?.let { mapRandomGameResponseToGame(it) }
                game?.let { dao.insertGame(it) }

                game?.let { listOf(it) } ?: emptyList()
            } else {
                emptyList()
            }
        } catch (_: Exception) {
            emptyList()
        }
    }



    suspend fun findGamesByKeyword(keyword: String): List<Game> = withContext(Dispatchers.IO) {
        val allGames = dao.getAllGames()
        if (keyword.isBlank()) allGames
        else allGames.filter { game ->
            game.name.contains(keyword, ignoreCase = true) ||
                    game.keywords.any { it.contains(keyword, ignoreCase = true) }
        }
    }


    suspend fun submitGuess(gameId: String, guess: String): GuessResponse? =
        withContext(Dispatchers.IO) {
            if (!NetworkUtils.isOnline(context)) return@withContext null
            try {
                val response = api.submitGuess(gameId, guess).execute()
                if (response.isSuccessful) response.body() else null
            } catch (_: Exception) {
                null
            }
        }

    suspend fun compareGame(request: CompareRequest): ComparisonResponse? {
        return try {
            if (NetworkUtils.isOnline(context)) {
                val response = RetrofitClient.api.compareGame(request).execute()
                response.body()
            } else {
                val game = getGameByIdOfflineSafe(request.gameId)
                val matches = mutableMapOf<String, String>()
                game?.keywords?.forEach { keyword ->
                    matches[keyword] = if (keyword.equals(request.guessName, ignoreCase = true)) "exact" else "partial"
                }
                ComparisonResponse(
                    correct = matches.any { it.value == "exact" },
                    matches = matches
                )
            }
        } catch (_: Exception) {
            null
        }
    }





}
