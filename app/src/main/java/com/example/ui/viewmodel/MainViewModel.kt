package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.database.AppDatabase
import com.example.data.entity.*
import com.example.data.repository.GameRepository
import com.example.rules.DMOrchestrator
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val repository = GameRepository(
        db.campaignDao(),
        db.gameCharacterDao(),
        db.worldFactDao(),
        db.eventLogDao()
    )
    private val orchestrator = DMOrchestrator(repository)

    // UI flows
    val campaigns: StateFlow<List<Campaign>> = repository.allCampaigns
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _selectedCampaignId = MutableStateFlow<Int?>(null)
    val selectedCampaignId: StateFlow<Int?> = _selectedCampaignId.asStateFlow()

    val currentCampaign: StateFlow<Campaign?> = _selectedCampaignId
        .flatMapLatest { id ->
            if (id == null) flowOf(null)
            else repository.allCampaigns.map { list -> list.find { it.id == id } }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val characters: StateFlow<List<GameCharacter>> = _selectedCampaignId
        .flatMapLatest { id ->
            if (id == null) flowOf(emptyList())
            else repository.getCharactersForCampaign(id)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val facts: StateFlow<List<WorldFact>> = _selectedCampaignId
        .flatMapLatest { id ->
            if (id == null) flowOf(emptyList())
            else repository.getFactsForCampaign(id)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val logs: StateFlow<List<EventLog>> = _selectedCampaignId
        .flatMapLatest { id ->
            if (id == null) flowOf(emptyList())
            else repository.getLogsForCampaign(id)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _isThinking = MutableStateFlow(false)
    val isThinking: StateFlow<Boolean> = _isThinking.asStateFlow()

    private val _selectedActorId = MutableStateFlow<Int?>(null)
    val selectedActorId: StateFlow<Int?> = _selectedActorId.asStateFlow()

    // Create a new campaign
    fun startCampaign(name: String, tone: String, difficulty: String) {
        viewModelScope.launch {
            val newCampaign = Campaign(
                name = name,
                description = "Quest in the gloomy lands.",
                tonePreset = tone,
                difficulty = difficulty
            )
            val campaignId = repository.insertCampaign(newCampaign).toInt()
            _selectedCampaignId.value = campaignId
            
            _isThinking.value = true
            try {
                orchestrator.initializeDungeonStarter(campaignId)
                // Select first player as default active actor
                val chars = repository.getCharactersSnapshot(campaignId).filter { !it.isMonster }
                if (chars.isNotEmpty()) {
                    _selectedActorId.value = chars.first().id
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isThinking.value = false
            }
        }
    }

    // Load an existing campaign
    fun selectCampaign(id: Int) {
        _selectedCampaignId.value = id
        viewModelScope.launch {
            val chars = repository.getCharactersSnapshot(id).filter { !it.isMonster }
            if (chars.isNotEmpty()) {
                _selectedActorId.value = chars.first().id
            }
        }
    }

    // Update campaign parameters (such as name, tone, difficulty) during drafting
    fun updateCampaign(campaign: Campaign) {
        viewModelScope.launch {
            repository.updateCampaign(campaign)
        }
    }

    // Commit dynamic character summaries and launch DM's opening narration
    fun embarkOnQuest() {
        val cid = _selectedCampaignId.value ?: return
        viewModelScope.launch {
            _isThinking.value = true
            try {
                orchestrator.commitOpeningNarration(cid)
                // Default active actor selection
                val chars = repository.getCharactersSnapshot(cid).filter { !it.isMonster }
                if (chars.isNotEmpty()) {
                    _selectedActorId.value = chars.first().id
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isThinking.value = false
            }
        }
    }

    // Set active acting character
    fun selectActiveActor(characterId: Int) {
        _selectedActorId.value = characterId
    }

    // Submit player custom input to sequence beat!
    fun submitPlayerAction(inputText: String) {
        val cid = _selectedCampaignId.value ?: return
        val aid = _selectedActorId.value ?: return
        if (inputText.isBlank()) return

        viewModelScope.launch {
            _isThinking.value = true
            try {
                orchestrator.executeBeat(cid, aid, inputText)
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isThinking.value = false
            }
        }
    }

    // Reset campaign data or logs
    fun resetCurrentCampaignLogs() {
        val cid = _selectedCampaignId.value ?: return
        viewModelScope.launch {
            repository.clearLogsForCampaign(cid)
            orchestrator.initializeDungeonStarter(cid)
        }
    }

    fun saveOrUpdateCharacter(character: GameCharacter) {
        viewModelScope.launch {
            if (character.id == 0) {
                val newId = repository.insertCharacter(character).toInt()
                if (!character.isMonster && _selectedActorId.value == null) {
                    _selectedActorId.value = newId
                }
            } else {
                repository.updateCharacter(character)
            }
        }
    }

    fun deleteCharacter(character: GameCharacter) {
        viewModelScope.launch {
            repository.deleteCharacter(character)
            val cid = _selectedCampaignId.value
            if (cid != null) {
                val remaining = repository.getCharactersSnapshot(cid).filter { !it.isMonster && it.id != character.id }
                if (remaining.isNotEmpty()) {
                    _selectedActorId.value = remaining.first().id
                } else {
                    _selectedActorId.value = null
                }
            }
        }
    }

    fun exitCampaign() {
        _selectedCampaignId.value = null
        _selectedActorId.value = null
        clearInitiative()
        clearLatestRoll()
    }

    // Load starting archetype heroes if party is empty
    fun loadPresetHeroes() {
        val cid = _selectedCampaignId.value ?: return
        viewModelScope.launch {
            _isThinking.value = true
            try {
                val currentChars = repository.getCharactersSnapshot(cid)
                val hasHeroes = currentChars.any { !it.isMonster }
                if (!hasHeroes) {
                    val player1 = GameCharacter(
                        campaignId = cid,
                        name = "Thorin",
                        characterClass = "Fighter",
                        isMonster = false,
                        level = 1,
                        strength = 16,
                        dexterity = 12,
                        constitution = 15,
                        intelligence = 9,
                        wisdom = 13,
                        charisma = 10,
                        maxHp = 14,
                        currentHp = 14,
                        armorClass = 16,
                        weaponsText = "Greatsword (1d12+3 slashing), Handaxe"
                    )
                    val player2 = GameCharacter(
                        campaignId = cid,
                        name = "Elysia",
                        characterClass = "Wizard",
                        isMonster = false,
                        level = 1,
                        strength = 8,
                        dexterity = 14,
                        constitution = 12,
                        intelligence = 16,
                        wisdom = 12,
                        charisma = 13,
                        maxHp = 7,
                        currentHp = 7,
                        armorClass = 12,
                        maxSpellSlots = 2,
                        currentSpellSlots = 2,
                        weaponsText = "Magic Missile (1d4+1 force per dart), Quarterstaff"
                    )
                    val id1 = repository.insertCharacter(player1).toInt()
                    repository.insertCharacter(player2)
                    _selectedActorId.value = id1
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isThinking.value = false
            }
        }
    }

    // Pull or draw a thematic starting rumor fact during drafting
    fun drawRandomTavernRumor() {
        val cid = _selectedCampaignId.value ?: return
        viewModelScope.launch {
            _isThinking.value = true
            try {
                val rumors = listOf(
                    "A traveling bard claims the stolen scepter is stored in a reinforced iron chest behind Orc Chief Kragor's throne.",
                    "The goblin patrols carry heavy iron keys that unlock pressure-plate dart traps in the deeper tunnels.",
                    "A hidden brick wall in the eastern caves is marked with a carved dragon skull. Pressing its eye opens a cache of healing potions.",
                    "The goblins are fiercely territorial, but terrified of open fire; a flaming torch or light spell can weaken their resolve.",
                    "Whispers say a friendly goblin prisoner named Bob is kept in the slave pins, willing to guide heroes in exchange for freedom.",
                    "Local woodcutters warn that the cave entrance is guarded by a sleeping sentinel beast that wakes if loud noises are made."
                )
                val currentFacts = repository.getFactsForCampaign(cid).firstOrNull() ?: emptyList()
                val unusedRumors = rumors.filter { r -> currentFacts.none { it.factValue == r } }
                val rumorText = if (unusedRumors.isNotEmpty()) unusedRumors.random() else rumors.random()

                val newFact = WorldFact(
                    campaignId = cid,
                    category = "RUMOR",
                    factKey = "rumor_" + System.currentTimeMillis(),
                    factValue = rumorText,
                    isCompletedOrResolved = false
                )
                repository.insertFact(newFact)
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isThinking.value = false
            }
        }
    }

    // --- TASK 1: REAL-TIME DICE TRAY SIMULATOR ---
    private val _latestRoll = MutableStateFlow<DiceRollState?>(null)
    val latestRoll: StateFlow<DiceRollState?> = _latestRoll.asStateFlow()

    fun clearLatestRoll() {
        _latestRoll.value = null
    }

    fun rollDiceFromTray(sides: Int, modifierType: String) {
        val cid = _selectedCampaignId.value ?: return
        viewModelScope.launch {
            val actorId = _selectedActorId.value
            val charactersList = repository.getCharactersSnapshot(cid)
            val character = charactersList.find { it.id == actorId }
            
            val charName = character?.name ?: "System Explorer"
            val statScore = if (character != null) {
                when (modifierType.uppercase()) {
                    "STR" -> character.strength
                    "DEX" -> character.dexterity
                    "CON" -> character.constitution
                    "INT" -> character.intelligence
                    "WIS" -> character.wisdom
                    "CHA" -> character.charisma
                    else -> 10
                }
            } else 10

            val modValue = if (modifierType == "none" || character == null) 0 else (statScore - 10) / 2
            val naturalRoll = kotlin.random.Random.nextInt(1, sides + 1)
            val totalRoll = naturalRoll + modValue

            val rollState = DiceRollState(
                sides = sides,
                naturalRoll = naturalRoll,
                modifierVal = modValue,
                modifierLabel = modifierType,
                total = totalRoll,
                characterName = charName
            )
            _latestRoll.value = rollState

            // Construct an custom EventLog and insert it into the logging database stream
            val logMessageText = "$charName rolled a physical d$sides from the dice tray (using $modifierType modifier)!"
            val specRollResult = "🎲 d$sides Result: [$naturalRoll] + modifier ($modValue) = $totalRoll"
            
            repository.insertLog(
                EventLog(
                    campaignId = cid,
                    senderName = charName,
                    messageType = "ROLL",
                    messageText = logMessageText,
                    diceRollResultText = specRollResult
                )
            )
        }
    }

    // --- TASK 2: ACTIVE COMBAT TURN CHRONOMETER & INITIATIVE TRACKER ---
    private val _initiativeList = MutableStateFlow<List<InitiativeMember>>(emptyList())
    val initiativeList: StateFlow<List<InitiativeMember>> = _initiativeList.asStateFlow()

    private val _currentTurnIndex = MutableStateFlow(0)
    val currentTurnIndex: StateFlow<Int> = _currentTurnIndex.asStateFlow()

    fun rollInitiative() {
        val cid = _selectedCampaignId.value ?: return
        viewModelScope.launch {
            val charactersList = repository.getCharactersSnapshot(cid).filter { !it.isDead() }
            if (charactersList.isEmpty()) return@launch

            val members = charactersList.map { char ->
                val dexMod = (char.dexterity - 10) / 2
                val d20Roll = kotlin.random.Random.nextInt(1, 21)
                val totalInitiative = d20Roll + dexMod + char.initiativeBonus
                InitiativeMember(
                    characterId = char.id,
                    name = char.name,
                    isMonster = char.isMonster,
                    initiativeRoll = totalInitiative,
                    ac = char.armorClass,
                    maxHp = char.maxHp,
                    currentHp = char.currentHp
                )
            }.sortedWith(
                compareByDescending<InitiativeMember> { it.initiativeRoll }
                    .thenBy { it.isMonster } // players go first on initiative ties
            )

            _initiativeList.value = members
            _currentTurnIndex.value = 0

            // Auto-select active actor to match first player in initiative list
            val firstPlayer = members.firstOrNull { !it.isMonster }
            if (firstPlayer != null) {
                _selectedActorId.value = firstPlayer.characterId
            }

            // Write initiative summary log directly into the campaign
            val chronologyList = members.joinToString(" ➔ ") { "${it.name} (${it.initiativeRoll})" }
            repository.insertLog(
                EventLog(
                    campaignId = cid,
                    senderName = "System",
                    messageType = "SYSTEM_CHECK",
                    messageText = "⚔️ Initiative has been rolled for the combat scene! Chronology order: $chronologyList. Combat has officially begun!"
                )
            )
        }
    }

    fun nextTurn() {
        val cid = _selectedCampaignId.value ?: return
        val list = _initiativeList.value
        if (list.isEmpty()) return

        val nextIndex = (_currentTurnIndex.value + 1) % list.size
        _currentTurnIndex.value = nextIndex

        val currentActor = list[nextIndex]
        if (!currentActor.isMonster) {
            _selectedActorId.value = currentActor.characterId
        }

        viewModelScope.launch {
            repository.insertLog(
                EventLog(
                    campaignId = cid,
                    senderName = "System",
                    messageType = "SYSTEM_CHECK",
                    messageText = "⌛ Turn shifts to **${currentActor.name}**! Initiative sequence slot: ${nextIndex + 1}/${list.size}."
                )
            )
        }
    }

    fun clearInitiative() {
        _initiativeList.value = emptyList()
        _currentTurnIndex.value = 0
    }
}

// Data class representation for real-time manual dice-cup simulations
data class DiceRollState(
    val sides: Int,
    val naturalRoll: Int,
    val modifierVal: Int,
    val modifierLabel: String,
    val total: Int,
    val characterName: String
)

// Data class representation for turn-ordered combatants
data class InitiativeMember(
    val characterId: Int,
    val name: String,
    val isMonster: Boolean,
    val initiativeRoll: Int,
    val ac: Int,
    val maxHp: Int,
    val currentHp: Int
)

