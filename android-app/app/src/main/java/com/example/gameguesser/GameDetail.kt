package com.example.gameguesser

import android.os.Bundle
import android.util.Log
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.example.gameguesser.Database.AppDatabase
import com.example.gameguesser.data.Game
import com.example.gameguesser.data.RetrofitClient
import com.example.gameguesser.repository.GameRepository
import com.example.gameguesser.utils.NetworkUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class GameDetail : AppCompatActivity() {

    private lateinit var repository: GameRepository
    private var gameId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_game_detail)

        val imageCover = findViewById<ImageView>(R.id.imageViewCover)
        val textName = findViewById<TextView>(R.id.textViewName)
        val textGenre = findViewById<TextView>(R.id.textViewGenre)
        val textPlatform = findViewById<TextView>(R.id.textViewPlatform)
        val textReleaseYear = findViewById<TextView>(R.id.textViewReleaseYear)
        val textDeveloper = findViewById<TextView>(R.id.textViewDeveloper)
        val textPublisher = findViewById<TextView>(R.id.textViewPublisher)
        val textBudget = findViewById<TextView>(R.id.textViewBudget)
        val textSaga = findViewById<TextView>(R.id.textViewSaga)
        val textPOV = findViewById<TextView>(R.id.textViewPOV)
        val textDescription = findViewById<TextView>(R.id.textViewDescription)

        gameId = intent.getStringExtra("GAME_ID")
        if (gameId.isNullOrEmpty()) {
            Toast.makeText(this, "Invalid game ID", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // Initialize repository
        val dao = AppDatabase.getDatabase(this).gameDao()
        repository = GameRepository(dao, RetrofitClient.api, this)

        fetchGame()
    }

    private fun fetchGame() {
        lifecycleScope.launch(Dispatchers.IO) {
            val game: Game? = try {
                // fetch using repository (handles online/offline)
                repository.getGameByIdOfflineSafe(gameId!!)
            } catch (e: Exception) {
                null
            }

            withContext(Dispatchers.Main) {
                if (game != null) {
                    populateUI(game)
                } else {
                    Toast.makeText(this@GameDetail, "Game not found", Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
        }
    }

    private fun populateUI(game: Game) {
        val imageCover = findViewById<ImageView>(R.id.imageViewCover)
        val textName = findViewById<TextView>(R.id.textViewName)
        val textGenre = findViewById<TextView>(R.id.textViewGenre)
        val textPlatform = findViewById<TextView>(R.id.textViewPlatform)
        val textReleaseYear = findViewById<TextView>(R.id.textViewReleaseYear)
        val textDeveloper = findViewById<TextView>(R.id.textViewDeveloper)
        val textPublisher = findViewById<TextView>(R.id.textViewPublisher)
        val textBudget = findViewById<TextView>(R.id.textViewBudget)
        val textSaga = findViewById<TextView>(R.id.textViewSaga)
        val textPOV = findViewById<TextView>(R.id.textViewPOV)
        val textDescription = findViewById<TextView>(R.id.textViewDescription)

        textName.text = game.name
        textGenre.text = "Genre: ${game.genre}"
        textPlatform.text = "Platforms: ${game.platforms.joinToString(", ")}"
        textReleaseYear.text = "Release Year: ${game.releaseYear}"
        textDeveloper.text = "Developer: ${game.developer}"
        textPublisher.text = "Publisher: ${game.publisher}"
        textBudget.text = "Budget: ${game.budget}"
        textSaga.text = "Saga: ${game.saga}"
        textPOV.text = "POV: ${game.pov}"
        textDescription.text = game.description

        if (!game.coverImageUrl.isNullOrEmpty()) {
            Glide.with(this)
                .load(game.coverImageUrl)
                .into(imageCover)
        }
    }
}

