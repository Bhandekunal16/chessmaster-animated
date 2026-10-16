package com.example.chess.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.chess.model.AIDifficulty
import com.example.chess.model.GameMode
import com.example.chess.model.GameSettings

/**
 * Top App Bar for Grandmaster Chess.
 *
 * Implements a responsive, single-line Material 3 toolbar with:
 * - Single-line app title that truncates gracefully on ultra-narrow screens without pushing controls.
 * - Non-wrapping Difficulty Selector pill with a dropdown menu to change difficulty or game mode.
 * - Evenly distributed, vertically aligned action buttons with accessible touch targets.
 */
@Composable
fun GrandmasterTopAppBar(
    settings: GameSettings,
    onDifficultySelected: (AIDifficulty) -> Unit,
    onModeSelected: (GameMode) -> Unit,
    onThemeClick: () -> Unit,
    onPgnClick: () -> Unit,
    onShareClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onOpenMultiplayer: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Surface(
        color = Color(0xFF0F172A),
        tonalElevation = 2.dp,
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(start = 12.dp, end = 4.dp, top = 6.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Left Title + Difficulty area using weight(1f) to occupy available width
            Row(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "Grandmaster Chess",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFF8FAFC),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    softWrap = false,
                    modifier = Modifier.weight(1f, fill = false)
                )

                DifficultySelector(
                    currentDifficulty = settings.aiDifficulty,
                    isPVC = settings.mode == GameMode.PVC,
                    gameMode = settings.mode,
                    onDifficultySelected = onDifficultySelected,
                    onModeSelected = onModeSelected,
                    onOpenMultiplayer = onOpenMultiplayer
                )
            }

            // Toolbar Action Buttons (Themes, PGN Export, Share App, Game Settings)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(1.dp)
            ) {
                ToolbarActionButton(
                    icon = Icons.Default.Palette,
                    contentDescription = "Theme Studio",
                    tint = Color(0xFF38BDF8),
                    testTag = "top_themes_button",
                    onClick = onThemeClick
                )
                ToolbarActionButton(
                    icon = Icons.Default.Description,
                    contentDescription = "PGN Game Export",
                    tint = Color(0xFF94A3B8),
                    testTag = "pgn_button",
                    onClick = onPgnClick
                )
                ToolbarActionButton(
                    icon = Icons.Default.Share,
                    contentDescription = "Share App",
                    tint = Color(0xFF38BDF8),
                    testTag = "top_share_app_button",
                    onClick = onShareClick
                )
                ToolbarActionButton(
                    icon = Icons.Default.Settings,
                    contentDescription = "Game Settings",
                    tint = Color(0xFF94A3B8),
                    testTag = "top_settings_button",
                    onClick = onSettingsClick
                )
            }
        }
    }
}

@Composable
fun DifficultySelector(
    currentDifficulty: AIDifficulty,
    isPVC: Boolean,
    gameMode: GameMode = GameMode.PVC,
    onDifficultySelected: (AIDifficulty) -> Unit,
    onModeSelected: (GameMode) -> Unit,
    onOpenMultiplayer: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    val badgeText = when (gameMode) {
        GameMode.LOCAL_MULTIPLAYER -> "Wi-Fi Match"
        GameMode.PVC -> currentDifficulty.label
        GameMode.PVP -> "2 Players"
    }

    val badgeColor = when (gameMode) {
        GameMode.LOCAL_MULTIPLAYER -> Color(0xFF2563EB)
        GameMode.PVC -> Color(0xFF0284C7)
        GameMode.PVP -> Color(0xFF059669)
    }

    Box(modifier = modifier.wrapContentSize()) {
        Surface(
            onClick = { expanded = true },
            shape = RoundedCornerShape(6.dp),
            color = badgeColor,
            modifier = Modifier.testTag("top_difficulty_selector")
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(3.dp),
                modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
            ) {
                if (gameMode == GameMode.LOCAL_MULTIPLAYER) {
                    Icon(
                        imageVector = Icons.Default.Wifi,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(13.dp)
                    )
                }
                Text(
                    text = badgeText,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 1,
                    softWrap = false
                )
                Icon(
                    imageVector = Icons.Default.ArrowDropDown,
                    contentDescription = "Select Mode or Difficulty",
                    tint = Color.White.copy(alpha = 0.9f),
                    modifier = Modifier.size(15.dp)
                )
            }
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier
                .background(Color(0xFF1E293B))
                .testTag("difficulty_dropdown_menu")
        ) {
            Text(
                text = "Match Mode & AI",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF94A3B8),
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
            )
            AIDifficulty.entries.forEach { diff ->
                val isSelected = isPVC && currentDifficulty == diff
                DropdownMenuItem(
                    text = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = diff.label,
                                color = if (isSelected) Color(0xFF38BDF8) else Color.White,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                fontSize = 13.sp,
                                maxLines = 1,
                                softWrap = false
                            )
                            if (isSelected) {
                                Spacer(Modifier.weight(1f))
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = Color(0xFF38BDF8),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    },
                    onClick = {
                        onModeSelected(GameMode.PVC)
                        onDifficultySelected(diff)
                        expanded = false
                    },
                    modifier = Modifier.testTag("difficulty_option_${diff.name}")
                )
            }
            HorizontalDivider(color = Color(0xFF334155), thickness = 0.8.dp)
            DropdownMenuItem(
                text = {
                    val isSelected = gameMode == GameMode.PVP
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "2 Players (Pass & Play)",
                            color = if (isSelected) Color(0xFF38BDF8) else Color.White,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            fontSize = 13.sp,
                            maxLines = 1,
                            softWrap = false
                        )
                        if (isSelected) {
                            Spacer(Modifier.weight(1f))
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = Color(0xFF38BDF8),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                },
                onClick = {
                    onModeSelected(GameMode.PVP)
                    expanded = false
                },
                modifier = Modifier.testTag("mode_option_pvp")
            )
            HorizontalDivider(color = Color(0xFF334155), thickness = 0.8.dp)
            DropdownMenuItem(
                text = {
                    val isSelected = gameMode == GameMode.LOCAL_MULTIPLAYER
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Wifi,
                            contentDescription = null,
                            tint = if (isSelected) Color(0xFF38BDF8) else Color(0xFF94A3B8),
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "Local Wi-Fi Match...",
                            color = if (isSelected) Color(0xFF38BDF8) else Color.White,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            fontSize = 13.sp,
                            maxLines = 1,
                            softWrap = false
                        )
                        if (isSelected) {
                            Spacer(Modifier.weight(1f))
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = Color(0xFF38BDF8),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                },
                onClick = {
                    expanded = false
                    onOpenMultiplayer?.invoke()
                },
                modifier = Modifier.testTag("mode_option_multiplayer")
            )
        }
    }
}

@Composable
private fun ToolbarActionButton(
    icon: ImageVector,
    contentDescription: String,
    tint: Color,
    testTag: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    IconButton(
        onClick = onClick,
        modifier = modifier
            .size(36.dp)
            .testTag(testTag)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = tint,
            modifier = Modifier.size(19.dp)
        )
    }
}
