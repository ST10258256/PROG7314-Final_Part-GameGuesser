package com.example.gameguesser.ui.encyclopedia

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.gameguesser.Adapters.GameAdapter
import com.example.gameguesser.Database.AppDatabase
import com.example.gameguesser.GameDetail
import com.example.gameguesser.R
import com.example.gameguesser.data.RetrofitClient
import com.example.gameguesser.data.Game
import com.example.gameguesser.repository.GameRepository
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class EncyclopediaFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: GameAdapter
    private var allGames = listOf<Game>()

    // filter options
    private val genreOptions = listOf("All", "Action", "Action-Adventure", "Action‑RPG","Battle Royale", "Card Battle", "Co-op Adventure", "Fashion", "Fighting", "First-Person Shooter", "Fitness", "Life Simulation", "MOBA","Metroidvania","RPG","Racing","Real Time Tactics","Retro","Roguelike","Sandbox","Soulslike","Sports","Stealth","Strategy","Survival Horror", "Third‑Person Shooter","Tower Defense","Trivia","Other")
    private val platformOptions = listOf("All", "PC", "PlayStation 2", "PlayStation 3", "PlayStation 4", "PlayStation 5", "Xbox 360", "Xbox Series X/S","Mac","Wii","3DS","iOS","Android","macOS","Windows", "Nintendo Switch", "Linux", "Xbox One","Nintendo DS","DS","Game Boy Advance","GameCube","Wii U","Switch","IOS Mobile","Xbox","PlayStation","OS X","PlayStation Vita","Mac OS X")
    private val povOptions = listOf("All", "First-person", "Third-person", "Top-down", "Side-scroll", "Isometric", "2.5D", "2D", "Dual Perspective", "Multiple","Various")

    // current filter/search state
    private var currentGenre = "All"
    private var currentPlatform = "All"
    private var currentPOV = "All"
    private var currentYear = "All"
    private var currentSearch = ""

    // debounce job for showing the toast only after changes settle
    private var toastJob: Job? = null
    private var currentToast: Toast? = null
    private val toastDebounceMs = 400L

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_encyclopedia, container, false)

        recyclerView = view.findViewById(R.id.recyclerViewGames)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        adapter = GameAdapter(allGames) { selectedGame ->
            Log.d("GameSelected", "Game: ${selectedGame.name}, id: ${selectedGame.id}")
            val intent = Intent(requireContext(), GameDetail::class.java)
            intent.putExtra("GAME_ID", selectedGame.id)
            startActivity(intent)
        }
        recyclerView.adapter = adapter

        // SearchView
        val searchView = view.findViewById<SearchView>(R.id.searchViewGames)
        searchView.queryHint = "Search games..."
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                currentSearch = query?.trim() ?: ""
                applyFiltersAndSearch()
                searchView.clearFocus()
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                currentSearch = newText?.trim() ?: ""
                applyFiltersAndSearch()
                return true
            }
        })
        searchView.setOnCloseListener {
            currentSearch = ""
            applyFiltersAndSearch()
            false
        }

        view.findViewById<FloatingActionButton>(R.id.fabFilter).setOnClickListener {
            showFilterBottomSheet()
        }

        fetchGames()

        return view
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Cancel any pending toast job and active toast when view is destroyed
        toastJob?.cancel()
        currentToast?.cancel()
    }

    private fun fetchGames() {
        val dao = AppDatabase.getDatabase(requireContext()).gameDao()
        val repository = GameRepository(dao, RetrofitClient.api)

        lifecycleScope.launch {
            try {
                allGames = repository.getAllGames()
                adapter.updateGames(allGames)
                Log.d("GAME_FETCH", "Loaded ${allGames.size} games")
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Failed to load games", Toast.LENGTH_SHORT).show()
                Log.e("GAME_FETCH", "Error fetching games", e)
            }
        }
    }

    private fun showFilterBottomSheet() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_filter, null)
        val dialog = BottomSheetDialog(requireContext())
        dialog.setContentView(dialogView)

        val spinnerGenre = dialogView.findViewById<Spinner>(R.id.spinnerGenre)
        val spinnerPlatform = dialogView.findViewById<Spinner>(R.id.spinnerPlatform)
        val spinnerPOV = dialogView.findViewById<Spinner>(R.id.spinnerPOV)
        val spinnerYear = dialogView.findViewById<Spinner>(R.id.spinnerYear)
        val buttonApply = dialogView.findViewById<Button>(R.id.btnApplyFilters)

        spinnerGenre.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, genreOptions)
        spinnerPlatform.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, platformOptions)
        spinnerPOV.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, povOptions)

        // build year options from allGames (if available)
        val years = mutableListOf("All") + allGames.mapNotNull { it.releaseYear?.toString() }.distinct().sortedDescending()
        spinnerYear.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, years)

        // pre-select current filter values if they exist
        spinnerGenre.setSelection(genreOptions.indexOf(currentGenre).coerceAtLeast(0))
        spinnerPlatform.setSelection(platformOptions.indexOf(currentPlatform).coerceAtLeast(0))
        spinnerPOV.setSelection(povOptions.indexOf(currentPOV).coerceAtLeast(0))
        spinnerYear.setSelection(years.indexOf(currentYear).coerceAtLeast(0))

        buttonApply.setOnClickListener {
            val selectedGenre = spinnerGenre.selectedItem.toString()
            val selectedPlatform = spinnerPlatform.selectedItem.toString()
            val selectedPOV = spinnerPOV.selectedItem.toString()
            val selectedYear = spinnerYear.selectedItem.toString()
            applyFilter(selectedGenre, selectedPlatform, selectedPOV, selectedYear)
            dialog.dismiss()
        }

        dialog.show()
    }

    // Update current filter selection and re-apply combined filter + search
    private fun applyFilter(genre: String, platform: String, pov: String, year: String) {
        currentGenre = genre
        currentPlatform = platform
        currentPOV = pov
        currentYear = year
        applyFiltersAndSearch()
    }

    // Core: apply both filters and the search query to allGames and update adapter
    private fun applyFiltersAndSearch() {
        val filtered = allGames.filter { game ->
            val genreMatch = currentGenre == "All" || (game.genre?.equals(currentGenre, ignoreCase = true) ?: false)
            val povMatch = currentPOV == "All" || (game.pov?.equals(currentPOV, ignoreCase = true) ?: false)
            val platformMatch = currentPlatform == "All" || (game.platforms?.any { it.equals(currentPlatform, ignoreCase = true) } ?: false)
            val yearMatch = currentYear == "All" || (game.releaseYear?.toString() == currentYear)

            // search: check name, maybe also description or developer if available
            val query = currentSearch.trim()
            val searchMatch = if (query.isEmpty()) {
                true
            } else {
                val lower = query.lowercase()
                val nameMatches = game.name?.lowercase()?.contains(lower) ?: false
                val descMatches = (game.description?.lowercase()?.contains(lower) ?: false)
                val genreMatches = game.genre?.lowercase()?.contains(lower) ?: false
                val platformMatches = game.platforms?.any { it.lowercase().contains(lower) } ?: false
                nameMatches || descMatches || genreMatches || platformMatches
            }

            genreMatch && povMatch && platformMatch && yearMatch && searchMatch
        }

        adapter.updateGames(filtered)

        // Debounce showing the toast so it only appears once after user stops changing filters/search
        toastJob?.cancel()
        // Cancel any visible toast so toasts don't stack
        currentToast?.cancel()
        toastJob = lifecycleScope.launch {
            delay(toastDebounceMs)
            currentToast = Toast.makeText(requireContext(), "Showing ${filtered.size} result(s)", Toast.LENGTH_SHORT)
            currentToast?.show()
        }
    }
}
