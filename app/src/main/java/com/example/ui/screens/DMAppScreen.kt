package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.entity.Campaign
import com.example.data.entity.EventLog
import com.example.data.entity.GameCharacter
import com.example.data.entity.WorldFact
import com.example.ui.viewmodel.MainViewModel
import com.example.ui.viewmodel.DiceRollState
import com.example.ui.viewmodel.InitiativeMember
import kotlinx.coroutines.launch
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.window.Dialog

// Custom Obsidian-Crimson-Gold RPG Color Scheme
val ObsidianDark = Color(0xFF0C0A0F)
val SlatePanel = Color(0xFF14121A)
val GoldAccent = Color(0xFFD4AF37)
val Goldenrod = Color(0xFFFFD700)
val DarkCrimson = Color(0xFF8B0000)
val CrimsonAccent = Color(0xFFFF2E2E)
val TextLight = Color(0xFFECEFF1)
val TextMuted = Color(0xFF90A4AE)
val EmeraldGreen = Color(0xFF2E7D32)

@Composable
fun DMAppScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val campaigns by viewModel.campaigns.collectAsState()
    val selectedCampaignId by viewModel.selectedCampaignId.collectAsState()
    val currentCampaign by viewModel.currentCampaign.collectAsState()
    val characters by viewModel.characters.collectAsState()
    val facts by viewModel.facts.collectAsState()
    val logs by viewModel.logs.collectAsState()
    val isThinking by viewModel.isThinking.collectAsState()
    val selectedActorId by viewModel.selectedActorId.collectAsState()
    val latestRoll by viewModel.latestRoll.collectAsState()
    val initiativeList by viewModel.initiativeList.collectAsState()
    val currentTurnIndex by viewModel.currentTurnIndex.collectAsState()

    Surface(
        modifier = modifier.fillMaxSize(),
        color = ObsidianDark
    ) {
        if (selectedCampaignId == null) {
            LobbyScreen(
                campaigns = campaigns,
                onStartCampaign = { name, tone, diff -> viewModel.startCampaign(name, tone, diff) },
                onSelectCampaign = { id -> viewModel.selectCampaign(id) }
            )
        } else {
            if (logs.isEmpty()) {
                GatheringRoomScreen(
                    campaign = currentCampaign,
                    characters = characters,
                    facts = facts,
                    isThinking = isThinking,
                    onUpdateCampaign = { updatedCampaign -> viewModel.updateCampaign(updatedCampaign) },
                    onSaveCharacter = { char -> viewModel.saveOrUpdateCharacter(char) },
                    onDeleteCharacter = { char -> viewModel.deleteCharacter(char) },
                    onEmbark = { viewModel.embarkOnQuest() },
                    onExit = { viewModel.exitCampaign() },
                    onLoadPresets = { viewModel.loadPresetHeroes() },
                    onDrawRumor = { viewModel.drawRandomTavernRumor() }
                )
            } else {
                PlayScreen(
                    campaign = currentCampaign,
                    characters = characters,
                    facts = facts,
                    logs = logs,
                    isThinking = isThinking,
                    selectedActorId = selectedActorId,
                    onSelectActor = { id -> viewModel.selectActiveActor(id) },
                    onSubmitAction = { action -> viewModel.submitPlayerAction(action) },
                    onResetLogs = { viewModel.resetCurrentCampaignLogs() },
                    onExit = { viewModel.exitCampaign() },
                    onSaveCharacter = { char -> viewModel.saveOrUpdateCharacter(char) },
                    onDeleteCharacter = { char -> viewModel.deleteCharacter(char) },
                    latestRoll = latestRoll,
                    onRollDice = { sides, modifierType -> viewModel.rollDiceFromTray(sides, modifierType) },
                    onClearLatestRoll = { viewModel.clearLatestRoll() },
                    initiativeList = initiativeList,
                    currentTurnIndex = currentTurnIndex,
                    onRollInitiative = { viewModel.rollInitiative() },
                    onNextTurn = { viewModel.nextTurn() },
                    onClearInitiative = { viewModel.clearInitiative() }
                )
            }
        }
    }
}

@Composable
fun LobbyScreen(
    campaigns: List<Campaign>,
    onStartCampaign: (String, String, String) -> Unit,
    onSelectCampaign: (Int) -> Unit
) {
    var newCampaignName by remember { mutableStateOf("Quest for the Crypt") }
    var selectedTone by remember { mutableStateOf("heroic") }
    var selectedDifficulty by remember { mutableStateOf("normal") }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        contentPadding = PaddingValues(vertical = 32.dp)
    ) {
        item {
            // Stylized RPG Gothic Title
            Text(
                text = "۩ AUTONOMOUS DM ۩",
                color = GoldAccent,
                fontSize = 32.sp,
                fontWeight = FontWeight.ExtraBold,
                fontFamily = FontFamily.Serif,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            Text(
                text = "Autonomous D&D Dungeon Master Engine",
                color = TextMuted,
                fontSize = 14.sp,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.padding(bottom = 32.dp)
            )
        }

        item {
            // Start campaign form Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                colors = CardDefaults.cardColors(containerColor = SlatePanel)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp)
                ) {
                    Text(
                        text = "Forge a New Campaign",
                        color = GoldAccent,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Serif,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    OutlinedTextField(
                        value = newCampaignName,
                        onValueChange = { newCampaignName = it },
                        label = { Text("Campaign Name", color = TextMuted) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = GoldAccent,
                            unfocusedBorderColor = TextMuted,
                            focusedTextColor = TextLight,
                            unfocusedTextColor = TextLight
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp)
                            .testTag("campaign_name_input")
                    )

                    // Tone Preset Selector
                    Text("Tone Preset", color = TextLight, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 6.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        listOf("heroic", "grim", "comedic").forEach { tone ->
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(horizontal = 4.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (selectedTone == tone) DarkCrimson else SlatePanel)
                                    .border(1.dp, if (selectedTone == tone) GoldAccent else TextMuted, RoundedCornerShape(8.dp))
                                    .clickable { selectedTone = tone }
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    tone.uppercase(),
                                    color = if (selectedTone == tone) Goldenrod else TextLight,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    // Difficulty selector
                    Text("Session Difficulty", color = TextLight, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 6.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 24.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        listOf("easy", "normal", "hard").forEach { diff ->
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(horizontal = 4.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (selectedDifficulty == diff) DarkCrimson else SlatePanel)
                                    .border(1.dp, if (selectedDifficulty == diff) GoldAccent else TextMuted, RoundedCornerShape(8.dp))
                                    .clickable { selectedDifficulty = diff }
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    diff.uppercase(),
                                    color = if (selectedDifficulty == diff) Goldenrod else TextLight,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    // Embark button
                    Button(
                        onClick = {
                            if (newCampaignName.isNotBlank()) {
                                onStartCampaign(newCampaignName, selectedTone, selectedDifficulty)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = GoldAccent),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("start_campaign_button")
                    ) {
                        Text(
                            "EMBARK ON QUEST",
                            color = ObsidianDark,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.ExtraBold,
                            fontFamily = FontFamily.Serif
                        )
                    }
                }
            }
        }

        if (campaigns.isNotEmpty()) {
            item {
                Spacer(modifier = Modifier.height(24.dp))
                HorizontalDivider(color = SlatePanel, thickness = 2.dp)
                Spacer(modifier = Modifier.height(16.dp))
                // Existing Campaigns Header
                Text(
                    text = "Resume Campaign Sessions",
                    color = GoldAccent,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Serif,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
            }

            items(campaigns) { campaign ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp)
                        .clickable { onSelectCampaign(campaign.id) },
                    colors = CardDefaults.cardColors(containerColor = SlatePanel),
                    border = BorderStroke(1.dp, DarkCrimson)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = campaign.name,
                                color = TextLight,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Tone: ${campaign.tonePreset.uppercase()} | Difficulty: ${campaign.difficulty.uppercase()}",
                                color = TextMuted,
                                fontSize = 11.sp
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Resume",
                            tint = GoldAccent
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PlayScreen(
    campaign: Campaign?,
    characters: List<GameCharacter>,
    facts: List<WorldFact>,
    logs: List<EventLog>,
    isThinking: Boolean,
    selectedActorId: Int?,
    onSelectActor: (Int) -> Unit,
    onSubmitAction: (String) -> Unit,
    onResetLogs: () -> Unit,
    onExit: () -> Unit,
    onSaveCharacter: (GameCharacter) -> Unit,
    onDeleteCharacter: (GameCharacter) -> Unit,
    latestRoll: DiceRollState?,
    onRollDice: (Int, String) -> Unit,
    onClearLatestRoll: () -> Unit,
    initiativeList: List<InitiativeMember>,
    currentTurnIndex: Int,
    onRollInitiative: () -> Unit,
    onNextTurn: () -> Unit,
    onClearInitiative: () -> Unit
) {
    var playerInputText by remember { mutableStateOf("") }
    val focusManager = LocalFocusManager.current
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    var showCharacterCreator by remember { mutableStateOf(false) }
    var editingCharacter by remember { mutableStateOf<GameCharacter?>(null) }
    var chosenModifierType by remember { mutableStateOf("RAW") }

    // Auto-scroll logs as they update
    LaunchedEffect(logs.size, isThinking) {
        if (logs.isNotEmpty()) {
            listState.animateScrollToItem(logs.size - 1)
        }
    }

    val activeActor = characters.find { it.id == selectedActorId }
    val players = characters.filter { !it.isMonster }
    val monsters = characters.filter { it.isMonster && !it.isDead() }

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // TOP HEADER
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(SlatePanel)
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onExit) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Exit", tint = GoldAccent)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = campaign?.name ?: "Campaign",
                        color = TextLight,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        fontFamily = FontFamily.Serif
                    )
                    Text(
                        text = "Tone: ${campaign?.tonePreset?.uppercase()} | Diff: ${campaign?.difficulty?.uppercase()}",
                        color = TextMuted,
                        fontSize = 10.sp
                    )
                }
            }

            Row {
                IconButton(onClick = onResetLogs) {
                    Icon(Icons.Default.Refresh, contentDescription = "Reset Logs", tint = CrimsonAccent)
                }
            }
        }

        // SCENE SUMMARY / PLOT THREAD SUMMARY ROW
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(DarkCrimson.copy(alpha = 0.3f))
                .border(1.dp, DarkCrimson)
                .padding(12.dp)
        ) {
            val activeThread = facts.firstOrNull { it.category == "PLOT_THREAD" }?.factValue ?: "Explore the cavern vaults."
            val activeLoop = facts.firstOrNull { it.category == "OPEN_LOOP" && !it.isCompletedOrResolved }?.factValue ?: "Hunt the boss monster Kragor."
            
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Star, contentDescription = "Objective", tint = GoldAccent, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "THE QUEST LINE",
                        color = GoldAccent,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.sp
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "$activeThread Goals: $activeLoop",
                    color = TextLight,
                    fontSize = 13.sp,
                    fontStyle = FontStyle.Italic
                )
            }
        }

        // --- TASK 2: LIVE COMBAT TURN CHRONOMETER ---
        if (initiativeList.isEmpty()) {
            if (monsters.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(DarkCrimson.copy(alpha = 0.2f))
                        .border(1.dp, CrimsonAccent.copy(alpha = 0.3f))
                        .clickable { onRollInitiative() }
                        .padding(vertical = 10.dp, horizontal = 16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("⚔️", fontSize = 16.sp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "STRIKE OPPORTUNITY! Roll Initiative for combat chronology!",
                                color = TextLight,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.testTag("prompt_roll_initiative")
                            )
                        }
                        Text(
                            text = "ROLL DICE NOW ➔",
                            color = GoldAccent,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }
            }
        } else {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                colors = CardDefaults.cardColors(containerColor = SlatePanel.copy(alpha = 0.8f)),
                border = BorderStroke(1.dp, GoldAccent.copy(alpha = 0.2f)),
                shape = RoundedCornerShape(0.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "⚔️ ROUND CHRONO:",
                            color = GoldAccent,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.ExtraBold,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.padding(end = 6.dp)
                        )
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            itemsIndexed(initiativeList) { idx, member ->
                                val isCurrent = idx == currentTurnIndex
                                val borderCol = if (isCurrent) GoldAccent else Color.Transparent
                                val scaleWidth = if (isCurrent) 125.dp else 105.dp
                                Card(
                                    modifier = Modifier
                                        .width(scaleWidth)
                                        .border(
                                            BorderStroke(
                                                if (isCurrent) 1.5.dp else 0.5.dp,
                                                if (isCurrent) GoldAccent else TextMuted.copy(alpha = 0.4f)
                                            ),
                                            RoundedCornerShape(8.dp)
                                        ),
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (isCurrent) DarkCrimson else ObsidianDark
                                    )
                                ) {
                                    Column(
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text(
                                                text = if (member.isMonster) "😈" else "🧙‍♂️",
                                                fontSize = 9.sp
                                            )
                                            Text(
                                                text = "#${member.initiativeRoll}",
                                                color = if (isCurrent) Goldenrod else TextMuted,
                                                fontSize = 8.sp,
                                                fontWeight = FontWeight.Bold,
                                                fontFamily = FontFamily.Monospace
                                            )
                                        }
                                        Text(
                                            text = member.name,
                                            color = if (isCurrent) TextLight else TextMuted,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        if (isCurrent) {
                                            Text(
                                                text = "◀ ACTIVE TURN",
                                                color = GoldAccent,
                                                fontSize = 8.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                letterSpacing = 0.5.sp
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Button(
                            onClick = onNextTurn,
                            colors = ButtonDefaults.buttonColors(containerColor = GoldAccent),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                            shape = RoundedCornerShape(4.dp),
                            modifier = Modifier.height(28.dp).testTag("next_turn_button")
                        ) {
                            Text(
                                text = "NEXT",
                                color = ObsidianDark,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        IconButton(
                            onClick = onClearInitiative,
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "End combat",
                                tint = CrimsonAccent,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
            }
        }

        // CHAT NARRATIVE STREAM AND SIDE DETAILS PANEL
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            Row(modifier = Modifier.fillMaxSize()) {
                // CHAT STREAM LazyColumn (Middle Pane)
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(logs) { log ->
                        EventLogItem(log)
                    }

                    if (isThinking) {
                        item {
                            ThinkingNarrationItem()
                        }
                    }
                }
            }
        }

        // MONSTER / THREAT THREAT-TRACKER BAR
        if (monsters.isNotEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(SlatePanel.copy(alpha = 0.8f))
                    .border(1.dp, SlatePanel)
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Column {
                    Text(
                        text = "HOSTILE ENCOUNTERS ARMORED",
                        color = CrimsonAccent,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.sp,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        monsters.forEach { m ->
                            Card(
                                colors = CardDefaults.cardColors(containerColor = SlatePanel),
                                border = BorderStroke(1.dp, DarkCrimson),
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(modifier = Modifier.padding(8.dp)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = m.name,
                                            color = TextLight,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.weight(1f),
                                            maxLines = 1
                                        )
                                        Icon(
                                            imageVector = Icons.Default.Edit,
                                            contentDescription = "Edit Monster",
                                            tint = CrimsonAccent.copy(alpha = 0.7f),
                                            modifier = Modifier
                                                .size(11.dp)
                                                .clickable { editingCharacter = m }
                                        )
                                    }
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text("AC ${m.armorClass}", color = TextMuted, fontSize = 11.sp)
                                        Text("HP ${m.currentHp}/${m.maxHp}", color = CrimsonAccent, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                    LinearProgressIndicator(
                                        progress = { m.currentHp.toFloat() / m.maxHp.toFloat() },
                                        color = CrimsonAccent,
                                        trackColor = Color.DarkGray,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(top = 4.dp)
                                            .height(4.dp)
                                            .clip(RoundedCornerShape(2.dp))
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // PLAYER HERO SELECTOR & QUICK STATUS
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(SlatePanel)
                .padding(vertical = 8.dp)
        ) {
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "SELECT REPRESENTER HERO",
                        color = GoldAccent,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.sp,
                    )
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .clickable { showCharacterCreator = true }
                            .padding(horizontal = 4.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Forge Character",
                            tint = GoldAccent,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                        Text(
                            text = "FORGE SHEET",
                            color = GoldAccent,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )
                    }
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp),
                    horizontalArrangement = Arrangement.Start
                ) {
                    players.forEach { actor ->
                        val isSel = actor.id == selectedActorId
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 4.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isSel) DarkCrimson else SlatePanel.copy(alpha = 0.5f))
                                .border(1.dp, if (isSel) GoldAccent else Color.Transparent, RoundedCornerShape(12.dp))
                                .clickable { onSelectActor(actor.id) }
                                .padding(8.dp)
                        ) {
                            Column {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f, fill = false)
                                    ) {
                                        Text(
                                            actor.name,
                                            color = if (isSel) Goldenrod else TextLight,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Icon(
                                            imageVector = Icons.Default.Edit,
                                            contentDescription = "Edit Character Sheet",
                                            tint = GoldAccent.copy(alpha = 0.7f),
                                            modifier = Modifier
                                                .size(12.dp)
                                                .clickable { editingCharacter = actor }
                                        )
                                    }
                                    Text("Lvl ${actor.level}", color = TextMuted, fontSize = 10.sp)
                                }
                                Text(actor.characterClass, color = TextMuted, fontSize = 11.sp)
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.Favorite,
                                        contentDescription = "HP",
                                        tint = CrimsonAccent,
                                        modifier = Modifier.size(10.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        "${actor.currentHp}/${actor.maxHp}",
                                        color = TextLight,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        "🛡",
                                        fontSize = 11.sp,
                                        modifier = Modifier.padding(bottom = 2.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        "${actor.armorClass}",
                                        color = TextLight,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // QUICK SUGGESTION ATTACK CHIPS
        activeActor?.let { actor ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(ObsidianDark)
                    .padding(horizontal = 16.dp, vertical = 6.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val promptList = if (monsters.isNotEmpty()) {
                        val mName = monsters.first().name.split(" ").first()
                        listOf(
                            "Strike $mName",
                            "Cast Magic Missile",
                            "Perceive Room",
                            "Seduce Threat"
                        )
                    } else {
                        listOf(
                            "Search Cave Vault",
                            "Stealthy Movement",
                            "Light a Torch",
                            "Talk to rock"
                        )
                    }

                    promptList.forEach { p ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .background(SlatePanel)
                                .border(1.dp, GoldAccent.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                                .clickable {
                                    playerInputText = when (p) {
                                        "Strike Grumble" -> "I strike Grumble with my greatsword!"
                                        "Strike Slasher" -> "I shoot Slasher with my weapon!"
                                        "Strike Kragor" -> "I slash Kragor with my heavy greatsword!"
                                        "Cast Magic Missile" -> "I cast Elysia's Magic Missile at Kragor!"
                                        "Perceive Room" -> "I roll standard check to perceive cavern secrets."
                                        "Seduce Threat" -> "I attempt Charisma block check to seduce the monster chief!"
                                        "Search Cave Vault" -> "I search the dripping rock crevices for lost treasures."
                                        "Stealthy Movement" -> "I activate stealthy sneaking shadows movement."
                                        else -> p
                                    }
                                }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(p, color = GoldAccent, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // --- TASK 1: POLYHEDRAL DICE TRAY & MODIFIER SELECTOR ---
        Card(
            modifier = Modifier
                .fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = SlatePanel.copy(alpha = 0.6f)),
            border = BorderStroke(1.dp, GoldAccent.copy(alpha = 0.15f)),
            shape = RoundedCornerShape(0.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
            ) {
                // Modifier Selection Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "🛰️ DICE CUP MODIFIER:",
                        color = GoldAccent,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.ExtraBold,
                        fontFamily = FontFamily.Monospace
                    )
                    
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        listOf("RAW", "STR", "DEX", "CON", "INT", "WIS", "CHA").forEach { mod ->
                            val isSel = chosenModifierType == mod
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(if (isSel) GoldAccent else ObsidianDark)
                                    .clickable { chosenModifierType = mod }
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = mod,
                                    color = if (isSel) ObsidianDark else TextMuted,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Polyhedral Roller Tray
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "🎲 ROLL RPG POLYHEDRAL:",
                        color = TextLight,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Serif
                    )

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(4, 6, 8, 10, 12, 20).forEach { sides ->
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(DarkCrimson)
                                    .border(1.dp, GoldAccent.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                                    .clickable { onRollDice(sides, chosenModifierType) }
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                                    .testTag("roll_d${sides}_button")
                            ) {
                                Text(
                                    text = "d$sides",
                                    color = Goldenrod,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }
                }
            }
        }

        // INPUT CONSOLE BAR
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(SlatePanel)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextField(
                value = playerInputText,
                onValueChange = { playerInputText = it },
                placeholder = {
                    Text(
                        text = "Say or Do something (e.g. 'I search for traps')",
                        color = TextMuted,
                        fontSize = 14.sp
                    )
                },
                maxLines = 2,
                colors = TextFieldDefaults.colors(
                    focusedTextColor = TextLight,
                    unfocusedTextColor = TextLight,
                    focusedContainerColor = ObsidianDark,
                    unfocusedContainerColor = ObsidianDark,
                    focusedIndicatorColor = GoldAccent,
                    unfocusedIndicatorColor = Color.Transparent
                ),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = {
                    if (playerInputText.isNotBlank()) {
                        onSubmitAction(playerInputText)
                        playerInputText = ""
                        focusManager.clearFocus()
                    }
                }),
                modifier = Modifier
                    .weight(1f)
                    .testTag("action_input_field")
            )

            Spacer(modifier = Modifier.width(12.dp))

            IconButton(
                onClick = {
                    if (playerInputText.isNotBlank()) {
                        onSubmitAction(playerInputText)
                        playerInputText = ""
                        focusManager.clearFocus()
                    }
                },
                modifier = Modifier
                    .clip(CircleShape)
                    .background(GoldAccent)
                    .size(48.dp)
                    .testTag("send_action_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Send,
                    contentDescription = "Send Intent",
                    tint = ObsidianDark
                )
            }
        }
    }

    if (showCharacterCreator) {
        CharacterCreatorDialog(
            campaignId = campaign?.id ?: 0,
            character = null,
            onDismiss = { showCharacterCreator = false },
            onSave = { newChar ->
                onSaveCharacter(newChar)
                showCharacterCreator = false
            }
        )
    }

    if (editingCharacter != null) {
        CharacterCreatorDialog(
            campaignId = campaign?.id ?: 0,
            character = editingCharacter,
            onDismiss = { editingCharacter = null },
            onSave = { updatedChar ->
                onSaveCharacter(updatedChar)
                editingCharacter = null
            },
            onDelete = { deletedChar ->
                onDeleteCharacter(deletedChar)
                editingCharacter = null
            }
        )
    }

    // TASK 1 OVERLAY: THE DICE ROLL CHAMBER
    latestRoll?.let { roll ->
        Dialog(
            onDismissRequest = onClearLatestRoll
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .border(2.dp, GoldAccent, RoundedCornerShape(16.dp)),
                colors = CardDefaults.cardColors(containerColor = ObsidianDark),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "۩ THE CHAMBER REVEALS ۩",
                        color = GoldAccent,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.ExtraBold,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 1.sp
                    )

                    // Large circular dice result
                    Box(
                        modifier = Modifier
                            .size(100.dp)
                            .clip(CircleShape)
                            .background(DarkCrimson)
                            .border(3.dp, GoldAccent, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = roll.total.toString(),
                                color = Goldenrod,
                                fontSize = 42.sp,
                                fontWeight = FontWeight.ExtraBold,
                                fontFamily = FontFamily.Serif
                            )
                            Text(
                                text = "TOTAL",
                                color = GoldAccent,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                        }
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "${roll.characterName} rolled a d${roll.sides}",
                            color = TextLight,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Serif
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "🎲 Raw Roll: ${roll.naturalRoll} | Modifier: ${if (roll.modifierVal >= 0) "+${roll.modifierVal}" else roll.modifierVal} (${roll.modifierLabel})",
                            color = TextMuted,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            textAlign = TextAlign.Center
                        )
                    }

                    Button(
                        onClick = onClearLatestRoll,
                        colors = ButtonDefaults.buttonColors(containerColor = GoldAccent),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth().height(42.dp).testTag("close_dice_modal")
                    ) {
                        Text(
                            text = "RETURN TO TABLE",
                            color = ObsidianDark,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun EventLogItem(log: EventLog) {
    val containerBg = when (log.messageType) {
        "NARRATION" -> SlatePanel
        "DIALOGUE" -> ObsidianDark
        "SYSTEM_CHECK" -> DarkCrimson.copy(alpha = 0.15f)
        else -> Color.Transparent
    }

    val borderWidth = if (log.messageType == "SYSTEM_CHECK") 1.dp else 0.dp
    val borderColor = if (log.messageType == "SYSTEM_CHECK") DarkCrimson else Color.Transparent

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(containerBg)
            .border(borderWidth, borderColor, RoundedCornerShape(8.dp))
            .padding(12.dp)
    ) {
        Column {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Header details
                Text(
                    text = log.senderName.uppercase(),
                    color = when (log.senderName) {
                        "DM" -> GoldAccent
                        "System" -> TextMuted
                        else -> Goldenrod
                    },
                    fontSize = 11.sp,
                    fontWeight = FontWeight.ExtraBold,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 1.sp
                )

                log.diceRollResultText?.let { rollText ->
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(DarkCrimson)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = rollText,
                            color = TextLight,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Body message text
            Text(
                text = log.messageText,
                color = when (log.messageType) {
                    "NARRATION" -> TextLight
                    "SYSTEM_CHECK" -> Goldenrod
                    else -> TextLight
                },
                fontSize = if (log.messageType == "NARRATION") 15.sp else 14.sp,
                fontFamily = if (log.messageType == "NARRATION") FontFamily.Serif else FontFamily.Default,
                fontStyle = if (log.messageType == "NARRATION") FontStyle.Italic else FontStyle.Normal,
                lineHeight = 22.sp
            )
        }
    }
}

@Composable
fun ThinkingNarrationItem() {
    val infiniteTransition = rememberInfiniteTransition(label = "ThinkingDots")
    val dotAlpha1 by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 1000
                0.2f at 0
                1.0f at 300
                0.2f at 600
            }
        ),
        label = "Dot1"
    )
    val dotAlpha2 by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 1000
                0.2f at 200
                1.0f at 500
                0.2f at 800
            }
        ),
        label = "Dot2"
    )
    val dotAlpha3 by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 1000
                0.2f at 400
                1.0f at 700
                0.2f at 1000
            }
        ),
        label = "Dot3"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(SlatePanel)
            .padding(16.dp)
    ) {
        Column {
            Text(
                text = "DM IS NARRATING...",
                color = GoldAccent,
                fontSize = 11.sp,
                fontWeight = FontWeight.ExtraBold,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 1.sp
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(GoldAccent.copy(alpha = dotAlpha1))
                )
                Spacer(modifier = Modifier.width(4.dp))
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(GoldAccent.copy(alpha = dotAlpha2))
                )
                Spacer(modifier = Modifier.width(4.dp))
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(GoldAccent.copy(alpha = dotAlpha3))
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Dungeon Master engine is evaluating rules checks and casting visual narration...",
                    color = TextMuted,
                    fontSize = 13.sp,
                    fontStyle = FontStyle.Italic
                )
            }
        }
    }
}

@Composable
fun CharacterCreatorDialog(
    campaignId: Int,
    character: GameCharacter?,
    onDismiss: () -> Unit,
    onSave: (GameCharacter) -> Unit,
    onDelete: ((GameCharacter) -> Unit)? = null
) {
    var name by remember { mutableStateOf(character?.name ?: "") }
    var charClass by remember { mutableStateOf(character?.characterClass ?: "Fighter") }
    var isMonster by remember { mutableStateOf(character?.isMonster ?: false) }
    var level by remember { mutableStateOf(character?.level ?: 1) }

    // Stats
    var strength by remember { mutableStateOf(character?.strength ?: 10) }
    var dexterity by remember { mutableStateOf(character?.dexterity ?: 10) }
    var constitution by remember { mutableStateOf(character?.constitution ?: 10) }
    var intelligence by remember { mutableStateOf(character?.intelligence ?: 10) }
    var wisdom by remember { mutableStateOf(character?.wisdom ?: 10) }
    var charisma by remember { mutableStateOf(character?.charisma ?: 10) }

    var maxHp by remember { mutableStateOf(character?.maxHp ?: 10) }
    var armorClass by remember { mutableStateOf(character?.armorClass ?: 10) }
    var weaponsText by remember { mutableStateOf(character?.weaponsText ?: "") }
    var equipmentText by remember { mutableStateOf(character?.equipmentText ?: "") }

    androidx.compose.ui.window.Dialog(
        onDismissRequest = onDismiss
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.95f)
                .border(2.dp, GoldAccent, RoundedCornerShape(16.dp)),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = ObsidianDark)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (character == null) "۩ FORGE CHARACTER SHEET ۩" else "۩ EDIT CHARACTER SHEET ۩",
                        color = GoldAccent,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.ExtraBold,
                        fontFamily = FontFamily.Serif
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = GoldAccent)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Scrollable Form
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        // Role Type Selection (Hero vs NPC/Monster)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(SlatePanel)
                                .padding(4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (!isMonster) DarkCrimson else Color.Transparent)
                                    .clickable { isMonster = false }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("PLAYABLE HERO", color = if (!isMonster) Goldenrod else TextMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (isMonster) DarkCrimson else Color.Transparent)
                                    .clickable { isMonster = true }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("MONSTER / NPC", color = if (isMonster) Goldenrod else TextMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    item {
                        // Name Input
                        OutlinedTextField(
                            value = name,
                            onValueChange = { name = it },
                            label = { Text("Hero or Creature Name", color = TextMuted) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = GoldAccent,
                                unfocusedBorderColor = TextMuted,
                                focusedTextColor = TextLight,
                                unfocusedTextColor = TextLight
                            ),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag("char_sheet_name_input")
                        )
                    }

                    item {
                        // Class Suggestion / Text Field
                        Column {
                            Text("Character Class", color = TextLight, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                val standardClasses = if (isMonster) listOf("Goblin", "Orc", "Skeleton", "Dragon") else listOf("Fighter", "Wizard", "Cleric", "Rogue", "Paladin")
                                standardClasses.forEach { sc ->
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(if (charClass == sc) DarkCrimson else SlatePanel)
                                            .border(1.dp, if (charClass == sc) GoldAccent else Color.Transparent, RoundedCornerShape(12.dp))
                                            .clickable { charClass = sc }
                                            .padding(horizontal = 10.dp, vertical = 6.dp)
                                    ) {
                                        Text(sc, color = if (charClass == sc) Goldenrod else TextLight, fontSize = 11.sp)
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = charClass,
                                onValueChange = { charClass = it },
                                label = { Text("Custom Class or Creature Race", color = TextMuted) },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = GoldAccent,
                                    unfocusedBorderColor = TextMuted,
                                    focusedTextColor = TextLight,
                                    unfocusedTextColor = TextLight
                                ),
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    item {
                        // Level Stepper
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Experience Level", color = TextLight, fontWeight = FontWeight.Bold)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(
                                    onClick = { if (level > 1) level-- },
                                    colors = IconButtonDefaults.iconButtonColors(containerColor = SlatePanel)
                                ) {
                                    Icon(imageVector = Icons.Default.KeyboardArrowDown, contentDescription = "Decrease", tint = GoldAccent)
                                }
                                Text(
                                    level.toString(),
                                    color = Goldenrod,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 16.dp)
                                )
                                IconButton(
                                    onClick = { if (level < 20) level++ },
                                    colors = IconButtonDefaults.iconButtonColors(containerColor = SlatePanel)
                                ) {
                                    Icon(imageVector = Icons.Default.KeyboardArrowUp, contentDescription = "Increase", tint = GoldAccent)
                                }
                            }
                        }
                    }

                    item {
                        // Vitals: HP & Armor Class
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Max Hit Points (HP)", color = TextLight, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    IconButton(
                                        onClick = { if (maxHp > 1) maxHp-- },
                                        colors = IconButtonDefaults.iconButtonColors(containerColor = SlatePanel),
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Text("-", color = CrimsonAccent, fontWeight = FontWeight.Bold)
                                    }
                                    Text(
                                        maxHp.toString(),
                                        color = TextLight,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.weight(1f),
                                        textAlign = TextAlign.Center
                                    )
                                    IconButton(
                                        onClick = { if (maxHp < 999) maxHp++ },
                                        colors = IconButtonDefaults.iconButtonColors(containerColor = SlatePanel),
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Text("+", color = CrimsonAccent, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Text("Armor Class (AC)", color = TextLight, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    IconButton(
                                        onClick = { if (armorClass > 1) armorClass-- },
                                        colors = IconButtonDefaults.iconButtonColors(containerColor = SlatePanel),
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Text("-", color = GoldAccent, fontWeight = FontWeight.Bold)
                                    }
                                    Text(
                                        armorClass.toString(),
                                        color = TextLight,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.weight(1f),
                                        textAlign = TextAlign.Center
                                    )
                                    IconButton(
                                        onClick = { if (armorClass < 40) armorClass++ },
                                        colors = IconButtonDefaults.iconButtonColors(containerColor = SlatePanel),
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Text("+", color = GoldAccent, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }

                    item {
                        // Stat Sheet Block
                        Column {
                            Text(
                                text = "CORE ABILITY SCORES (8 - 20)",
                                color = GoldAccent,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 1.sp,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )

                            // 4D6 Drop Lowest Roller Button
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 12.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(SlatePanel)
                                    .clickable {
                                        // Roll 4d6 drop lowest for all 6 stats
                                        val rollStat = {
                                            List(4) { kotlin.random.Random.nextInt(1, 7) }.sorted().drop(1).sum()
                                        }
                                        strength = rollStat()
                                        dexterity = rollStat()
                                        constitution = rollStat()
                                        intelligence = rollStat()
                                        wisdom = rollStat()
                                        charisma = rollStat()
                                        
                                        // Recalculate hit points based on level & constitution modifier
                                        // Class standard HD: Fighter=10, Wizard=6, Cleric=8, Rogue=8, Paladin=10
                                        val hitDie = when (charClass.lowercase().trim()) {
                                            "fighter", "paladin" -> 10
                                            "wizard", "sorcerer" -> 6
                                            "cleric", "druid", "rogue", "orclike", "bard", "monk", "ranger" -> 8
                                            "barbarian" -> 12
                                            else -> 8
                                        }
                                        val conMod = (constitution - 10) / 2
                                        maxHp = (hitDie + conMod).coerceAtLeast(4) + (level - 1) * (hitDie / 2 + 1 + conMod).coerceAtLeast(1)
                                        // Recalculate Armor Class based on dex modifier
                                        val dexMod = (dexterity - 10) / 2
                                        armorClass = when (charClass.lowercase().trim()) {
                                            "fighter", "paladin" -> 16 // Starting heavy mail
                                            "wizard", "sorcerer" -> 10 + dexMod // Unarmored wizard
                                            "cleric", "druid" -> 14 + dexMod.coerceAtMost(2) // Scale/Shield
                                            "rogue", "ranger" -> 12 + dexMod // Leather
                                            else -> 10 + dexMod
                                        }
                                    }
                                    .border(1.dp, GoldAccent.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                                    .padding(vertical = 12.dp, horizontal = 16.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = "Roll",
                                    tint = GoldAccent,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "🎲 RANDOMIZE VIA 4D6 DEVIL DICE",
                                    color = Goldenrod,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontFamily = FontFamily.Monospace
                                )
                            }

                            val attributes = listOf(
                                Triple("Strength (STR)", strength, { v: Int -> strength = v }),
                                Triple("Dexterity (DEX)", dexterity, { v: Int -> dexterity = v }),
                                Triple("Constitution (CON)", constitution, { v: Int -> constitution = v }),
                                Triple("Intelligence (INT)", intelligence, { v: Int -> intelligence = v }),
                                Triple("Wisdom (WIS)", wisdom, { v: Int -> wisdom = v }),
                                Triple("Charisma (CHA)", charisma, { v: Int -> charisma = v })
                            )

                            attributes.forEach { (label, value, setter) ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(SlatePanel)
                                        .padding(horizontal = 12.dp, vertical = 6.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(label, color = TextLight, fontSize = 13.sp)
                                        val mod = (value - 10) / 2
                                        val plus = if (mod >= 0) "+" else ""
                                        Text("Modifier: $plus$mod", color = TextMuted, fontSize = 10.sp)
                                    }

                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        IconButton(
                                            onClick = { if (value > 3) setter(value - 1) },
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Text("-", color = GoldAccent, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                        }
                                        Text(
                                            value.toString(),
                                            color = Goldenrod,
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 8.dp)
                                        )
                                        IconButton(
                                            onClick = { if (value < 30) setter(value + 1) },
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Text("+", color = GoldAccent, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    item {
                        // Weapons of Choice
                        OutlinedTextField(
                            value = weaponsText,
                            onValueChange = { weaponsText = it },
                            label = { Text("Combat Weapons & Formulas", color = TextMuted) },
                            placeholder = { Text("e.g. Scimitar (1d6+2 slashing)", color = Color.DarkGray) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = GoldAccent,
                                unfocusedBorderColor = TextMuted,
                                focusedTextColor = TextLight,
                                unfocusedTextColor = TextLight
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    item {
                        // Equipment & Spells list
                        OutlinedTextField(
                            value = equipmentText,
                            onValueChange = { equipmentText = it },
                            label = { Text("Abilities, Spells & Equipment", color = TextMuted) },
                            placeholder = { Text("e.g. Shield, Sleep Spell, Healing potion", color = Color.DarkGray) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = GoldAccent,
                                unfocusedBorderColor = TextMuted,
                                focusedTextColor = TextLight,
                                unfocusedTextColor = TextLight
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Footer Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (character != null && onDelete != null) {
                        Button(
                            onClick = { onDelete(character) },
                            colors = ButtonDefaults.buttonColors(containerColor = CrimsonAccent),
                            modifier = Modifier.weight(0.4f)
                        ) {
                            Text("DELETE", color = TextLight, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        }
                    }

                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(containerColor = SlatePanel),
                        modifier = Modifier.weight(0.3f)
                    ) {
                        Text("CANCEL", color = TextMuted, fontSize = 11.sp)
                    }

                    Button(
                        onClick = {
                            if (name.isNotBlank()) {
                                onSave(
                                    GameCharacter(
                                        id = character?.id ?: 0,
                                        campaignId = campaignId,
                                        name = name,
                                        characterClass = charClass,
                                        isMonster = isMonster,
                                        level = level,
                                        strength = strength,
                                        dexterity = dexterity,
                                        constitution = constitution,
                                        intelligence = intelligence,
                                        wisdom = wisdom,
                                        charisma = charisma,
                                        maxHp = maxHp,
                                        currentHp = if (character == null) maxHp else character.currentHp.coerceAtMost(maxHp),
                                        armorClass = armorClass,
                                        weaponsText = weaponsText,
                                        equipmentText = equipmentText,
                                        conditionsText = character?.conditionsText ?: "",
                                        location = character?.location ?: "Entrance"
                                    )
                                )
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = GoldAccent),
                        enabled = name.isNotBlank(),
                        modifier = Modifier
                            .weight(0.5f)
                            .testTag("char_sheet_save_button")
                    ) {
                        Text("SAVE SHEET", color = ObsidianDark, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun GatheringRoomScreen(
    campaign: Campaign?,
    characters: List<GameCharacter>,
    facts: List<WorldFact>,
    isThinking: Boolean,
    onUpdateCampaign: (Campaign) -> Unit,
    onSaveCharacter: (GameCharacter) -> Unit,
    onDeleteCharacter: (GameCharacter) -> Unit,
    onEmbark: () -> Unit,
    onExit: () -> Unit,
    onLoadPresets: () -> Unit,
    onDrawRumor: () -> Unit
) {
    if (campaign == null) return

    var showCreator by remember { mutableStateOf(false) }
    var editingChar by remember { mutableStateOf<GameCharacter?>(null) }

    val heroes = characters.filter { !it.isMonster }
    val rumors = facts.filter { it.category == "RUMOR" }
    
    var campaignName by remember(campaign.id) { mutableStateOf(campaign.name) }
    var selectedTone by remember(campaign.id) { mutableStateOf(campaign.tonePreset) }
    var selectedDiff by remember(campaign.id) { mutableStateOf(campaign.difficulty) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianDark)
    ) {
        // TOP HEADER
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(SlatePanel)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onExit) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Return to Lobby",
                    tint = GoldAccent
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    text = "۩ THE HEARTH & TAVERN ۩",
                    color = GoldAccent,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold,
                    fontFamily = FontFamily.Serif
                )
                Text(
                    text = "Campaign Setup & Party Forge",
                    color = TextMuted,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        }

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
            contentPadding = PaddingValues(vertical = 24.dp)
        ) {
            // Welcome Intro Callout
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = SlatePanel),
                    border = BorderStroke(1.dp, GoldAccent.copy(alpha = 0.4f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = "Tavern Setup",
                                tint = GoldAccent,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "SESSION 0 GATHERING",
                                color = GoldAccent,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 1.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "A legendary campaign always starts around the tavern hearth. Before the Dungeon Master unfolds the maps and rolls the opening narration, your group must set up parameters, forge heroic characters, and gather the whispers of the local land. Ready your dice and follow the steps below!",
                            color = TextLight,
                            fontSize = 12.sp,
                            lineHeight = 18.sp
                        )
                    }
                }
            }

            // D&D PROGRESS CHECKLIST STEPPER
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = SlatePanel),
                    border = BorderStroke(1.dp, GoldAccent.copy(alpha = 0.3f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "۩ TABLETOP PREPARATION STEPS ۩",
                            color = GoldAccent,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.ExtraBold,
                            fontFamily = FontFamily.Serif,
                            modifier = Modifier
                                .align(Alignment.CenterHorizontally)
                                .padding(bottom = 12.dp)
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            val stepChecks = listOf(
                                Triple("1. SCRIBE", campaignName.isNotBlank(), "Setting"),
                                Triple("2. FORGE", heroes.isNotEmpty(), "Party"),
                                Triple("3. RUMOR", facts.any { it.category == "RUMOR" }, "Lores"),
                                Triple("4. EMBARK", heroes.isNotEmpty() && campaignName.isNotBlank(), "Quest")
                            )

                            stepChecks.forEach { (title, completed, desc) ->
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(26.dp)
                                            .clip(CircleShape)
                                            .background(if (completed) EmeraldGreen else DarkCrimson),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = if (completed) "✓" else "!",
                                            color = if (completed) TextLight else Goldenrod,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.ExtraBold
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = title,
                                        color = if (completed) Goldenrod else TextLight,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = desc,
                                        color = TextMuted,
                                        fontSize = 8.sp,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // SECTION 1: DM's LEDGER
            item {
                Text(
                    text = "I. THE DM'S LEDGER (SCENARIO SETUP)",
                    color = GoldAccent,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.5.sp,
                    modifier = Modifier.padding(bottom = 4.dp)
                )

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = SlatePanel)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        // Editable Campaign Name
                        OutlinedTextField(
                            value = campaignName,
                            onValueChange = {
                                campaignName = it
                                onUpdateCampaign(campaign.copy(name = it))
                            },
                            label = { Text("Campaign Scenario Name", color = TextMuted) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = GoldAccent,
                                unfocusedBorderColor = TextMuted,
                                focusedTextColor = TextLight,
                                unfocusedTextColor = TextLight
                            ),
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("forge_campaign_name_input")
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Tone Selector
                        Text("Campaign Tone", color = TextLight, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 6.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            listOf("heroic", "grim", "comedic").forEach { tone ->
                                val isSelected = selectedTone == tone
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isSelected) DarkCrimson else ObsidianDark)
                                        .border(1.dp, if (isSelected) GoldAccent else TextMuted.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                                        .clickable {
                                            selectedTone = tone
                                            onUpdateCampaign(campaign.copy(tonePreset = tone))
                                        }
                                        .padding(vertical = 10.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = tone.uppercase(),
                                        color = if (isSelected) Goldenrod else TextLight,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Difficulty Selector
                        Text("Session Difficulty", color = TextLight, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 6.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            listOf("easy", "normal", "hard").forEach { diff ->
                                val isSelected = selectedDiff == diff
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isSelected) DarkCrimson else ObsidianDark)
                                        .border(1.dp, if (isSelected) GoldAccent else TextMuted.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                                        .clickable {
                                            selectedDiff = diff
                                            onUpdateCampaign(campaign.copy(difficulty = diff))
                                        }
                                        .padding(vertical = 10.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = diff.uppercase(),
                                        color = if (isSelected) Goldenrod else TextLight,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // SECTION 2: THE CAMPFIRE
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "II. THE CAMPFIRE (FORGE YOUR PARTY)",
                        color = GoldAccent,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.5.sp
                    )

                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .clickable { showCreator = true }
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Forge New Sheet",
                            tint = GoldAccent,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "ROLL NEW HERO",
                            color = GoldAccent,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))

                if (heroes.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, CrimsonAccent.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                            .background(SlatePanel)
                            .padding(20.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "🛡️ Your party campfire is cold and empty!",
                                color = TextMuted,
                                fontSize = 12.sp,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Button(
                                    onClick = onLoadPresets,
                                    colors = ButtonDefaults.buttonColors(containerColor = DarkCrimson),
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text("🧙‍♂️ LOAD PRESET PARTY", color = Goldenrod, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                                Button(
                                    onClick = { showCreator = true },
                                    colors = ButtonDefaults.buttonColors(containerColor = GoldAccent),
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text("⚔️ FORGE CUSTOM HERO", color = ObsidianDark, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }

            // List of created heroes
            items(heroes) { hero ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { editingChar = hero },
                    colors = CardDefaults.cardColors(containerColor = SlatePanel),
                    border = BorderStroke(1.dp, if (editingChar == hero) GoldAccent else TextMuted.copy(alpha = 0.2f))
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(DarkCrimson),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = hero.name.take(1).uppercase(),
                                        color = Goldenrod,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = hero.name,
                                        color = Goldenrod,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "Level ${hero.level} ${hero.characterClass}",
                                        color = TextMuted,
                                        fontSize = 11.sp
                                    )
                                }
                            }

                            Row {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "Edit Character Sheet",
                                    tint = GoldAccent,
                                    modifier = Modifier
                                        .size(16.dp)
                                        .clickable { editingChar = hero }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        HorizontalDivider(color = ObsidianDark, thickness = 1.dp)
                        Spacer(modifier = Modifier.height(10.dp))

                        // Stats bar
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("VITALS", color = GoldAccent, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                Text("HP: ${hero.currentHp}/${hero.maxHp} | AC: ${hero.armorClass}", color = TextLight, fontSize = 11.sp)
                            }
                            Column {
                                Text("ATTRIBUTES", color = GoldAccent, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    listOf("STR" to hero.strength, "DEX" to hero.dexterity, "CON" to hero.constitution)
                                        .forEach { (label, value) ->
                                            Text("$label: $value", color = TextLight, fontSize = 10.sp)
                                        }
                                }
                            }
                        }
                    }
                }
            }

            // SECTION 3: STARTING LORE LOBBY PREVIEW & TAVERN RUMORS
            item {
                Text(
                    text = "III. THE SCENE BOOK (WORLD HOOKS & RUMORS)",
                    color = GoldAccent,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.5.sp,
                    modifier = Modifier.padding(bottom = 4.dp)
                )

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = SlatePanel)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.Star, contentDescription = "Active Threads", tint = GoldAccent, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Active Hooks & Rumors", color = GoldAccent, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        facts.forEach { fact ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Text("•", color = GoldAccent, modifier = Modifier.padding(end = 8.dp))
                                Text(
                                    text = "[${fact.category}] ${fact.factValue}",
                                    color = TextLight,
                                    fontSize = 11.sp,
                                    lineHeight = 15.sp,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        
                        Button(
                            onClick = onDrawRumor,
                            colors = ButtonDefaults.buttonColors(containerColor = DarkCrimson),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(40.dp),
                            shape = RoundedCornerShape(6.dp),
                            border = BorderStroke(1.dp, GoldAccent.copy(alpha = 0.4f))
                        ) {
                            Text(
                                text = "📜 CHAT WITH BARKEEP (DRAW NEW RUMOR)",
                                color = TextLight,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // SECTION 4: THE CALL TO ADVENTURE (EMBARK ACTION)
            item {
                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = onEmbark,
                    colors = ButtonDefaults.buttonColors(containerColor = GoldAccent),
                    enabled = heroes.isNotEmpty() && !isThinking && campaignName.isNotBlank(),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .testTag("launch_embargation_button")
                ) {
                    if (isThinking) {
                        CircularProgressIndicator(color = ObsidianDark, modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "THE DM IS PREPARING THE WORLD...",
                            color = ObsidianDark,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    } else {
                        Text(
                            text = "۩ ROLL INITIATIVE & EMBARK ۩",
                            color = ObsidianDark,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.ExtraBold,
                            fontFamily = FontFamily.Serif
                        )
                    }
                }

                if (heroes.isEmpty()) {
                    Text(
                        text = "Forge at least one Playable Hero or load presets above to unlock embarking!",
                        color = CrimsonAccent,
                        fontSize = 11.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp)
                    )
                }
            }
        }
    }

    if (showCreator) {
        CharacterCreatorDialog(
            campaignId = campaign.id,
            character = null,
            onDismiss = { showCreator = false },
            onSave = { newChar ->
                onSaveCharacter(newChar)
                showCreator = false
            }
        )
    }

    if (editingChar != null) {
        CharacterCreatorDialog(
            campaignId = campaign.id,
            character = editingChar,
            onDismiss = { editingChar = null },
            onSave = { updatedChar ->
                onSaveCharacter(updatedChar)
                editingChar = null
            },
            onDelete = { deletedChar ->
                onDeleteCharacter(deletedChar)
                editingChar = null
            }
        )
    }
}
