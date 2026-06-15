package com.example.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "world_facts")
data class WorldFact(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val campaignId: Int,
    val category: String, // "NPC_DOSSIER", "PLOT_THREAD", "OPEN_LOOP", "GENERAL"
    val factKey: String,   // name of NPC, ID of thread, or loop name/time
    val factValue: String, // dossier text, thread description, or status value
    val isCompletedOrResolved: Boolean = false,
    val knownByEntityNames: String = "ALL" // comma-separated names of characters who know this fact, or "ALL"
)
