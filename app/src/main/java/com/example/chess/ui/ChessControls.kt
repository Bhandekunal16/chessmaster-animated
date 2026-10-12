package com.example.chess.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.chess.model.Move

@Composable
fun MoveHistoryRow(
    moves: List<Move>,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()

    LaunchedEffect(moves.size) {
        if (moves.isNotEmpty()) {
            listState.animateScrollToItem(moves.size - 1)
        }
    }

    Surface(
        color = Color(0xFF1E293B),
        shape = RoundedCornerShape(10.dp),
        modifier = modifier
            .fillMaxWidth()
            .height(44.dp)
            .padding(horizontal = 8.dp)
    ) {
        if (moves.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = "No moves played yet. White to begin.",
                    fontSize = 12.sp,
                    color = Color(0xFF64748B)
                )
            }
        } else {
            LazyRow(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                itemsIndexed(moves) { index, move ->
                    val moveNum = (index / 2) + 1
                    val isWhiteMove = index % 2 == 0

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (isWhiteMove) {
                            Text(
                                text = "$moveNum.",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF94A3B8),
                                modifier = Modifier.padding(end = 4.dp)
                            )
                        }

                        Surface(
                            color = if (index == moves.lastIndex) Color(0xFF0284C7) else Color(0xFF0F172A),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = move.san,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                fontFamily = FontFamily.Monospace,
                                color = if (index == moves.lastIndex) Color.White else Color(0xFFE2E8F0),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ChessActionBar(
    canUndo: Boolean,
    canRedo: Boolean,
    isThinking: Boolean,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onHint: () -> Unit,
    onThemes: () -> Unit,
    onFlip: () -> Unit,
    onNewGame: () -> Unit,
    onSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Undo
        IconButton(
            onClick = onUndo,
            enabled = canUndo && !isThinking,
            modifier = Modifier.size(44.dp).testTag("undo_button")
        ) {
            Icon(
                imageVector = Icons.Default.Undo,
                contentDescription = "Undo Move",
                tint = if (canUndo && !isThinking) Color(0xFFE2E8F0) else Color(0xFF475569)
            )
        }

        // Redo
        IconButton(
            onClick = onRedo,
            enabled = canRedo && !isThinking,
            modifier = Modifier.size(44.dp).testTag("redo_button")
        ) {
            Icon(
                imageVector = Icons.Default.Redo,
                contentDescription = "Redo Move",
                tint = if (canRedo && !isThinking) Color(0xFFE2E8F0) else Color(0xFF475569)
            )
        }

        // Hint (AI suggestion)
        IconButton(
            onClick = onHint,
            enabled = !isThinking,
            modifier = Modifier.size(44.dp).testTag("hint_button")
        ) {
            Icon(
                imageVector = Icons.Default.Lightbulb,
                contentDescription = "Get Hint",
                tint = if (!isThinking) Color(0xFFFBBF24) else Color(0xFF475569)
            )
        }

        // Themes Studio
        IconButton(
            onClick = onThemes,
            modifier = Modifier.size(44.dp).testTag("themes_button")
        ) {
            Icon(
                imageVector = Icons.Default.Palette,
                contentDescription = "Theme Studio",
                tint = Color(0xFF38BDF8)
            )
        }

        // Flip Board
        IconButton(
            onClick = onFlip,
            modifier = Modifier.size(44.dp).testTag("flip_button")
        ) {
            Icon(
                imageVector = Icons.Default.Rotate90DegreesCw,
                contentDescription = "Flip Board",
                tint = Color(0xFFE2E8F0)
            )
        }

        // Restart / New Game
        IconButton(
            onClick = onNewGame,
            modifier = Modifier.size(44.dp).testTag("new_game_button")
        ) {
            Icon(
                imageVector = Icons.Default.RestartAlt,
                contentDescription = "New Game",
                tint = Color(0xFFE2E8F0)
            )
        }

        // Settings
        IconButton(
            onClick = onSettings,
            modifier = Modifier.size(44.dp).testTag("settings_button")
        ) {
            Icon(
                imageVector = Icons.Default.Settings,
                contentDescription = "Settings",
                tint = Color(0xFFE2E8F0)
            )
        }
    }
}
