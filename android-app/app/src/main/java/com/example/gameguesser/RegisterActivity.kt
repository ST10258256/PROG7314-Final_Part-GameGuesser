package com.example.gameguesser

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.gameguesser.Class.LocalUser
import com.example.gameguesser.Database.UserDatabase
import com.example.gameguesser.data.PasswordUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class RegisterActivity : AppCompatActivity() {

    private lateinit var etUsername: EditText
    private lateinit var etEmail: EditText
    private lateinit var etPassword: EditText
    private lateinit var etConfirmPassword: EditText
    private lateinit var btnCreateAccount: Button
    private lateinit var btnCancel: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        etUsername = findViewById(R.id.etUsername)
        etEmail = findViewById(R.id.etEmail)
        etPassword = findViewById(R.id.etPassword)
        etConfirmPassword = findViewById(R.id.etConfirmPassword)
        btnCreateAccount = findViewById(R.id.btnCreateAccount)
        btnCancel = findViewById(R.id.btnCancel)

        btnCreateAccount.setOnClickListener { registerUser() }

        btnCancel.setOnClickListener {
            finish() // Return to login screen
        }
    }

    private fun registerUser() {
        val username = etUsername.text.toString().trim()
        val email = etEmail.text.toString().trim()
        val password = etPassword.text.toString()
        val confirmPassword = etConfirmPassword.text.toString()

        if (username.isEmpty() || email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
            Toast.makeText(this, "Please fill in all fields.", Toast.LENGTH_SHORT).show()
            return
        }

        if (password != confirmPassword) {
            Toast.makeText(this, "Passwords do not match.", Toast.LENGTH_SHORT).show()
            return
        }

        val hashedPassword = PasswordUtils.hash(password)

        val user = LocalUser(
            email = email,
            userName = username,
            passwordHash = hashedPassword
        )

        val db = UserDatabase.getDatabase(this)
        val userDao = db.localUserDao()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                userDao.registerUser(user)
                runOnUiThread {
                    Toast.makeText(this@RegisterActivity, "Account created!", Toast.LENGTH_SHORT).show()
                    finish() // Go back to login
                }
            } catch (e: Exception) {
                runOnUiThread {
                    Toast.makeText(
                        this@RegisterActivity,
                        "Email already registered.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }
}
