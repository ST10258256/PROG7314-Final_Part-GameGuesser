package com.example.gameguesser.repository

import android.content.Context
import com.example.gameguesser.DAOs.GameDAO.GameDao
import com.example.gameguesser.data.ApiService
import com.example.gameguesser.data.Game
import com.example.gameguesser.data.RetrofitClient.api
import com.example.gameguesser.models.CompareRequest
import com.example.gameguesser.models.ComparisonResponse
import com.example.gameguesser.models.GuessResponse
import com.example.gameguesser.utils.NetworkUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.Response

class GameRepository(
    private val dao: GameDao,
    private val api: ApiService,
    private val context : Context
) {

    // Fetch all games (offline first)
    suspend fun getAllGames(): List<Game> = withContext(Dispatchers.IO) {
        val localGames = dao.getAllGames()
        if (localGames.isNotEmpty()) {
            localGames
        } else {
            try {
                val apiRes = api.getAllGamesFull().execute()
                if (apiRes.isSuccessful) {
                    val games = apiRes.body() ?: emptyList()
                    dao.insertGames(games)
                    games
                } else {
                    emptyList()
                }
            } catch (e: Exception) {
                localGames // fallback to offline cache
            }
        }
    }

    // Fetch a single game by ID (offline first)
    suspend fun getGameById(id: String): Game? = withContext(Dispatchers.IO) {
        val localGame = dao.getGameById(id)
        if (localGame != null) {
            localGame
        } else {
            try {
                val apiRes = api.getGameById(id).execute()
                if (apiRes.isSuccessful) {
                    apiRes.body()?.let { game ->
                        dao.insertGame(game)
                        game
                    }
                } else {
                    null
                }
            } catch (e: Exception) {
                null
            }
        }
    }

    suspend fun syncFromApi() {
        withContext(Dispatchers.IO) {
            try {
                val apiResponse = api.getAllGamesFull().execute()
                if (apiResponse.isSuccessful) {
                    val games = apiResponse.body() ?: emptyList()
                    dao.insertGames(games) // insert into Room DB
                }
            } catch (e: Exception) {
            }
        }
    }

    // ---------------------------
    // Find games by keyword in local DB
    // ---------------------------
    suspend fun findGamesByKeyword(keyword: String): List<Game> {
        return withContext(Dispatchers.IO) {
            val allGames = dao.getAllGames()
            if (keyword.isBlank()) allGames
            else allGames.filter { game ->
                game.name.contains(keyword, ignoreCase = true) ||
                        game.keywords.any { it.contains(keyword, ignoreCase = true) }
            }
        }
    }

    // ---------------------------
    // Get random game (offline-safe)
    // ---------------------------
    suspend fun getRandomGame(): Game? {
        return withContext(Dispatchers.IO) {
            try {
                val response = api.getRandomGame().execute()
                if (response.isSuccessful) {
                    val randomGameResponse = response.body()
                    val game = randomGameResponse?.let { resp ->
                        Game(
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

                    game?.let { dao.insertGame(it) }
                    game ?: dao.getAllGames().randomOrNull()
                } else {
                    dao.getAllGames().randomOrNull()
                }
            } catch (e: Exception) {
                dao.getAllGames().randomOrNull()
            }
        }
    }

    suspend fun getGameByIdOfflineSafe(id: String): Game? {
        return try {
            if (NetworkUtils.isOnline(context)) {
                val response = api.getGameById(id).execute()
                response.body()?.let { game ->
                    dao.insertGame(game)
                    game // <-- return the game here
                } ?: dao.getGameById(id)
            } else {
                dao.getGameById(id)
            }
        } catch (e: Exception) {
            dao.getGameById(id)
        }
    }



    // Submit a guess (network only)
    suspend fun submitGuess(gameId: String, guess: String): GuessResponse? = withContext(Dispatchers.IO) {
        try {
            val apiRes = api.submitGuess(gameId, guess).execute()
            if (apiRes.isSuccessful) apiRes.body() else null
        } catch (e: Exception) {
            null
        }
    }

    // Compare a guess for CompareGameMode
    suspend fun compareGame(compareRequest: CompareRequest): ComparisonResponse? = withContext(Dispatchers.IO) {
        try {
            val apiRes = api.compareGame(compareRequest).execute()
            if (apiRes.isSuccessful) apiRes.body() else null
        } catch (e: Exception) {
            null
        }
    }


}
