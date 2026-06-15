package com.example.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "event_logs")
data class EventLog(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val campaignId: Int,
    val timestamp: Long = System.currentTimeMillis(),
    val senderName: String, // "DM", player name, NPC name, "System"
    val messageType: String, // "NARRATION", "DIALOGUE", "ACTION", "SYSTEM_CHECK", "ROLL", "WHISPER"
    val messageText: String,
    val diceRollResultText: String? = null, // e.g. "D20(14) + 3 = 17"
    val targetCharacterName: String? = null // if it's a private whisper
)
