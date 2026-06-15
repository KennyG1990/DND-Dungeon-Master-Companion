package com.example.data.repository

import com.example.data.dao.*
import com.example.data.entity.*
import kotlinx.coroutines.flow.Flow

class GameRepository(
    val campaignDao: CampaignDao,
    val gameCharacterDao: GameCharacterDao,
    val worldFactDao: WorldFactDao,
    val eventLogDao: EventLogDao
) {
    val allCampaigns: Flow<List<Campaign>> = campaignDao.getAllCampaigns()

    suspend fun getCampaignById(id: Int): Campaign? = campaignDao.getCampaignById(id)
    suspend fun insertCampaign(campaign: Campaign): Long = campaignDao.insertCampaign(campaign)
    suspend fun updateCampaign(campaign: Campaign) = campaignDao.updateCampaign(campaign)
    suspend fun deleteCampaign(campaign: Campaign) = campaignDao.deleteCampaign(campaign)

    fun getCharactersForCampaign(campaignId: Int): Flow<List<GameCharacter>> =
        gameCharacterDao.getCharactersForCampaign(campaignId)

    suspend fun getCharactersSnapshot(campaignId: Int): List<GameCharacter> =
        gameCharacterDao.getCharactersForCampaignSnapshot(campaignId)

    suspend fun getCharacterById(id: Int): GameCharacter? = gameCharacterDao.getCharacterById(id)
    suspend fun insertCharacter(character: GameCharacter): Long = gameCharacterDao.insertCharacter(character)
    suspend fun insertAllCharacters(characters: List<GameCharacter>) = gameCharacterDao.insertAllCharacters(characters)
    suspend fun updateCharacter(character: GameCharacter) = gameCharacterDao.updateCharacter(character)
    suspend fun deleteCharacter(character: GameCharacter) = gameCharacterDao.deleteCharacter(character)

    fun getFactsForCampaign(campaignId: Int): Flow<List<WorldFact>> =
        worldFactDao.getFactsForCampaign(campaignId)

    suspend fun getFactsSnapshot(campaignId: Int): List<WorldFact> =
        worldFactDao.getFactsForCampaignSnapshot(campaignId)

    suspend fun insertFact(fact: WorldFact): Long = worldFactDao.insertFact(fact)
    suspend fun updateFact(fact: WorldFact) = worldFactDao.updateFact(fact)
    suspend fun deleteFact(fact: WorldFact) = worldFactDao.deleteFact(fact)

    fun getLogsForCampaign(campaignId: Int): Flow<List<EventLog>> =
        eventLogDao.getLogsForCampaign(campaignId)

    suspend fun getLogsSnapshot(campaignId: Int): List<EventLog> =
        eventLogDao.getLogsForCampaignSnapshot(campaignId)

    suspend fun insertLog(log: EventLog): Long = eventLogDao.insertLog(log)
    suspend fun clearLogsForCampaign(campaignId: Int) = eventLogDao.clearLogsForCampaign(campaignId)
}
