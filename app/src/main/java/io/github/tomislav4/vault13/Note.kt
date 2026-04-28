package io.github.tomislav4.vault13

import java.util.UUID

data class Note(
    val id: String = UUID.randomUUID().toString(),
    val content: String,
    val timestamp: Long = System.currentTimeMillis()
)
