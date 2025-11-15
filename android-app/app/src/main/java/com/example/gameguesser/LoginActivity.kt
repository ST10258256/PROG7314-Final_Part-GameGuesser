package com.example.gameguesser

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
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
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {

    private lateinit var googleSignInClient: GoogleSignInClient
    private lateinit var signInLauncher: ActivityResultLauncher<Intent>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        // 🔒 FIRST STEP → Require fingerprint before accessing login page
        authenticateBiometric()

        // -------------------- Your login code continues --------------------
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

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(this, gso)

        val account = GoogleSignIn.getLastSignedInAccount(this)

        if (account != null) {
            goToMainActivity(account)
        } else {
            val prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE)
            val savedUserId = prefs.getString("userId", null)
            val savedUserName = prefs.getString("userName", null)

            if (savedUserId != null) {
                Toast.makeText(this, "Welcome back $savedUserName (offline)", Toast.LENGTH_SHORT).show()
                Handler(Looper.getMainLooper()).postDelayed({
                    goToMainActivity(null)
                }, 500)
            }
        }

        val btnGoogleSignIn = findViewById<SignInButton>(R.id.btnGoogleSignIn)
        btnGoogleSignIn.setOnClickListener { signIn() }

        signInLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            handleSignInResult(task)
        }
    }

    // ---------------- ⭐️ BIOMETRIC AUTHENTICATION CODE ⭐️ ----------------
    private fun authenticateBiometric() {
        val biometricManager = BiometricManager.from(this)

        when (biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG)) {
            BiometricManager.BIOMETRIC_SUCCESS -> {
                // OK
            }
            else -> {
                Toast.makeText(this, "Biometric authentication not available", Toast.LENGTH_LONG).show()
                finish()
                return
            }
        }

        val executor = ContextCompat.getMainExecutor(this)

        val biometricPrompt = BiometricPrompt(this, executor,
            object : BiometricPrompt.AuthenticationCallback() {

                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    // allow user to continue
                    Toast.makeText(applicationContext, "Authenticated", Toast.LENGTH_SHORT).show()
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    Toast.makeText(applicationContext, "Fingerprint not recognized", Toast.LENGTH_SHORT).show()
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    Toast.makeText(applicationContext, "Authentication error: $errString", Toast.LENGTH_SHORT).show()
                    finish() // close app
                }
            })

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Fingerprint Required")
            .setSubtitle("Scan your fingerprint to access GameGuesser")
            .setNegativeButtonText("Exit")
            .build()

        biometricPrompt.authenticate(promptInfo)
    }

    // ---------------- END BIOMETRIC ----------------


    private fun signIn() {
        signInLauncher.launch(googleSignInClient.signInIntent)
    }

    private fun handleSignInResult(completedTask: Task<GoogleSignInAccount>) {
        try {
            val account = completedTask.getResult(ApiException::class.java)
            if (account != null) {
                val prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE)
                with(prefs.edit()) {
                    putString("userName", account.displayName)
                    putString("userEmail", account.email)
                    putString("userId", account.id)
                    apply()
                }

                val user = User(
                    userId = account.id ?: "",
                    userName = account.displayName ?: "Player",
                    streak = 0
                )

                val db = UserDatabase.getDatabase(this)
                kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                    db.userDao().addUser(user)
                }

                Toast.makeText(this, "Welcome ${account.displayName}", Toast.LENGTH_SHORT).show()
                goToMainActivity(account)
            }
        } catch (e: ApiException) {
            Toast.makeText(this, "Sign-in failed: ${e.statusCode}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun goToMainActivity(account: GoogleSignInAccount?) {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}
