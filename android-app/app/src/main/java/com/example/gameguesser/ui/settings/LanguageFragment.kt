package com.example.gameguesser.ui.settings

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.gameguesser.databinding.FragmentLanguageBinding
import java.util.Locale

class LanguagesFragment : Fragment() {

    private var _binding: FragmentLanguageBinding? = null
    private val binding get() = _binding!!

    // Map checkbox names to Android locale codes
    private val languageMap = mapOf(
        "English" to "en",
        "Afrikaans" to "af",
        "Zulu" to "zu",
        "French" to "fr",
        "Spanish" to "es",
        "German" to "de"
    )

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLanguageBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Load saved language and pre-check the corresponding checkbox
        val prefs = requireContext().getSharedPreferences("settings", Context.MODE_PRIVATE)
        val savedLangCode = prefs.getString("app_language", "en")
        when (savedLangCode) {
            "en" -> binding.cbEnglish.isChecked = true
            "af" -> binding.cbAfrikaans.isChecked = true
            "zu" -> binding.cbZulu.isChecked = true
            "fr" -> binding.cbFrench.isChecked = true
            "es" -> binding.cbSpanish.isChecked = true
            "de" -> binding.cbGerman.isChecked = true
        }

        binding.btnSaveLanguages.setOnClickListener {
            // Only allow one language selection
            val selectedLanguage = when {
                binding.cbEnglish.isChecked -> "English"
                binding.cbAfrikaans.isChecked -> "Afrikaans"
                binding.cbZulu.isChecked -> "Zulu"
                binding.cbFrench.isChecked -> "French"
                binding.cbSpanish.isChecked -> "Spanish"
                binding.cbGerman.isChecked -> "German"
                else -> "English" // default
            }

            val selectedCode = languageMap[selectedLanguage] ?: "en"

            // Save the language to SharedPreferences
            prefs.edit().putString("app_language", selectedCode).apply()

            // Apply the new locale
            setAppLocale(requireContext(), selectedCode)

            // Restart the activity to reflect the change
            activity?.recreate()

            Toast.makeText(
                requireContext(),
                "Language switched to $selectedLanguage",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    // Helper function to change app locale
    private fun setAppLocale(context: Context, languageCode: String) {
        val locale = Locale(languageCode)
        Locale.setDefault(locale)
        val config = context.resources.configuration
        config.setLocale(locale)
        context.resources.updateConfiguration(config, context.resources.displayMetrics)
    }
}
