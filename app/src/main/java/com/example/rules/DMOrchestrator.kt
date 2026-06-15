package com.example.rules

import android.util.Log
import com.example.api.*
import com.example.data.entity.*
import com.example.data.repository.GameRepository
import kotlinx.coroutines.flow.firstOrNull
import kotlin.random.Random

class DMOrchestrator(private val repository: GameRepository) {

    companion object {
        private const val TAG = "DMOrchestrator"
    }

    // Helper to get modifier for a skill
    fun getSkillModifier(character: GameCharacter, skillName: String): Int {
        val attrScore = when (skillName.lowercase().trim()) {
            "athletics" -> character.strength
            "acrobatics", "sleight of hand", "stealth" -> character.dexterity
            "arcana", "history", "investigation", "nature", "religion" -> character.intelligence
            "animal handling", "insight", "medicine", "perception", "survival" -> character.wisdom
            "deception", "intimidation", "performance", "persuasion" -> character.charisma
            // Attribute direct checks
            "strength" -> character.strength
            "dexterity" -> character.dexterity
            "constitution" -> character.constitution
            "intelligence" -> character.intelligence
            "wisdom" -> character.wisdom
            "charisma" -> character.charisma
            else -> character.wisdom // default to wisdom/perception
        }
        return character.getModifier(attrScore)
    }

    // Initialize standard starter campaign if empty
    suspend fun initializeDungeonStarter(campaignId: Int) {
        val chars = repository.getCharactersSnapshot(campaignId)
        if (chars.isEmpty()) {
            // Create Heroic Players
            val player1 = GameCharacter(
                campaignId = campaignId,
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
                campaignId = campaignId,
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
            repository.insertCharacter(player1)
            repository.insertCharacter(player2)

            // Create dungeon boss and monsters
            val goblin1 = GameCharacter(
                campaignId = campaignId,
                name = "Grumble the Goblin",
                characterClass = "Goblin",
                isMonster = true,
                strength = 8,
                dexterity = 14,
                constitution = 10,
                maxHp = 7,
                currentHp = 7,
                armorClass = 12,
                weaponsText = "Scimitar (1d6+2 slashing)"
            )
            val goblin2 = GameCharacter(
                campaignId = campaignId,
                name = "Slasher the Goblin",
                characterClass = "Goblin",
                isMonster = true,
                strength = 8,
                dexterity = 14,
                constitution = 10,
                maxHp = 7,
                currentHp = 7,
                armorClass = 12,
                weaponsText = "Shortbow (1d6+2 piercing)"
            )
            val boss = GameCharacter(
                campaignId = campaignId,
                name = "Kragor the Orc Chief",
                characterClass = "Orc Boss",
                isMonster = true,
                level = 2,
                strength = 17,
                dexterity = 12,
                constitution = 16,
                maxHp = 22,
                currentHp = 22,
                armorClass = 14,
                weaponsText = "Greataxe (1d12+3 slashing, savage attack)"
            )
            repository.insertCharacter(goblin1)
            repository.insertCharacter(goblin2)
            repository.insertCharacter(boss)

            // Starter Plot threads
            val thread1 = WorldFact(
                campaignId = campaignId,
                category = "PLOT_THREAD",
                factKey = "the_goblin_ambush",
                factValue = "The party has tracked a pack of goblins to their hideout cave to recover the stolen royal scepter.",
                isCompletedOrResolved = false
            )
            val thread2 = WorldFact(
                campaignId = campaignId,
                category = "OPEN_LOOP",
                factKey = "scepter_restitution",
                factValue = "Recover the royal scepter from Orc Chief Kragor. Reward promised: 100 gold coins.",
                isCompletedOrResolved = false
            )
            val detailFact = WorldFact(
                campaignId = campaignId,
                category = "GENERAL",
                factKey = "cave_lighting",
                factValue = "The cave entrance is dark, with damp moss covering the walls and a faint smell of sulfur.",
                isCompletedOrResolved = false
            )

            repository.insertFact(thread1)
            repository.insertFact(thread2)
            repository.insertFact(detailFact)
        }
    }

    // Insert the DM's opening narration based on the dynamic party forged by players
    suspend fun commitOpeningNarration(campaignId: Int) {
        val players = repository.getCharactersSnapshot(campaignId).filter { !it.isMonster }
        val playerSummary = if (players.isNotEmpty()) {
            players.joinToString(" and ") { "${it.name} (${it.characterClass})" }
        } else {
            "your party of brave adventurers"
        }
        
        repository.insertLog(
            EventLog(
                campaignId = campaignId,
                senderName = "System",
                messageType = "SYSTEM_CHECK",
                messageText = "The campaign has officially begun! Your party ($playerSummary) stands assembled before the dangerous cavern entrance."
            )
        )
        repository.insertLog(
            EventLog(
                campaignId = campaignId,
                senderName = "DM",
                messageType = "NARRATION",
                messageText = "The damp silence of the ravine is broken only by the crackle of your torches. The dark mouth of the cave yawns before you, O brave heroes. Mossy stone walls drip with moisture, and a faint odor of sulfur floats out from the gloom. Somewhere inside lies the stolen royal scepter. Ready your weapons! What do you do?"
            )
        )
    }

    // Process a single timeline beat from a player's raw intent statement
    suspend fun executeBeat(
        campaignId: Int,
        activeCharacterId: Int,
        rawPlayerInputText: String
    ): String {
        val campaign = repository.getCampaignById(campaignId) ?: return "Campaign not found"
        val activeChar = repository.getCharacterById(activeCharacterId) ?: return "Active Character not found"
        
        // Log player's declared intent statement
        repository.insertLog(
            EventLog(
                campaignId = campaignId,
                senderName = activeChar.name,
                messageType = "ACTION",
                messageText = rawPlayerInputText
            )
        )

        // 1. Snapshot ledger resources
        val allActors = repository.getCharactersSnapshot(campaignId)
        val activeFacts = repository.getFactsSnapshot(campaignId)
        val logs = repository.getLogsSnapshot(campaignId)

        // Compile summaries for Prompt context inclusion
        val liveActorsSummary = allActors.filter { !it.isDead() }.joinToString("; ") { 
            "${it.name} (${it.characterClass}, HP:${it.currentHp}/${it.maxHp}, ${if (it.isMonster) "Monster" else "Player"})" 
        }
        val factsSummary = activeFacts.filter { !it.isCompletedOrResolved }.joinToString("; ") { 
            "[${it.category}] ${it.factKey}: ${it.factValue}" 
        }
        val recentLogsText = logs.takeLast(10).joinToString("\n") { 
            "${it.senderName} (${it.messageType}): ${it.messageText}" 
        }

        // 2. PARSE via Gemini
        val sceneContext = activeFacts.firstOrNull { it.factKey == "cave_lighting" }?.factValue ?: "Cave interior"
        val parsedAction = GeminiClient.parsePlayerAction(
            playerText = rawPlayerInputText,
            actorName = activeChar.name,
            sceneContext = sceneContext,
            allActorsSummary = liveActorsSummary
        )

        Log.d(TAG, "Parsed action intent: $parsedAction")

        var computedResolutionText = ""
        var diceRollResult: String? = null

        // 3. ADJUDICATE via rules engine (the single source of truth!)
        when (parsedAction.actionType) {
            "ATTACK" -> {
                // Find matching target
                val targets = allActors.filter { 
                    it.name.contains(parsedAction.targetName, ignoreCase = true) || 
                    it.characterClass.contains(parsedAction.targetName, ignoreCase = true)
                }
                val target = targets.firstOrNull { it.isMonster != activeChar.isMonster && !it.isDead() } ?: allActors.firstOrNull { it.isMonster != activeChar.isMonster && !it.isDead() }

                if (target != null) {
                    val weapon = if (parsedAction.parameterDetail.isNotEmpty()) parsedAction.parameterDetail else "Greatsword"
                    val isRange = weapon.lowercase().contains("bow") || weapon.lowercase().contains("arrow")
                    val mod = if (isRange) activeChar.getModifier(activeChar.dexterity) else activeChar.getModifier(activeChar.strength)
                    
                    val damageFormula = if (weapon.lowercase().contains("sword")) "1d12" else if (weapon.lowercase().contains("bow")) "1d6" else "1d8"
                    val finalDamageFormula = "$damageFormula+$mod"

                    val outcome = RulesEngine.resolveAttack(
                        attackerName = activeChar.name,
                        attackModifier = mod + activeChar.level,
                        damageFormula = finalDamageFormula,
                        targetName = target.name,
                        targetAc = target.armorClass
                    )

                    computedResolutionText = outcome.detailText
                    diceRollResult = "Roll: ${outcome.totalRoll} vs AC ${target.armorClass}"

                    if (outcome.isHit && outcome.damageDealt > 0) {
                        val updatedHp = (target.currentHp - outcome.damageDealt).coerceAtLeast(0)
                        val conditions = if (updatedHp <= 0) "Dead, Unconscious" else target.conditionsText
                        repository.updateCharacter(target.copy(currentHp = updatedHp, conditionsText = conditions))
                    }
                } else {
                    computedResolutionText = "No hostile target matches '${parsedAction.targetName}'! Swing and missed empty air."
                }
            }
            "CAST_SPELL" -> {
                // Simple rules resolution for Magic Missile / spells
                val spellName = parsedAction.parameterDetail
                if (activeChar.maxSpellSlots > 0 && activeChar.currentSpellSlots <= 0) {
                    computedResolutionText = "${activeChar.name} tried to cast $spellName but lacks sufficient remaining spell slots!"
                } else {
                    val targets = allActors.filter { it.isMonster != activeChar.isMonster && !it.isDead() }
                    val target = targets.firstOrNull()
                    
                    if (target != null) {
                        val damageFormula = "1d4+1"
                        val rollResult = RulesEngine.rollFormula(damageFormula)
                        val totalDamage = rollResult.total
                        
                        // Apply cast mechanics
                        val updatedSlots = (activeChar.currentSpellSlots - 1).coerceAtLeast(0)
                        repository.updateCharacter(activeChar.copy(currentSpellSlots = updatedSlots))

                        val targetHp = (target.currentHp - totalDamage).coerceAtLeast(0)
                        val conditions = if (targetHp <= 0) "Dead, Unconscious" else target.conditionsText
                        repository.updateCharacter(target.copy(currentHp = targetHp, conditionsText = conditions))

                        computedResolutionText = "${activeChar.name} casts Magic Missile at ${target.name}. Damage rolled: $totalDamage force damage (${rollResult.textDetail}). target current HP remains ${targetHp}/${target.maxHp}."
                    } else {
                        computedResolutionText = "Casted $spellName but no targets remain visible."
                    }
                }
            }
            "MOVE" -> {
                val dir = if (parsedAction.parameterDetail.isNotEmpty()) parsedAction.parameterDetail else "forward"
                computedResolutionText = "${activeChar.name} moves $dir into the zone."
            }
            "DIALOGUE" -> {
                computedResolutionText = "${activeChar.name} says aloud: \"${parsedAction.spokenDialogue}\""
            }
            else -> {
                // Creative logic check via DM advisory
                val ruling = GeminiClient.requestCreativeRuling(
                    declaredAction = rawPlayerInputText,
                    actorName = activeChar.name,
                    dossiersText = factsSummary,
                    activeSceneFacts = sceneContext
                )

                val mod = getSkillModifier(activeChar, ruling.skillType)
                val checkResult = RulesEngine.resolveCheck(
                    characterName = activeChar.name,
                    modifier = mod,
                    dc = ruling.difficultyClass,
                    checkType = ruling.skillType
                )

                computedResolutionText = "DM Ruling Context: ${ruling.promptNarration}\nSystem Outcome: ${checkResult.detailText}"
                diceRollResult = "Roll: ${checkResult.totalRoll} vs DC ${ruling.difficultyClass}"

                // If success climbing or sneaking, update position location or facts slightly
                if (checkResult.isSuccess && ruling.skillType.lowercase() == "stealth") {
                    repository.updateCharacter(activeChar.copy(location = "Shadows of Cavern"))
                }
            }
        }

        // Write mechanical resolution system check
        repository.insertLog(
            EventLog(
                campaignId = campaignId,
                senderName = "System",
                messageType = "SYSTEM_CHECK",
                messageText = computedResolutionText,
                diceRollResultText = diceRollResult
            )
        )

        // 4. NARRATE outcome via Gemini
        val combinedOutput = "ACTION: $rawPlayerInputText\nMECHANICS RESOLVED: $computedResolutionText"
        val dmNarrationText = GeminiClient.generateResultNarration(
            campaignName = campaign.name,
            tone = campaign.tonePreset,
            contextScene = factsSummary,
            outcomeText = combinedOutput,
            recentLogs = recentLogsText
        )

        // Commit narrative text back to logs
        repository.insertLog(
            EventLog(
                campaignId = campaignId,
                senderName = "DM",
                messageType = "NARRATION",
                messageText = dmNarrationText
            )
        )

        // Auto-resolve any dead NPCs or Monster drops or automatic registration of named items
        val liveStateActors = repository.getCharactersSnapshot(campaignId)
        val monsters = liveStateActors.filter { it.isMonster }
        if (monsters.isNotEmpty() && monsters.all { it.isDead() }) {
            // End combat victory loop, write to facts
            val completionLogs = "All goblins and threats have been defeated!"
            repository.insertLog(
                EventLog(
                    campaignId = campaignId,
                    senderName = "System",
                    messageType = "SYSTEM_CHECK",
                    messageText = completionLogs
                )
            )
            repository.insertLog(
                EventLog(
                    campaignId = campaignId,
                    senderName = "DM",
                    messageType = "NARRATION",
                    messageText = "Silence falls over the cave chamber. The primary threat has been resolved, and glints of metallic gold catch your eyes near the Kragor chest. The scepter has been secured."
                )
            )
            val scepterFact = activeFacts.firstOrNull { it.factKey == "scepter_restitution" }
            if (scepterFact != null && !scepterFact.isCompletedOrResolved) {
                repository.updateFact(scepterFact.copy(isCompletedOrResolved = true, factValue = "Stolen royal scepter has been retrieved from the cold hands of Kragor! Returning it is all that is left."))
            }
        }

        return dmNarrationText
    }
}
