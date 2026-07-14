package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "transcript_notes")
data class TranscriptNote(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val rawText: String,
    val summary: String = "",
    val keyTakeawaysJson: String = "", // Stored as a simple newline-separated string
    val durationSeconds: Long = 0L,
    val timestamp: Long = System.currentTimeMillis(),
    val isSynced: Boolean = false,
    val lastModified: Long = System.currentTimeMillis()
) {
    fun getKeyTakeawaysList(): List<String> {
        if (keyTakeawaysJson.isBlank()) return emptyList()
        return keyTakeawaysJson.split("\n").map { it.trim() }.filter { it.isNotEmpty() }
    }
}
