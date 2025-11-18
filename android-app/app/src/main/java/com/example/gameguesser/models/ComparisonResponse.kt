package com.example.gameguesser.models

data class ComparisonResponse (
    val correct: Boolean,
    val matches: Map<String, String>
)
