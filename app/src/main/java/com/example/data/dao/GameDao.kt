package com.example.data.dao

import androidx.room.*
import com.example.data.entity.*
import kotlinx.coroutines.flow.Flow

@Dao
interface CampaignDao {
    @Query("SELECT * FROM campaigns ORDER BY createdTimestamp DESC")
    fun getAllCampaigns(): Flow<List<Campaign>>

    @Query("SELECT * FROM campaigns WHERE id = :id LIMIT 1")
    suspend fun getCampaignById(id: Int): Campaign?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCampaign(campaign: Campaign): Long

    @Update
    suspend fun updateCampaign(campaign: Campaign)

    @Delete
    suspend fun deleteCampaign(campaign: Campaign)
}

@Dao
interface GameCharacterDao {
    @Query("SELECT * FROM game_characters WHERE campaignId = :campaignId")
    fun getCharactersForCampaign(campaignId: Int): Flow<List<GameCharacter>>

    @Query("SELECT * FROM game_characters WHERE campaignId = :campaignId")
    suspend fun getCharactersForCampaignSnapshot(campaignId: Int): List<GameCharacter>

    @Query("SELECT * FROM game_characters WHERE id = :id LIMIT 1")
    suspend fun getCharacterById(id: Int): GameCharacter?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCharacter(character: GameCharacter): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllCharacters(characters: List<GameCharacter>)

    @Update
    suspend fun updateCharacter(character: GameCharacter)

    @Delete
    suspend fun deleteCharacter(character: GameCharacter)
}

@Dao
interface WorldFactDao {
    @Query("SELECT * FROM world_facts WHERE campaignId = :campaignId")
    fun getFactsForCampaign(campaignId: Int): Flow<List<WorldFact>>

    @Query("SELECT * FROM world_facts WHERE campaignId = :campaignId")
    suspend fun getFactsForCampaignSnapshot(campaignId: Int): List<WorldFact>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFact(fact: WorldFact): Long

    @Update
    suspend fun updateFact(fact: WorldFact)

    @Delete
    suspend fun deleteFact(fact: WorldFact)
}

@Dao
interface EventLogDao {
    @Query("SELECT * FROM event_logs WHERE campaignId = :campaignId ORDER BY timestamp ASC")
    fun getLogsForCampaign(campaignId: Int): Flow<List<EventLog>>

    @Query("SELECT * FROM event_logs WHERE campaignId = :campaignId ORDER BY timestamp ASC")
    suspend fun getLogsForCampaignSnapshot(campaignId: Int): List<EventLog>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: EventLog): Long

    @Query("DELETE FROM event_logs WHERE campaignId = :campaignId")
    suspend fun clearLogsForCampaign(campaignId: Int)
}
