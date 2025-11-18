package com.example.gameguesser.ui.compareGameMode

import android.app.AlertDialog
import android.content.Context
import android.content.res.ColorStateList
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
import com.example.gameguesser.models.CompareRequest
import com.example.gameguesser.models.ComparisonResponse
import com.example.gameguesser.repository.GameRepository
import com.example.gameguesser.utils.NetworkUtils
import com.google.android.flexbox.FlexboxLayout
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class CompareGameFragment : Fragment() {

    private lateinit var userDb: UserDatabase
    private lateinit var userDao: UserDao
    private lateinit var repository: GameRepository

    private var currentGameId: String? = null
    private var currentGameName: String? = null
    private var currentGameCover: String? = null

    private lateinit var comparisonContainer: LinearLayout
    private lateinit var resultText: TextView
    private lateinit var guessInput: AutoCompleteTextView
    private lateinit var guessButton: Button
    private lateinit var keywordsChipGroup: ChipGroup
    private lateinit var heartsContainer: LinearLayout

    private val hearts: MutableList<ImageView> = mutableListOf()
    private val maxLives = 5

    private lateinit var adapter: ArrayAdapter<String>
    private val allGames: MutableList<String> = mutableListOf()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_compare_game, container, false)

        // Initialize DBs and repository
        userDb = UserDatabase.getDatabase(requireContext())
        userDao = userDb.userDao()
        val gameDao = AppDatabase.getDatabase(requireContext()).gameDao()
        repository = GameRepository(gameDao, RetrofitClient.api, requireContext())

        // UI elements
        resultText = view.findViewById(R.id.resultText)
        guessInput = view.findViewById(R.id.guessInput)
        guessButton = view.findViewById(R.id.guessButton)
        keywordsChipGroup = view.findViewById(R.id.keywordsChipGroup)
        heartsContainer = view.findViewById(R.id.guessHeartsContainer)
        comparisonContainer = view.findViewById(R.id.comparisonContainer)

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
            currentGameId?.let { submitGuess(it, guess) }
                ?: Toast.makeText(requireContext(), "Game not loaded yet", Toast.LENGTH_SHORT).show()
        }

        return view
    }

    // Fetch all games
    private fun fetchAllGames() {
        lifecycleScope.launch(Dispatchers.IO) {
            val games = if (NetworkUtils.isOnline(requireContext())) {
                try {
                    repository.syncFromApi()
                    repository.getAllGames()
                } catch (_: Exception) {
                    repository.getAllGames()
                }
            } else {
                repository.getAllGames()
            }

            allGames.clear()
            allGames.addAll(games.map { it.name })
            withContext(Dispatchers.Main) {
                adapter.notifyDataSetChanged()
            }
        }
    }


    // Fetch random game
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
                    comparisonContainer.removeAllViews()
                }
            }
        }
    }


    // Submit guess
    private fun submitGuess(gameId: String, guess: String) {
        lifecycleScope.launch(Dispatchers.IO) {
            val response: ComparisonResponse? = try {
                // Online or offline via repository
                repository.compareGame(CompareRequest(gameId, guess))
            } catch (_: Exception) {
                // In case repository fails, fallback offline
                val game = repository.getGameByIdOfflineSafe(gameId)
                val matches = mutableMapOf<String, String>()
                game?.keywords?.forEach { keyword ->
                    matches[keyword] = if (keyword.equals(guess, ignoreCase = true)) "exact" else "partial"
                }
                ComparisonResponse(
                    correct = matches.any { it.value == "exact" },
                    matches = matches
                )
            }

            val matches = response?.matches ?: emptyMap()
            val correct = response?.correct ?: matches.any { it.value == "exact" }

            withContext(Dispatchers.Main) {
                processComparisonResult(matches, correct)
            }
        }
    }



    // Update UI with guess result
    private fun processComparisonResult(matches: Map<String, String>, correct: Boolean) {
        val card = layoutInflater.inflate(R.layout.item_guess_card, null)
        val guessTitle = card.findViewById<TextView>(R.id.guessTitle)
        val chipContainer = card.findViewById<FlexboxLayout>(R.id.chipContainer)

        guessTitle.text = "You guessed: ${guessInput.text}"

        for ((key, status) in matches) {
            val chipView = layoutInflater.inflate(R.layout.item_match_chip, null)
            val chip = chipView.findViewById<TextView>(R.id.matchChip)
            chip.text = key
            val color = when (status.lowercase()) {
                "exact" -> R.color.green
                "partial" -> R.color.orange
                else -> R.color.red
            }
            chip.backgroundTintList = ColorStateList.valueOf(resources.getColor(color, null))
            chipContainer.addView(chipView)
        }

        comparisonContainer.addView(card, 0)

        if (correct) {
            showEndGameDialog(true, currentGameName ?: "Unknown", currentGameCover)
        } else {
            loseHeart()
        }
    }


    // Heart management
    private fun loseHeart() {
        if (hearts.isNotEmpty()) {
            val lastHeart = hearts.removeAt(hearts.size - 1)
            lastHeart.visibility = View.INVISIBLE
        }
        if (hearts.isEmpty()) {
            showEndGameDialog(false, currentGameName ?: "Unknown", currentGameCover)
            guessButton.isEnabled = false
            guessInput.isEnabled = false
        }
    }

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
        chip.setChipBackgroundColorResource(R.color.purple_500)
        keywordsChipGroup.addView(chip)
    }


    // End game dialog and streak
    private fun showEndGameDialog(won: Boolean, gameName: String, coverUrl: String?) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_end_game, null)
        val imageView = dialogView.findViewById<ImageView>(R.id.gameCoverImage)
        val titleText = dialogView.findViewById<TextView>(R.id.dialogTitle)
        val nameText = dialogView.findViewById<TextView>(R.id.gameName)
        val playAgainBtn = dialogView.findViewById<Button>(R.id.playAgainButton)
        val mainMenuBtn = dialogView.findViewById<Button>(R.id.mainMenuButton)

        titleText.text = if (won) getString(R.string.congrats) else getString(R.string.failure)
        nameText.text = "The game was: $gameName"

        coverUrl?.let { Glide.with(this).load(it).into(imageView) }

        if (won) {
            lifecycleScope.launch(Dispatchers.IO) {
                val userId = getLoggedInUserId() ?: return@launch
                val user = userDao.getUser(userId) ?: return@launch

                if (!isToday(user.lastPlayedCG)) user.streakCG += 1
                if (user.streakCG > user.bestStreakCG) user.bestStreakCG = user.streakCG
                user.lastPlayedCG = System.currentTimeMillis()
                userDao.updateUser(user)
            }
        }

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
        comparisonContainer.removeAllViews()
        fetchRandomGame()
    }


    // Helpers
    private fun isToday(timestamp: Long): Boolean {
        if (timestamp == 0L) return false
        val cal = Calendar.getInstance().apply { timeInMillis = timestamp }
        val today = Calendar.getInstance()
        return cal.get(Calendar.YEAR) == today.get(Calendar.YEAR) &&
                cal.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR)
    }

    private fun getLoggedInUserId(): String? {
        val prefs = requireActivity().getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
        return prefs.getString("userId", null)
    }
}
