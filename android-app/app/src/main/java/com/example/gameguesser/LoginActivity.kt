package com.example.gameguesser

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Looper
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.gameguesser.Class.User
import com.example.gameguesser.Database.UserDatabase
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.SignInButton
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.tasks.Task
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import android.os.Handler

class LoginActivity : AppCompatActivity() {

    private lateinit var googleSignInClient: GoogleSignInClient
    private lateinit var signInLauncher: ActivityResultLauncher<Intent>

    // privacy policy url
    private val privacyUrl = "https://www.freeprivacypolicy.com/live/4fe88b6c-922e-4134-9e89-c1fbf239b51e"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        // --- show privacy policy clickable link ---
        val privacyTv = findViewById<TextView>(R.id.privacyLink)
        privacyTv.setOnClickListener {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(privacyUrl))
            startActivity(intent)
        }

        // Require fingerprint before showing / using the login flow
        // If biometrics unavailable -> show toast and continue
        if (!requestBiometricIfAvailable()) {
            Toast.makeText(this, "Biometric authentication not available — continuing to login.", Toast.LENGTH_LONG).show()
        }

        //Change to true if there is an issue with logging
        val devBypass = false
        if (devBypass) {
            Toast.makeText(this, "Bypassing login (DEV MODE)", Toast.LENGTH_SHORT).show()
            goToMainActivity(null)
            return
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // setup for sign in
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()  // only setup for basic email
            .build()

        googleSignInClient = GoogleSignIn.getClient(this, gso)

        // checks if they have already signed in
        val account = GoogleSignIn.getLastSignedInAccount(this)

        //used this to test the offline mode, works
        //val account: GoogleSignInAccount? = null

        if (account != null) {
            // User still has a valid Google session
            goToMainActivity(account)
        } else {
            // check SharedPreferences for offline mode
            val prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE)
            val savedUserId = prefs.getString("userId", null)
            val savedUserName = prefs.getString("userName", null)

            if (savedUserId != null) {
                // User logged in before, allow offline access
                Toast.makeText(this, "Welcome back $savedUserName (offline)", Toast.LENGTH_SHORT).show()
                Handler(Looper.getMainLooper()).postDelayed({
                    goToMainActivity(null)
                }, 500)
            }
        }

        val btnGoogleSignIn = findViewById<SignInButton>(R.id.btnGoogleSignIn)
        btnGoogleSignIn.setOnClickListener {
            signIn() // runs the sign in if not already signed in
        }

        signInLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            handleSignInResult(task)
        }
    }

    private fun requestBiometricIfAvailable(): Boolean {
        val biometricManager = BiometricManager.from(this)
        val canAuthenticate = biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG)
        when (canAuthenticate) {
            BiometricManager.BIOMETRIC_SUCCESS -> {
                showBiometricPrompt()
                return true
            }
            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE,
            BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE,
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> {
                // Not available or not set up
                return false
            }
            else -> return false
        }
    }

    private fun showBiometricPrompt() {
        val executor = ContextCompat.getMainExecutor(this)
        val biometricPrompt = BiometricPrompt(this, executor, object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(result)
                runOnUiThread {
                    Toast.makeText(applicationContext, "Fingerprint recognised", Toast.LENGTH_SHORT).show()
                    // Authentication successful — continue to login flow (nothing else needed)
                }
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                super.onAuthenticationError(errorCode, errString)
                runOnUiThread {
                    Toast.makeText(applicationContext, "Authentication error: $errString", Toast.LENGTH_SHORT).show()
                    // If user cancels or too many failures, we'll exit the app:
                    finish()
                }
            }

            override fun onAuthenticationFailed() {
                super.onAuthenticationFailed()
                runOnUiThread {
                    Toast.makeText(applicationContext, "Fingerprint not recognized", Toast.LENGTH_SHORT).show()
                }
            }
        })

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Fingerprint Required")
            .setSubtitle("Scan your fingerprint to access GameGuesser")
            .setNegativeButtonText("Exit")
            .build()

        biometricPrompt.authenticate(promptInfo)
    }

    private fun signIn() {
        // get the intent from the Google client and launch the sign-in flow
        val signInIntent = googleSignInClient.signInIntent
        signInLauncher.launch(signInIntent)
    }

    private fun handleSignInResult(completedTask: Task<GoogleSignInAccount>) {
        try {
            val account = completedTask.getResult(ApiException::class.java)
            if (account != null) {
                // saves them to shared pref for offline
                val prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE)
                with(prefs.edit()) {
                    putString("userName", account.displayName)
                    putString("userEmail", account.email)
                    putString("userId", account.id)
                    apply()
                }

                // optionally save to Room (uncomment if needed)
                /*
                val user = User(
                    userId = account.id ?: "",
                    userName = account.displayName ?: "Player",
                    streak = 0
                )
                val db = UserDatabase.getDatabase(this)
                CoroutineScope(Dispatchers.IO).launch {
                    db.userDao().addUser(user)
                }
                */

                // welcome msg
                Toast.makeText(this, "Welcome ${account.displayName}", Toast.LENGTH_SHORT).show()

                // goes to main
                goToMainActivity(account)
            }
        } catch (e: ApiException) {
            Toast.makeText(this, "Sign-in failed: ${e.statusCode}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun goToMainActivity(account: GoogleSignInAccount?) {
        // flow to main, can't go back to login, needs to logout
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()
    }
}
