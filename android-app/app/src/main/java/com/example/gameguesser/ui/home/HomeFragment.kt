package com.example.gameguesser.ui.home

import android.content.Context
import android.content.Intent
import android.icu.util.Calendar
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.launch
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.example.gameguesser.databinding.FragmentHomeBinding
// import com.example.gameguesser.ui.keyWordGame.KeyGameFragment  // commented out - game guesser part
import androidx.navigation.fragment.findNavController
import com.example.gameguesser.DAOs.UserDao
import com.example.gameguesser.Database.UserDatabase
import com.example.gameguesser.LoginActivity
import com.example.gameguesser.R
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!
    private lateinit var userDb: UserDatabase
    private lateinit var userDao: UserDao
    private val compareGameStreakFlow = MutableStateFlow(0)
    private val keyWordStreakFlow = MutableStateFlow(0)


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val homeViewModel =
            ViewModelProvider(this).get(HomeViewModel::class.java)

        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        val root: View = binding.root

        // displays the name in the user label, felt cute might delete later
        val account = GoogleSignIn.getLastSignedInAccount(requireContext())
        account?.let {
            val displayName = it.displayName ?: "Player"
            binding.usernameText.text = displayName
        }
        // Initialize DB and DAO
        userDb = UserDatabase.getDatabase(requireContext())
        userDao = userDb.userDao()

        // Check streaks when the user reaches the home screen
        checkAllStreaksOnAppLoad()

        viewLifecycleOwner.lifecycleScope.launch {
            keyWordStreakFlow.collect { streakValue ->
                // This block runs on the main thread whenever keyWordStreakFlow is updated.
                binding.keyWordsStreak.text = getString(R.string.key_words_streak, streakValue)
            }

        }
        viewLifecycleOwner.lifecycleScope.launch {
            compareGameStreakFlow.collect { streakValue ->
                // This block runs on the main thread whenever keyWordStreakFlow is updated.
                binding.compareGame.text = getString(R.string.key_words_streak, streakValue)
            }

        }

        // Navigate to Key Game (active)
        binding.playKeyWordsButton.setOnClickListener {
            findNavController().navigate(R.id.keyGameFragment)
        }

        binding.playBonusGameButton.setOnClickListener {
            findNavController().navigate(R.id.compareGameFragment)
        }

        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun checkAllStreaksOnAppLoad() {
        lifecycleScope.launch(Dispatchers.IO) {
            val userId = getLoggedInUserId() ?: return@launch
            val user = userDao.getUser(userId) ?: return@launch

            var wasReset = false

            // Check Compare Game streak
            if (user.lastPlayedCG > 0L && !wasYesterdayOrToday(user.lastPlayedCG)) {
                user.streakCG = 0
                wasReset = true
            }

            // Check KeyWord Game streak
            if (user.lastPlayedKW > 0L && !wasYesterdayOrToday(user.lastPlayedKW)) {
                user.streakKW = 0
                wasReset = true
            }

            if (wasReset) {
                userDao.updateUser(user)
            }

            //adding values to stateflow
            keyWordStreakFlow.value = user.streakKW
            compareGameStreakFlow.value = user.streakCG



            // The withContext block for UI updates can be removed from here
            // as we will handle it in onCreateView.
        }
    }



    private fun wasYesterdayOrToday(timestamp: Long): Boolean {
        if (timestamp == 0L) return false

        val lastPlayedCal =
            Calendar.getInstance().apply { timeInMillis = timestamp }
        val todayCal = Calendar.getInstance()

        // Check if it's the same day
        if (lastPlayedCal.get(Calendar.YEAR) == todayCal.get(Calendar.YEAR) &&
            lastPlayedCal.get(Calendar.DAY_OF_YEAR) == todayCal.get(Calendar.DAY_OF_YEAR)) {
            return true
        }

        // Check if it was yesterday
        val yesterdayCal =
            Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }
        if (lastPlayedCal.get(Calendar.YEAR) == yesterdayCal.get(Calendar.YEAR) &&
            lastPlayedCal.get(Calendar.DAY_OF_YEAR) == yesterdayCal.get(Calendar.DAY_OF_YEAR)) {
            return true
        }

        return false
    }

    private fun getLoggedInUserId(): String? {
        val prefs = requireActivity().getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
        return prefs.getString("userId", null)
    }
}