package com.example.gameguesser.ui.settings

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CompoundButton
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

        val prefs = requireContext().getSharedPreferences("settings", Context.MODE_PRIVATE)
        val savedLangCode = prefs.getString("app_language", "en")

        // Map checkbox references to language codes
        val checkBoxes = mapOf(
            binding.cbEnglish to "en",
            binding.cbAfrikaans to "af",
            binding.cbZulu to "zu",
            binding.cbFrench to "fr",
            binding.cbSpanish to "es",
            binding.cbGerman to "de"
        )

        // Pre-check saved language
        checkBoxes.forEach { (cb, code) ->
            cb.isChecked = savedLangCode == code
        }

        // Ensure only one checkbox can be selected at a time
        checkBoxes.keys.forEach { cb ->
            cb.setOnCheckedChangeListener { buttonView: CompoundButton, isChecked: Boolean ->
                if (isChecked) {
                    checkBoxes.keys.filter { it != buttonView }.forEach { it.isChecked = false }
                }
            }
        }

        binding.btnSaveLanguages.setOnClickListener {
            // Find the selected language
            val selectedCode = checkBoxes.entries.firstOrNull { it.key.isChecked }?.value ?: "en"
            val selectedLanguage = languageMap.entries.first { it.value == selectedCode }.key

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
