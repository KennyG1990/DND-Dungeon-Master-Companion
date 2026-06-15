package com.example.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "game_characters")
data class GameCharacter(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val campaignId: Int,
    val name: String,
    val characterClass: String, // e.g. "Fighter", "Wizard", "Cleric", "Rogue", "Goblin", "Dragon"
    val isMonster: Boolean = false,
    val level: Int = 1,
    
    // Stats / Attributes
    val strength: Int = 10,
    val dexterity: Int = 10,
    val constitution: Int = 10,
    val intelligence: Int = 10,
    val wisdom: Int = 10,
    val charisma: Int = 10,
    
    // Mechanics
    val maxHp: Int = 10,
    val currentHp: Int = 10,
    val armorClass: Int = 10,
    val initiativeBonus: Int = 0,
    val speed: Int = 30, // feet per turn
    
    // Spell Slots
    val maxSpellSlots: Int = 0,
    val currentSpellSlots: Int = 0,
    
    // Equipped items and Inventory
    val weaponsText: String = "", // e.g. "Longsword (+5 to hit, 1d8+3 slashing)"
    val equipmentText: String = "Leather Armor, Backpack",
    
    // Status text
    val conditionsText: String = "", // comma-separated e.g. "poisoned,prone" or empty
    
    // Positions
    val location: String = "Entrance", // descriptive scene location
    val positionX: Int = 0,
    val positionY: Int = 0
) {
    fun getModifier(score: Int): Int {
        return (score - 10) / 2
    }
    
    fun isDead(): Boolean {
        return currentHp <= 0 || conditionsText.contains("dead", ignoreCase = true)
    }
}
