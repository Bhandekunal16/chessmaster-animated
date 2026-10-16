package com.example.chess.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.chess.model.*

@Composable
fun SettingsDialog(
    currentSettings: GameSettings,
    onSave: (GameSettings) -> Unit,
    onOpenThemeStudio: () -> Unit = {},
    onDismiss: () -> Unit
) {
    var mode by remember { mutableStateOf(currentSettings.mode) }
    var difficulty by remember { mutableStateOf(currentSettings.aiDifficulty) }
    var theme by remember { mutableStateOf(currentSettings.boardTheme) }
    var pieceSet by remember { mutableStateOf(currentSettings.pieceSet) }
    var animSpeed by remember { mutableStateOf(currentSettings.animationSpeed) }
    var sound by remember { mutableStateOf(currentSettings.soundEnabled) }
    var haptics by remember { mutableStateOf(currentSettings.hapticsEnabled) }
    var is3D by remember { mutableStateOf(currentSettings.is3DView) }
    var timeControl by remember { mutableStateOf(currentSettings.timeControl) }
    var syncEffects by remember { mutableStateOf(currentSettings.syncThemeEffects) }
    val context = LocalContext.current

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnClickOutside = false,
            dismissOnBackPress = true
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.65f))
                .clickable(
                    interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss
                ),
            contentAlignment = Alignment.Center
        ) {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFF334155)),
                modifier = Modifier
                    .fillMaxWidth(0.95f)
                    .fillMaxHeight(0.92f)
                    .clickable(
                        interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                        indication = null,
                        onClick = { /* Consume clicks so dialog does not dismiss */ }
                    )
                    .testTag("settings_dialog")
            ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF1E293B))
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            color = Color(0xFF0284C7),
                            shape = CircleShape,
                            modifier = Modifier.size(38.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Tune,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                        Column {
                            Text(
                                text = "Game Settings",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "Rules, AI difficulty, themes & sensory controls",
                                fontSize = 12.sp,
                                color = Color(0xFF94A3B8)
                            )
                        }
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        IconButton(
                            onClick = { ShareAppHelper.shareApp(context) },
                            modifier = Modifier.testTag("settings_header_share_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = "Share App",
                                tint = Color(0xFF38BDF8)
                            )
                        }
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier.testTag("close_settings_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close Settings",
                                tint = Color(0xFFCBD5E1)
                            )
                        }
                    }
                }

                // Scrollable Settings Content
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .testTag("settings_lazy_column"),
                    verticalArrangement = Arrangement.spacedBy(22.dp),
                    contentPadding = PaddingValues(vertical = 18.dp)
                ) {
                    // Section 1: Game Mode
                    item {
                        SettingsSectionHeader(
                            icon = Icons.Default.SportsEsports,
                            title = "Game Mode",
                            subtitle = "Choose match type"
                        )
                        Spacer(Modifier.height(10.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            GameModeCard(
                                title = "Player vs Computer",
                                description = "Single player against intelligent engine with customizable depth",
                                icon = Icons.Default.Computer,
                                isSelected = mode == GameMode.PVC,
                                onClick = { mode = GameMode.PVC }
                            )
                            GameModeCard(
                                title = "Pass & Play (2 Players)",
                                description = "Local two-player match on the same device with flip controls",
                                icon = Icons.Default.People,
                                isSelected = mode == GameMode.PVP,
                                onClick = { mode = GameMode.PVP }
                            )
                        }
                    }

                    // Section 2: AI Difficulty
                    item {
                        val isEnabled = mode == GameMode.PVC
                        SettingsSectionHeader(
                            icon = Icons.Default.Psychology,
                            title = "AI Difficulty",
                            subtitle = if (isEnabled) "Adjust engine search depth & heuristics" else "Disabled in Pass & Play mode",
                            modifier = Modifier.alpha(if (isEnabled) 1f else 0.45f)
                        )
                        Spacer(Modifier.height(10.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .alpha(if (isEnabled) 1f else 0.45f),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            AIDifficultyCard(
                                difficulty = AIDifficulty.EASY,
                                title = "Novice",
                                subtitle = "Friendly for beginners",
                                isSelected = difficulty == AIDifficulty.EASY && isEnabled,
                                isEnabled = isEnabled,
                                onClick = { if (isEnabled) difficulty = AIDifficulty.EASY },
                                modifier = Modifier.weight(1f)
                            )
                            AIDifficultyCard(
                                difficulty = AIDifficulty.MEDIUM,
                                title = "Intermediate",
                                subtitle = "Solid tactical awareness",
                                isSelected = difficulty == AIDifficulty.MEDIUM && isEnabled,
                                isEnabled = isEnabled,
                                onClick = { if (isEnabled) difficulty = AIDifficulty.MEDIUM },
                                modifier = Modifier.weight(1f)
                            )
                            AIDifficultyCard(
                                difficulty = AIDifficulty.HARD,
                                title = "Master",
                                subtitle = "Deep alpha-beta search",
                                isSelected = difficulty == AIDifficulty.HARD && isEnabled,
                                isEnabled = isEnabled,
                                onClick = { if (isEnabled) difficulty = AIDifficulty.HARD },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    // Section 3: Board Theme Gallery
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            SettingsSectionHeader(
                                icon = Icons.Default.GridOn,
                                title = "Board Theme",
                                subtitle = "Select board palette and square contrast"
                            )
                            TextButton(
                                onClick = {
                                    onDismiss()
                                    onOpenThemeStudio()
                                }
                            ) {
                                Icon(Icons.Default.Palette, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color(0xFF38BDF8))
                                Spacer(Modifier.width(4.dp))
                                Text("Piece Sets", fontSize = 12.sp, color = Color(0xFF38BDF8))
                            }
                        }
                        Spacer(Modifier.height(10.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            val allThemes = BoardTheme.values()
                            for (row in allThemes.indices step 2) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    BoardThemeCard(
                                        theme = allThemes[row],
                                        isSelected = theme == allThemes[row],
                                        onClick = { theme = allThemes[row] },
                                        modifier = Modifier.weight(1f)
                                    )
                                    if (row + 1 < allThemes.size) {
                                        BoardThemeCard(
                                            theme = allThemes[row + 1],
                                            isSelected = theme == allThemes[row + 1],
                                            onClick = { theme = allThemes[row + 1] },
                                            modifier = Modifier.weight(1f)
                                        )
                                    } else {
                                        Spacer(Modifier.weight(1f))
                                    }
                                }
                            }
                        }
                    }

                    // Section 3b: Piece Set Gallery & Options
                    item {
                        SettingsSectionHeader(
                            icon = Icons.Default.Category,
                            title = "Piece Theme & Style",
                            subtitle = "Choose custom rendered piece aesthetics and silhouettes"
                        )
                        Spacer(Modifier.height(10.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            val allPieceSets = PieceSet.values()
                            for (row in allPieceSets.indices step 2) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    PieceSetCard(
                                        pieceSet = allPieceSets[row],
                                        isSelected = pieceSet == allPieceSets[row],
                                        onClick = { pieceSet = allPieceSets[row] },
                                        modifier = Modifier.weight(1f)
                                    )
                                    if (row + 1 < allPieceSets.size) {
                                        PieceSetCard(
                                            pieceSet = allPieceSets[row + 1],
                                            isSelected = pieceSet == allPieceSets[row + 1],
                                            onClick = { pieceSet = allPieceSets[row + 1] },
                                            modifier = Modifier.weight(1f)
                                        )
                                    } else {
                                        Spacer(Modifier.weight(1f))
                                    }
                                }
                            }
                        }
                    }

                    // Section 4: Piece Animation Speed
                    item {
                        SettingsSectionHeader(
                            icon = Icons.Default.Speed,
                            title = "Piece Animation Speed",
                            subtitle = "Control how quickly pieces move and perform capture animations."
                        )
                        Spacer(Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            AnimationSpeedCard(
                                speed = AnimationSpeed.SLOW,
                                label = "Cinematic",
                                subLabel = "1.5x Slow",
                                isSelected = animSpeed == AnimationSpeed.SLOW,
                                onClick = { animSpeed = AnimationSpeed.SLOW },
                                modifier = Modifier.weight(1f)
                            )
                            AnimationSpeedCard(
                                speed = AnimationSpeed.NORMAL,
                                label = "Normal",
                                subLabel = "1.0x Default",
                                isSelected = animSpeed == AnimationSpeed.NORMAL,
                                onClick = { animSpeed = AnimationSpeed.NORMAL },
                                modifier = Modifier.weight(1f)
                            )
                            AnimationSpeedCard(
                                speed = AnimationSpeed.FAST,
                                label = "Rapid",
                                subLabel = "0.6x Fast",
                                isSelected = animSpeed == AnimationSpeed.FAST,
                                onClick = { animSpeed = AnimationSpeed.FAST },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    // Section 5: Clock and Timer
                    item {
                        SettingsSectionHeader(
                            icon = Icons.Default.Timer,
                            title = "Clock & Time Control",
                            subtitle = "Select game clock limit per player"
                        )
                        Spacer(Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            TimeControlCard(
                                timeControl = TimeControl.UNLIMITED,
                                label = "No Timer",
                                desc = "Unlimited",
                                isSelected = timeControl == TimeControl.UNLIMITED,
                                onClick = { timeControl = TimeControl.UNLIMITED },
                                modifier = Modifier.weight(1f)
                            )
                            TimeControlCard(
                                timeControl = TimeControl.BLITZ_5,
                                label = "Blitz 5 min",
                                desc = "Tactical",
                                isSelected = timeControl == TimeControl.BLITZ_5,
                                onClick = { timeControl = TimeControl.BLITZ_5 },
                                modifier = Modifier.weight(1f)
                            )
                            TimeControlCard(
                                timeControl = TimeControl.RAPID_10,
                                label = "Rapid 10 min",
                                desc = "Standard",
                                isSelected = timeControl == TimeControl.RAPID_10,
                                onClick = { timeControl = TimeControl.RAPID_10 },
                                modifier = Modifier.weight(1f)
                            )
                            TimeControlCard(
                                timeControl = TimeControl.CLASSICAL_30,
                                label = "Classical",
                                desc = "30 min",
                                isSelected = timeControl == TimeControl.CLASSICAL_30,
                                onClick = { timeControl = TimeControl.CLASSICAL_30 },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    // Section 6: Sound Effects & Haptic Feedback
                    item {
                        SettingsSectionHeader(
                            icon = Icons.Default.VolumeUp,
                            title = "Sensory Feedback",
                            subtitle = "Audio cues and tactile vibrations"
                        )
                        Spacer(Modifier.height(10.dp))
                        Card(
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF334155)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)) {
                                SettingsSwitchRow(
                                    icon = Icons.Default.GraphicEq,
                                    title = "Sound Effects",
                                    subtitle = "Play movement, capture, check & victory audio",
                                    checked = sound,
                                    onCheckedChange = { sound = it }
                                )
                                HorizontalDivider(color = Color(0xFF334155), thickness = 0.8.dp)
                                SettingsSwitchRow(
                                    icon = Icons.Default.Vibration,
                                    title = "Haptic Feedback",
                                    subtitle = "Provide vibration feedback on supported devices",
                                    checked = haptics,
                                    onCheckedChange = { haptics = it }
                                )
                            }
                        }
                    }

                    // Section 7: Perspective & Effects
                    item {
                        SettingsSectionHeader(
                            icon = Icons.Default.ViewInAr,
                            title = "Perspective & Visuals",
                            subtitle = "3D depth and particle synchronization"
                        )
                        Spacer(Modifier.height(10.dp))
                        Card(
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF334155)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)) {
                                SettingsSwitchRow(
                                    icon = Icons.Default.Layers,
                                    title = "3D Isometric Board",
                                    subtitle = "Tilt the board perspective with dimensional elevation shadows",
                                    checked = is3D,
                                    onCheckedChange = { is3D = it }
                                )
                                HorizontalDivider(color = Color(0xFF334155), thickness = 0.8.dp)
                                SettingsSwitchRow(
                                    icon = Icons.Default.AutoAwesome,
                                    title = "Sync Theme Effects",
                                    subtitle = "Adapt capture impact shockwaves & particles to board theme",
                                    checked = syncEffects,
                                    onCheckedChange = { syncEffects = it }
                                )
                            }
                        }
                    }

                    // Section 8: Share App & Community
                    item {
                        SettingsSectionHeader(
                            icon = Icons.Default.Share,
                            title = "Share App",
                            subtitle = "Invite friends & challenge fellow chess enthusiasts"
                        )
                        Spacer(Modifier.height(10.dp))
                        Card(
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF334155)),
                            modifier = Modifier.fillMaxWidth().testTag("settings_share_app_card")
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Surface(
                                        color = Color(0xFF0284C7).copy(alpha = 0.2f),
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.size(44.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = Icons.Default.Share,
                                                contentDescription = null,
                                                tint = Color(0xFF38BDF8),
                                                modifier = Modifier.size(24.dp)
                                            )
                                        }
                                    }
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Grandmaster Chess",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp,
                                            color = Color.White
                                        )
                                        Text(
                                            text = "Share with all compatible apps including WhatsApp, Telegram, Gmail, Messages & Nearby Share.",
                                            fontSize = 12.sp,
                                            color = Color(0xFF94A3B8),
                                            lineHeight = 16.sp
                                        )
                                    }
                                }

                                Button(
                                    onClick = { ShareAppHelper.shareApp(context) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(44.dp)
                                        .testTag("settings_share_app_button"),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFF0284C7),
                                        contentColor = Color.White
                                    ),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Share,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        text = "Share App",
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 14.sp
                                    )
                                }
                            }
                        }
                    }
                }

                // Footer Actions Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF1E293B))
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = {
                            mode = GameMode.PVC
                            difficulty = AIDifficulty.MEDIUM
                            theme = BoardTheme.CLASSIC_WOOD
                            pieceSet = PieceSet.CLASSIC_STAUNTON
                            animSpeed = AnimationSpeed.NORMAL
                            sound = true
                            haptics = true
                            is3D = false
                            timeControl = TimeControl.UNLIMITED
                            syncEffects = true
                        },
                        modifier = Modifier.testTag("settings_reset_button")
                    ) {
                        Icon(Icons.Default.RestartAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Reset Defaults", fontSize = 13.sp, color = Color(0xFF94A3B8))
                    }

                    Button(
                        onClick = {
                            onSave(
                                currentSettings.copy(
                                    mode = mode,
                                    aiDifficulty = difficulty,
                                    boardTheme = theme,
                                    pieceSet = pieceSet,
                                    animationSpeed = animSpeed,
                                    soundEnabled = sound,
                                    hapticsEnabled = haptics,
                                    is3DView = is3D,
                                    timeControl = timeControl,
                                    syncThemeEffects = syncEffects
                                )
                            )
                            onDismiss()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.testTag("settings_apply_button")
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Apply & Close", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
}

@Composable
private fun SettingsSectionHeader(
    icon: ImageVector,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color(0xFF38BDF8),
            modifier = Modifier.size(18.dp)
        )
        Column {
            Text(
                text = title,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Text(
                text = subtitle,
                fontSize = 11.sp,
                color = Color(0xFF94A3B8)
            )
        }
    }
}

@Composable
private fun GameModeCard(
    title: String,
    description: String,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) Color(0xFF1E293B) else Color(0xFF131D31)
        ),
        border = androidx.compose.foundation.BorderStroke(
            width = if (isSelected) 2.dp else 1.dp,
            color = if (isSelected) Color(0xFF38BDF8) else Color(0xFF334155)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("mode_${if (isSelected) "selected" else "option"}")
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    color = if (isSelected) Color(0x3338BDF8) else Color(0xFF1E293B),
                    shape = CircleShape,
                    modifier = Modifier.size(36.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = if (isSelected) Color(0xFF38BDF8) else Color(0xFF94A3B8),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                Column {
                    Text(
                        text = title,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = if (isSelected) Color.White else Color(0xFFCBD5E1)
                    )
                    Text(
                        text = description,
                        fontSize = 11.sp,
                        color = Color(0xFF94A3B8),
                        lineHeight = 14.sp
                    )
                }
            }
            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Selected",
                    tint = Color(0xFF38BDF8),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun AIDifficultyCard(
    difficulty: AIDifficulty,
    title: String,
    subtitle: String,
    isSelected: Boolean,
    isEnabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) Color(0xFF1E293B) else Color(0xFF131D31)
        ),
        border = androidx.compose.foundation.BorderStroke(
            width = if (isSelected) 2.dp else 1.dp,
            color = if (isSelected) Color(0xFF38BDF8) else Color(0xFF334155)
        ),
        modifier = modifier
            .clickable(enabled = isEnabled) { onClick() }
            .testTag("difficulty_${difficulty.name}")
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = if (isSelected) Color(0xFF38BDF8) else Color.White
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = subtitle,
                fontSize = 10.sp,
                color = Color(0xFF94A3B8),
                lineHeight = 13.sp
            )
            if (isSelected) {
                Spacer(Modifier.height(6.dp))
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = Color(0xFF38BDF8),
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
private fun BoardThemeCard(
    theme: BoardTheme,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) Color(0xFF1E293B) else Color(0xFF131D31)
        ),
        border = androidx.compose.foundation.BorderStroke(
            width = if (isSelected) 2.dp else 1.dp,
            color = if (isSelected) Color(0xFF38BDF8) else Color(0xFF334155)
        ),
        modifier = modifier
            .clickable { onClick() }
            .testTag("settings_theme_${theme.name}")
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Miniature 2x2 board swatch
            Row(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .border(1.dp, Color(theme.boardBorder), RoundedCornerShape(6.dp))
            ) {
                Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
                    Box(modifier = Modifier.weight(1f).fillMaxWidth().background(Color(theme.lightSquare)))
                    Box(modifier = Modifier.weight(1f).fillMaxWidth().background(Color(theme.darkSquare)))
                }
                Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
                    Box(modifier = Modifier.weight(1f).fillMaxWidth().background(Color(theme.darkSquare)))
                    Box(modifier = Modifier.weight(1f).fillMaxWidth().background(Color(theme.lightSquare)))
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = theme.title,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isSelected) Color(0xFF38BDF8) else Color.White
                )
                Text(
                    text = theme.subtitle,
                    fontSize = 9.sp,
                    color = Color(0xFF94A3B8),
                    maxLines = 1
                )
            }

            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = Color(0xFF38BDF8),
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
private fun PieceSetCard(
    pieceSet: PieceSet,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) Color(0xFF1E293B) else Color(0xFF131D31)
        ),
        border = androidx.compose.foundation.BorderStroke(
            width = if (isSelected) 2.dp else 1.dp,
            color = if (isSelected) Color(0xFF38BDF8) else Color(0xFF334155)
        ),
        modifier = modifier
            .clickable { onClick() }
            .testTag("settings_pieceset_${pieceSet.name}")
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Live piece preview icons (White Knight + Black King)
            Row(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color(0xFF0F172A))
                    .border(1.dp, Color(0xFF334155), RoundedCornerShape(6.dp)),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(modifier = Modifier.size(18.dp)) {
                    ChessPieceView(type = PieceType.KNIGHT, color = PieceColor.WHITE, pieceSet = pieceSet)
                }
                Box(modifier = Modifier.size(18.dp)) {
                    ChessPieceView(type = PieceType.KING, color = PieceColor.BLACK, pieceSet = pieceSet)
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = pieceSet.title,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isSelected) Color(0xFF38BDF8) else Color.White
                )
                Text(
                    text = pieceSet.description,
                    fontSize = 9.sp,
                    color = Color(0xFF94A3B8),
                    maxLines = 1
                )
            }

            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = Color(0xFF38BDF8),
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
private fun AnimationSpeedCard(
    speed: AnimationSpeed,
    label: String,
    subLabel: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) Color(0xFF1E293B) else Color(0xFF131D31)
        ),
        border = androidx.compose.foundation.BorderStroke(
            width = if (isSelected) 2.dp else 1.dp,
            color = if (isSelected) Color(0xFF38BDF8) else Color(0xFF334155)
        ),
        modifier = modifier
            .clickable { onClick() }
            .testTag("speed_${speed.name}")
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = label,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = if (isSelected) Color(0xFF38BDF8) else Color.White
            )
            Text(
                text = subLabel,
                fontSize = 10.sp,
                color = Color(0xFF94A3B8)
            )
            if (isSelected) {
                Spacer(Modifier.height(4.dp))
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = Color(0xFF38BDF8),
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}

@Composable
private fun TimeControlCard(
    timeControl: TimeControl,
    label: String,
    desc: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) Color(0xFF1E293B) else Color(0xFF131D31)
        ),
        border = androidx.compose.foundation.BorderStroke(
            width = if (isSelected) 2.dp else 1.dp,
            color = if (isSelected) Color(0xFF38BDF8) else Color(0xFF334155)
        ),
        modifier = modifier
            .clickable { onClick() }
            .testTag("time_${timeControl.name}")
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = if (isSelected) Color(0xFF38BDF8) else Color.White,
                maxLines = 1
            )
            Text(
                text = desc,
                fontSize = 9.sp,
                color = Color(0xFF94A3B8)
            )
            if (isSelected) {
                Spacer(Modifier.height(3.dp))
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = Color(0xFF38BDF8),
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}

@Composable
private fun SettingsSwitchRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (checked) Color(0xFF38BDF8) else Color(0xFF64748B),
                modifier = Modifier.size(22.dp)
            )
            Column {
                Text(
                    text = title,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    color = Color.White
                )
                Text(
                    text = subtitle,
                    fontSize = 11.sp,
                    color = Color(0xFF94A3B8),
                    lineHeight = 14.sp
                )
            }
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = Color(0xFF0284C7),
                uncheckedThumbColor = Color(0xFF94A3B8),
                uncheckedTrackColor = Color(0xFF334155)
            )
        )
    }
}
