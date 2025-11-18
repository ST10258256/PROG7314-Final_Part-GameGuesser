package com.example.gameguesser.models

import com.example.gameguesser.data.Game

data class RandomGameResponse(
    val id: String,
    val name: String,
    val keywords: List<String>,
    val coverImageUrl: String?
)