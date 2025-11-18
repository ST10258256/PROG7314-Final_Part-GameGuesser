package com.example.gameguesser.ui.keyWordGame

import android.app.AlertDialog
import android.content.Context
import android.icu.util.Calendar
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.example.gameguesser.DAOs.UserDao
import com.example.gameguesser.Database.AppDatabase
import com.example.gameguesser.Database.UserDatabase
import com.example.gameguesser.R
import com.example.gameguesser.data.RetrofitClient
import com.example.gameguesser.repository.GameRepository
import com.example.gameguesser.utils.NetworkUtils
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class KeyGameFragment : Fragment() {

    private lateinit var userDb: UserDatabase
    private lateinit var userDao: UserDao
    private lateinit var repository: GameRepository

    private var currentGameId: String? = null
    private var currentGameName: String? = null
    private var currentGameCover: String? = null

    private lateinit var resultText: TextView
    private lateinit var guessInput: AutoCompleteTextView
    private lateinit var guessButton: Button
    private lateinit var keywordsChipGroup: ChipGroup
    private lateinit var heartsContainer: LinearLayout
    private var hearts: MutableList<ImageView> = mutableListOf()
    private val maxLives = 5

    private lateinit var adapter: ArrayAdapter<String>
    private var allGames: MutableList<String> = mutableListOf()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_key_game, container, false)

        userDb = UserDatabase.getDatabase(requireContext())
        userDao = userDb.userDao()
        val gameDao = AppDatabase.getDatabase(requireContext()).gameDao()
        repository = GameRepository(gameDao, RetrofitClient.api, requireContext())

        resultText = view.findViewById(R.id.resultText)
        guessInput = view.findViewById(R.id.guessInput)
        guessButton = view.findViewById(R.id.guessButton)
        keywordsChipGroup = view.findViewById(R.id.keywordsChipGroup)
        heartsContainer = view.findViewById(R.id.guessHeartsContainer)

        adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, allGames)
        guessInput.setAdapter(adapter)

        // Initialize hearts
        hearts.clear()
        for (i in 0 until maxLives) {
            val heart = heartsContainer.getChildAt(i) as ImageView
            hearts.add(heart)
        }

        fetchAllGames()
        fetchRandomGame()

        // AutoComplete filter
        guessInput.addTextChangedListener { editable ->
            val input = editable.toString()
            val filtered = allGames.filter { it.contains(input, ignoreCase = true) }
            adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, filtered)
            guessInput.setAdapter(adapter)
            adapter.notifyDataSetChanged()
            guessInput.showDropDown()
        }

        guessButton.setOnClickListener {
            val guess = guessInput.text.toString()
            if (guess.isBlank()) {
                Toast.makeText(requireContext(), "Enter a guess", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            currentGameId?.let { id ->
                submitGuess(id, guess)
            } ?: Toast.makeText(requireContext(), "Game not loaded yet", Toast.LENGTH_SHORT).show()
        }

        return view
    }

    // ---------------------------
    // Fetch all games (Offline-Ready)
    // ---------------------------
    private fun fetchAllGames() {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                if (NetworkUtils.isOnline(requireContext())) {
                    repository.syncFromApi()
                }

                val games = repository.findGamesByKeyword("")

                allGames.clear()
                allGames.addAll(games.map { it.name })

                withContext(Dispatchers.Main) {
                    adapter.notifyDataSetChanged()
                }

            } catch (e: Exception) {
                val fallbackGames = repository.findGamesByKeyword("")
                allGames.clear()
                allGames.addAll(fallbackGames.map { it.name })

                withContext(Dispatchers.Main) {
                    adapter.notifyDataSetChanged()
                }
            }
        }
    }


    // ---------------------------
    // Fetch random game
    // ---------------------------
    private fun fetchRandomGame() {
        lifecycleScope.launch(Dispatchers.IO) {
            val game = repository.getRandomGame()
            game?.let {
                currentGameId = it.id
                currentGameName = it.name
                currentGameCover = it.coverImageUrl

                withContext(Dispatchers.Main) {
                    keywordsChipGroup.removeAllViews()
                    it.keywords.forEach { keyword -> addChip(keyword) }
                    resultText.text = ""
                    guessInput.text.clear()
                    resetHearts()
                }
            }
        }
    }

    // ---------------------------
    // Submit guess (Offline-Ready)
    // ---------------------------
    private fun submitGuess(gameId: String, guess: String) {
        if (NetworkUtils.isOnline(requireContext())) {
            RetrofitClient.api.submitGuess(gameId, guess).enqueue(object :
                retrofit2.Callback<com.example.gameguesser.models.GuessResponse> {

                override fun onResponse(
                    call: retrofit2.Call<com.example.gameguesser.models.GuessResponse>,
                    response: retrofit2.Response<com.example.gameguesser.models.GuessResponse>
                ) {
                    val result = response.body()
                    processGuessResult(result?.correct ?: false, result?.hint)
                }

                override fun onFailure(
                    call: retrofit2.Call<com.example.gameguesser.models.GuessResponse>,
                    t: Throwable
                ) {
                    Toast.makeText(requireContext(), "Error: ${t.message}", Toast.LENGTH_SHORT).show()
                }
            })
        } else {
            // OFFLINE fallback: match keyword to random game
            lifecycleScope.launch(Dispatchers.IO) {
                val game = repository.getRandomGame()
                val correct = game?.keywords?.any { it.equals(guess, ignoreCase = true) } ?: false
                val hint = if (!correct && game != null && game.keywords.isNotEmpty()) game.keywords.random() else null
                withContext(Dispatchers.Main) {
                    processGuessResult(correct, hint)
                }
            }
        }
    }

    // ---------------------------
    // Process guess result
    // ---------------------------
    private fun processGuessResult(correct: Boolean, hint: String?) {
        if (correct) {
            showEndGameDialog(true, currentGameName ?: "Unknown", currentGameCover)
        } else {
            if (hearts.isNotEmpty()) {
                val lastHeart = hearts.removeAt(hearts.size - 1)
                lastHeart.visibility = View.INVISIBLE
            }
            hint?.let { addChip(it) }
            resultText.text = "Wrong"
            if (hearts.isEmpty()) {
                showEndGameDialog(false, currentGameName ?: "Unknown", currentGameCover)
                guessButton.isEnabled = false
                guessInput.isEnabled = false
            } else {
                Toast.makeText(requireContext(), "Hint: ${hint ?: "No hint"}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // ---------------------------
    // UI helpers
    // ---------------------------
    private fun resetHearts() {
        hearts.clear()
        for (i in 0 until maxLives) {
            val heart = heartsContainer.getChildAt(i) as ImageView
            heart.visibility = View.VISIBLE
            hearts.add(heart)
        }
        guessButton.isEnabled = true
        guessInput.isEnabled = true
    }

    private fun addChip(text: String) {
        val chip = Chip(requireContext())
        chip.text = text
        chip.isClickable = false
        chip.isCheckable = false
        chip.setTextColor(resources.getColor(android.R.color.white, null))
        chip.chipBackgroundColor = resources.getColorStateList(android.R.color.holo_purple, null)
        keywordsChipGroup.addView(chip)
    }

    // ---------------------------
    // End game dialog + streaks
    // ---------------------------
    private fun showEndGameDialog(won: Boolean, gameName: String, coverUrl: String?) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_end_game, null)
        val imageView = dialogView.findViewById<ImageView>(R.id.gameCoverImage)
        val titleText = dialogView.findViewById<TextView>(R.id.dialogTitle)
        val playAgainBtn = dialogView.findViewById<Button>(R.id.playAgainButton)
        val mainMenuBtn = dialogView.findViewById<Button>(R.id.mainMenuButton)

        titleText.text = if (won) "Congratulations" else "Better luck next time"

        if (won) {
            lifecycleScope.launch(Dispatchers.IO) {
                val userId = getLoggedInUserId() ?: return@launch
                val user = userDao.getUser(userId) ?: return@launch

                if (!isToday(user.lastPlayedKW)) user.streakKW += 1
                if (user.streakKW > user.bestStreakKW) user.bestStreakKW = user.streakKW
                user.lastPlayedKW = System.currentTimeMillis()
                userDao.updateUser(user)
            }
        }

        coverUrl?.let { Glide.with(this).load(it).into(imageView) }

        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .setCancelable(false)
            .create()

        playAgainBtn.setOnClickListener {
            dialog.dismiss()
            resetGame()
        }

        mainMenuBtn.setOnClickListener {
            dialog.dismiss()
            requireActivity().onBackPressed()
        }

        dialog.show()
    }

    private fun resetGame() {
        keywordsChipGroup.removeAllViews()
        resetHearts()
        guessInput.text.clear()
        resultText.text = ""
        fetchRandomGame()
    }

    // ---------------------------
    // Helpers
    // ---------------------------
    private fun isToday(timestamp: Long): Boolean {
        if (timestamp == 0L) return false
        val lastPlayedCal = Calendar.getInstance().apply { timeInMillis = timestamp }
        val todayCal = Calendar.getInstance()
        return lastPlayedCal.get(Calendar.YEAR) == todayCal.get(Calendar.YEAR)
                && lastPlayedCal.get(Calendar.DAY_OF_YEAR) == todayCal.get(Calendar.DAY_OF_YEAR)
    }

    private fun getLoggedInUserId(): String? {
        val prefs = requireActivity().getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
        return prefs.getString("userId", null)
    }
}

