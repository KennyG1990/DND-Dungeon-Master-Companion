package com.example.api

import android.util.Log
import com.example.BuildConfig
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object GeminiClient {
    private const val TAG = "GeminiClient"
    private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/"
    private const val MODEL = "gemini-3.5-flash"

    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    // Base request to generate content from system instruction and user prompt
    suspend fun generateText(
        prompt: String,
        systemInstruction: String = ""
    ): String = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            Log.e(TAG, "API Key is missing or default!")
            return@withContext "Error: Gemini API Key is missing. Please set it in the Secrets panel."
        }

        val url = "${BASE_URL}models/$MODEL:generateContent?key=$apiKey"

        try {
            // Build request JSON using JSONObject
            val contentObj = JSONObject().apply {
                put("parts", JSONArray().apply {
                    put(JSONObject().apply { put("text", prompt) })
                })
            }

            val rootObj = JSONObject().apply {
                put("contents", JSONArray().apply { put(contentObj) })
                
                if (systemInstruction.isNotEmpty()) {
                    put("systemInstruction", JSONObject().apply {
                        put("parts", JSONArray().apply {
                            put(JSONObject().apply { put("text", systemInstruction) })
                        })
                    })
                }
                
                // Config
                put("generationConfig", JSONObject().apply {
                    put("temperature", 0.6) 
                })
            }

            val body = rootObj.toString().toRequestBody("application/json".toMediaType())
            val request = Request.Builder()
                .url(url)
                .post(body)
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    val errBody = response.body?.string() ?: ""
                    Log.e(TAG, "API error response: $errBody")
                    return@withContext "Error: API call failed with code ${response.code}\n$errBody"
                }

                val resText = response.body?.string() ?: return@withContext "Error: Empty response"
                val jsonRes = JSONObject(resText)
                val candidates = jsonRes.optJSONArray("candidates")
                if (candidates != null && candidates.length() > 0) {
                    val candidate = candidates.getJSONObject(0)
                    val content = candidate.optJSONObject("content")
                    val parts = content?.optJSONArray("parts")
                    if (parts != null && parts.length() > 0) {
                        return@withContext parts.getJSONObject(0).optString("text") ?: "No text returned"
                    }
                }
                return@withContext "Error: Failed to parse Gemini response text."
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception during API call", e)
            return@withContext "Error: ${e.message}"
        }
    }

    // Parses a player raw statement into a structured RPG action proposal
    suspend fun parsePlayerAction(
        playerText: String,
        actorName: String,
        sceneContext: String,
        allActorsSummary: String
    ): ParsedActionInfo {
        val systemPrompt = """
            You are a D&D 5e assistant parser. Your job is to parse a player's natural language action into a structured JSON proposal.
            Return ONLY a valid JSON object matching the schema below. No other introductory/concluding text, no ```json formatting.
            
            JSON schema:
            {
               "action_type": "ATTACK" | "CAST_SPELL" | "USE_ITEM" | "SKILL_CHECK" | "MOVE" | "DIALOGUE" | "UNKNOWN",
               "target_name": "Name of target character/monster/object, or empty string",
               "parameter_detail": "Details (weapon name, spell name, item name, skill name like Stealth/Perception/Athletics, or direction/distance)",
               "spoken_dialogue": "Extract quote spoken by the character, or empty string",
               "is_combat_intent": true | false
            }
            
            Context clues:
            - Player Character Name: $actorName
            - Active Entities/Monsters around: $allActorsSummary
            - Active Scene Detail: $sceneContext
        """.trimIndent()

        val prompt = "Parse the following statement from $actorName: \"$playerText\""
        
        try {
            val response = generateText(prompt, systemPrompt)
            val jsonString = response.trim().removeSurrounding("```json", "```").trim()
            val json = JSONObject(jsonString)
            return ParsedActionInfo(
                actionType = json.optString("action_type", "UNKNOWN"),
                targetName = json.optString("target_name", ""),
                parameterDetail = json.optString("parameter_detail", ""),
                spokenDialogue = json.optString("spoken_dialogue", ""),
                isCombatIntent = json.optBoolean("is_combat_intent", false)
            )
        } catch (e: Exception) {
            Log.e(TAG, "parsePlayerAction failed, defaulting", e)
            // Simple heuristic mapping
            val cleanStr = playerText.lowercase()
            val isDialogue = cleanStr.contains("\"") || cleanStr.startsWith("say") || cleanStr.startsWith("speak") || cleanStr.startsWith("tell")
            val isAttack = cleanStr.contains("attack") || cleanStr.contains("hit") || cleanStr.contains("shoot") || cleanStr.contains("slash") || cleanStr.contains("cast fire")
            return ParsedActionInfo(
                actionType = if (isAttack) "ATTACK" else if (isDialogue) "DIALOGUE" else "SKILL_CHECK",
                targetName = "",
                parameterDetail = if (isDialogue) "" else "general",
                spokenDialogue = if (isDialogue) playerText else "",
                isCombatIntent = isAttack
            )
        }
    }

    // Resolves a creative skill check when the rules engine doesn't have a direct code translation
    suspend fun requestCreativeRuling(
        declaredAction: String,
        actorName: String,
        dossiersText: String,
        activeSceneFacts: String
    ): CreativeRuling {
        val systemPrompt = """
            You are the Dungeon Master (D&D 5e). The player wants to perform an action that has no direct hardcoded mechanical formula (e.g. lockpicking, convincing a guard, leaping chasms, sneaking).
            Synthesize an appropriate D&D 5e ability check.
            Your output MUST be a JSON object ONLY, formatted as follows without further comments:
            {
              "skill_type": "Acrobatics" | "Animal Handling" | "Arcana" | "Athletics" | "Deception" | "History" | "Insight" | "Intimidation" | "Investigation" | "Medicine" | "Nature" | "Perception" | "Performance" | "Persuasion" | "Religion" | "Sleight of Hand" | "Stealth" | "Survival" | "Strength" | "Dexterity" | "Constitution" | "Intelligence" | "Wisdom" | "Charisma",
              "difficulty_class": Int, (usually between 10 and 20. 10 is Easy, 15 is Moderate, 20 is Hard)
              "prompt_narration": "A Flavorful description of what they are attempting, ending right at the moment of suspense before they know if they succeed."
            }
            Keep context in mind:
            - Player: $actorName
            - Scene details & facts: $activeSceneFacts
            - NPCs details: $dossiersText
        """.trimIndent()

        try {
            val response = generateText(declaredAction, systemPrompt)
            val jsonString = response.trim().removeSurrounding("```json", "```").trim()
            val json = JSONObject(jsonString)
            return CreativeRuling(
                skillType = json.optString("skill_type", "Perception"),
                difficultyClass = json.optInt("difficulty_class", 12),
                promptNarration = json.optString("prompt_narration", "You attempt to proceed.")
            )
        } catch (e: Exception) {
            Log.e(TAG, "requestCreativeRuling failed, defaulting", e)
            return CreativeRuling("Perception", 12, "You attempt to proceed carefully.")
        }
    }

    // Generates a cinematic narration of a resolved mechanical event
    suspend fun generateResultNarration(
        campaignName: String,
        tone: String,
        contextScene: String,
        outcomeText: String,
        recentLogs: String
    ): String {
        val systemPrompt = """
            You are the Dungeon Master (D&D 5e) narrating an ongoing session.
            Tone format: $tone.
            Use evocative, high-immersion description. Integrate rolls, success/failures, HP changes, and status updates seamlessly.
            Rule: DO NOT invent dynamic number stats or change mechanics. If the outcome says "Hit for 6 damage", describe 6 damage exactly. 
            Keep descriptions short but captivating (3 sentences max). No preamble or explanations outside of the narrative prose.
            
            Game context:
            - Campaign: $campaignName
            - Current Scene: $contextScene
            - Historical log snippet: $recentLogs
        """.trimIndent()

        val prompt = "Rules Engine committed output to narrate visually:\n$outcomeText"
        return generateText(prompt, systemPrompt)
    }
}

data class ParsedActionInfo(
    val actionType: String,
    val targetName: String,
    val parameterDetail: String,
    val spokenDialogue: String,
    val isCombatIntent: Boolean
)

data class CreativeRuling(
    val skillType: String,
    val difficultyClass: Int,
    val promptNarration: String
)
