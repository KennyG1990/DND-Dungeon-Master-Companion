package com.example.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "campaigns")
data class Campaign(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val description: String,
    val createdTimestamp: Long = System.currentTimeMillis(),
    val dungeonMasterName: String = "Narrator",
    val tonePreset: String = "heroic", // grim, heroic, comedic
    val difficulty: String = "normal",   // easy, normal, hard
    val status: String = "active"       // active, paused, completed
)
