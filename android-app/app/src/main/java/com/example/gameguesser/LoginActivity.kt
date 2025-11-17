package com.example.gameguesser

import android.content.Intent
import android.os.Bundle
import android.os.Looper
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.os.postDelayed
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.gameguesser.Class.User
import com.example.gameguesser.Class.LocalUser
import com.example.gameguesser.Database.UserDatabase
import com.example.gameguesser.data.PasswordUtils
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

class LoginActivity : AppCompatActivity() {

    private lateinit var googleSignInClient: GoogleSignInClient
    private lateinit var signInLauncher: ActivityResultLauncher<Intent>

    private lateinit var emailField: EditText
    private lateinit var passwordField: EditText
    private lateinit var btnEmailLogin: Button
    private lateinit var btnRegister: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

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

        // GOOGLE SIGN-IN SETUP
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
                android.os.Handler(Looper.getMainLooper()).postDelayed({
                    goToMainActivity(null)
                }, 500)
            }
        }

        // Google sign-in button
        val btnGoogleSignIn = findViewById<SignInButton>(R.id.btnGoogleSignIn)
        btnGoogleSignIn.setOnClickListener {
            signIn()
        }

        signInLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            handleSignInResult(task)
        }

        // EMAIL LOGIN UI
        emailField = findViewById(R.id.etEmail)
        passwordField = findViewById(R.id.etPassword)
        btnEmailLogin = findViewById(R.id.btnEmailLogin)
        btnRegister = findViewById(R.id.btnRegister)

        btnEmailLogin.setOnClickListener {
            loginWithEmail()
        }

        btnRegister.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
    }

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
                CoroutineScope(Dispatchers.IO).launch {
                    db.userDao().addUser(user)
                }

                Toast.makeText(this, "Welcome ${account.displayName}", Toast.LENGTH_SHORT).show()
                goToMainActivity(account)
            }
        } catch (e: ApiException) {
            Toast.makeText(this, "Sign-in failed: ${e.statusCode}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun loginWithEmail() {
        val email = emailField.text.toString().trim()
        val password = passwordField.text.toString().trim()

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please enter both email and password", Toast.LENGTH_SHORT).show()
            return
        }

        val db = UserDatabase.getDatabase(this)

        CoroutineScope(Dispatchers.IO).launch {
            // Use localUserDao (where Register stored LocalUser)
            val localUser = db.localUserDao().getUser(email)

            runOnUiThread {
                if (localUser == null) {
                    Toast.makeText(this@LoginActivity, "Account not found", Toast.LENGTH_SHORT).show()
                    return@runOnUiThread
                }

                val hashedInput = PasswordUtils.hash(password)

                if (hashedInput != localUser.passwordHash) {
                    Toast.makeText(this@LoginActivity, "Incorrect password", Toast.LENGTH_SHORT).show()
                } else {
                    // Save prefs for offline welcome like Google flow
                    val prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE)
                    with(prefs.edit()) {
                        putString("userName", localUser.userName)
                        putString("userEmail", localUser.email)
                        putString("userId", localUser.email) // using email as id for local users
                        apply()
                    }

                    Toast.makeText(this@LoginActivity, "Welcome back ${localUser.userName}", Toast.LENGTH_SHORT).show()
                    goToMainActivity(null)
                }
            }
        }
    }

    private fun goToMainActivity(account: GoogleSignInAccount?) {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()
    }
}
