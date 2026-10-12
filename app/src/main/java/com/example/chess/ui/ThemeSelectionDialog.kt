package com.example.chess.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.chess.model.*

@Composable
fun ThemeSelectionDialog(
    currentSettings: GameSettings,
    onApplySettings: (GameSettings) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedBoardTheme by remember { mutableStateOf(currentSettings.boardTheme) }
    var selectedPieceSet by remember { mutableStateOf(currentSettings.pieceSet) }
    var syncEffects by remember { mutableStateOf(currentSettings.syncThemeEffects) }
    var selectedTab by remember { mutableStateOf(0) } // 0: Board Themes, 1: Piece Sets

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
                        onClick = { /* Consume clicks inside dialog */ }
                    )
                    .testTag("theme_selection_dialog")
            ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.Default.Palette, contentDescription = null, tint = Color(0xFF38BDF8), modifier = Modifier.size(22.dp))
                            Text(
                                text = "Chess Theme Studio",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                        Text(
                            text = "Customize board aesthetics & piece sets independently",
                            fontSize = 12.sp,
                            color = Color(0xFF94A3B8)
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color(0xFF94A3B8))
                    }
                }

                Spacer(Modifier.height(14.dp))

                // Live Interactive Miniature Preview Box (4x4 board)
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                    border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(selectedBoardTheme.boardBorder)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "LIVE PREVIEW",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF38BDF8),
                                letterSpacing = 1.sp
                            )
                            Text(
                                text = "${selectedBoardTheme.title} • ${selectedPieceSet.title}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFFE2E8F0)
                            )
                        }

                        Spacer(Modifier.height(8.dp))

                        // 4x4 Miniature Chessboard with real pieces and highlights
                        Box(
                            modifier = Modifier
                                .size(176.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .border(2.dp, Color(selectedBoardTheme.boardBorder), RoundedCornerShape(8.dp))
                        ) {
                            Column(modifier = Modifier.fillMaxSize()) {
                                for (r in 0..3) {
                                    Row(modifier = Modifier.weight(1f).fillMaxWidth()) {
                                        for (f in 0..3) {
                                            val isLight = (f + r) % 2 != 0
                                            val isSelected = f == 1 && r == 1
                                            val isLastMove = f == 2 && r == 2
                                            val isLegalTarget = f == 1 && r == 2

                                            val bg = when {
                                                isSelected -> Color(selectedBoardTheme.highlightColor)
                                                isLastMove -> Color(selectedBoardTheme.lastMoveColor)
                                                isLight -> Color(selectedBoardTheme.lightSquare)
                                                else -> Color(selectedBoardTheme.darkSquare)
                                            }

                                            Box(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .fillMaxHeight()
                                                    .background(bg),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                // Place sample pieces
                                                when {
                                                    f == 1 && r == 1 -> ChessPieceView(type = PieceType.KNIGHT, color = PieceColor.WHITE, pieceSet = selectedPieceSet, modifier = Modifier.fillMaxSize().padding(2.dp))
                                                    f == 2 && r == 0 -> ChessPieceView(type = PieceType.KING, color = PieceColor.WHITE, pieceSet = selectedPieceSet, modifier = Modifier.fillMaxSize().padding(2.dp))
                                                    f == 1 && r == 3 -> ChessPieceView(type = PieceType.QUEEN, color = PieceColor.BLACK, pieceSet = selectedPieceSet, modifier = Modifier.fillMaxSize().padding(2.dp))
                                                    f == 3 && r == 2 -> ChessPieceView(type = PieceType.PAWN, color = PieceColor.BLACK, pieceSet = selectedPieceSet, modifier = Modifier.fillMaxSize().padding(2.dp))
                                                }

                                                // Legal move dot preview
                                                if (isLegalTarget) {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(10.dp)
                                                            .clip(CircleShape)
                                                            .background(Color(selectedBoardTheme.legalMoveColor))
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(14.dp))

                // Navigation Tabs: Board Themes vs Piece Sets
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = Color(0xFF1E293B),
                    contentColor = Color(0xFF38BDF8),
                    modifier = Modifier.clip(RoundedCornerShape(12.dp))
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Icon(Icons.Default.GridOn, contentDescription = null, modifier = Modifier.size(16.dp))
                                Text("Board Themes (${BoardTheme.values().size})", fontWeight = FontWeight.SemiBold)
                            }
                        }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Icon(Icons.Default.Category, contentDescription = null, modifier = Modifier.size(16.dp))
                                Text("Piece Sets (${PieceSet.values().size})", fontWeight = FontWeight.SemiBold)
                            }
                        }
                    )
                }

                Spacer(Modifier.height(10.dp))

                // Main Content List
                Box(modifier = Modifier.weight(1f)) {
                    if (selectedTab == 0) {
                        // Board Themes Grid
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(2),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxSize().testTag("board_themes_grid")
                        ) {
                            items(BoardTheme.values()) { bTheme ->
                                val isSelected = selectedBoardTheme == bTheme
                                Card(
                                    shape = RoundedCornerShape(14.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (isSelected) Color(0xFF1E293B) else Color(0xFF131D31)
                                    ),
                                    border = androidx.compose.foundation.BorderStroke(
                                        width = if (isSelected) 2.5.dp else 1.dp,
                                        color = if (isSelected) Color(0xFF38BDF8) else Color(0xFF334155)
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            selectedBoardTheme = bTheme
                                            // Apply immediately without restarting game
                                            onApplySettings(currentSettings.copy(boardTheme = bTheme, pieceSet = selectedPieceSet, syncThemeEffects = syncEffects))
                                        }
                                        .testTag("theme_item_${bTheme.name}")
                                ) {
                                    Column(modifier = Modifier.padding(10.dp)) {
                                        // Miniature 2x2 board swatch
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(44.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                                .border(1.dp, Color(bTheme.boardBorder), RoundedCornerShape(8.dp))
                                        ) {
                                            Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
                                                Box(modifier = Modifier.weight(1f).fillMaxWidth().background(Color(bTheme.lightSquare)))
                                                Box(modifier = Modifier.weight(1f).fillMaxWidth().background(Color(bTheme.darkSquare)))
                                            }
                                            Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
                                                Box(modifier = Modifier.weight(1f).fillMaxWidth().background(Color(bTheme.darkSquare)))
                                                Box(modifier = Modifier.weight(1f).fillMaxWidth().background(Color(bTheme.lightSquare)))
                                            }
                                        }

                                        Spacer(Modifier.height(8.dp))

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = bTheme.title,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isSelected) Color(0xFF38BDF8) else Color.White
                                            )
                                            if (isSelected) {
                                                Icon(
                                                    imageVector = Icons.Default.CheckCircle,
                                                    contentDescription = "Selected",
                                                    tint = Color(0xFF38BDF8),
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        }
                                        Text(
                                            text = bTheme.subtitle,
                                            fontSize = 10.sp,
                                            color = Color(0xFF94A3B8),
                                            maxLines = 1
                                        )
                                    }
                                }
                            }
                        }
                    } else {
                        // Piece Sets Grid
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(2),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxSize().testTag("piece_sets_grid")
                        ) {
                            items(PieceSet.values()) { pSet ->
                                val isSelected = selectedPieceSet == pSet
                                Card(
                                    shape = RoundedCornerShape(14.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (isSelected) Color(0xFF1E293B) else Color(0xFF131D31)
                                    ),
                                    border = androidx.compose.foundation.BorderStroke(
                                        width = if (isSelected) 2.5.dp else 1.dp,
                                        color = if (isSelected) Color(0xFF38BDF8) else Color(0xFF334155)
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            selectedPieceSet = pSet
                                            // Apply immediately without restarting game
                                            onApplySettings(currentSettings.copy(boardTheme = selectedBoardTheme, pieceSet = pSet, syncThemeEffects = syncEffects))
                                        }
                                        .testTag("piece_set_${pSet.name}")
                                ) {
                                    Column(modifier = Modifier.padding(10.dp)) {
                                        // Piece preview box: White Knight + Black King
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(48.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(Color(0xFF0F172A))
                                                .border(1.dp, Color(0xFF334155), RoundedCornerShape(8.dp)),
                                            horizontalArrangement = Arrangement.SpaceEvenly,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Box(modifier = Modifier.size(34.dp)) {
                                                ChessPieceView(type = PieceType.KNIGHT, color = PieceColor.WHITE, pieceSet = pSet)
                                            }
                                            Box(modifier = Modifier.size(34.dp)) {
                                                ChessPieceView(type = PieceType.KING, color = PieceColor.BLACK, pieceSet = pSet)
                                            }
                                        }

                                        Spacer(Modifier.height(8.dp))

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = pSet.title,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isSelected) Color(0xFF38BDF8) else Color.White
                                            )
                                            if (isSelected) {
                                                Icon(
                                                    imageVector = Icons.Default.CheckCircle,
                                                    contentDescription = "Selected",
                                                    tint = Color(0xFF38BDF8),
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        }
                                        Text(
                                            text = pSet.description,
                                            fontSize = 10.sp,
                                            color = Color(0xFF94A3B8),
                                            maxLines = 2,
                                            lineHeight = 13.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(10.dp))

                // Bottom Controls: Sync effects toggle & Reset to Default & Done
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Reset to Default Button
                    TextButton(
                        onClick = {
                            selectedBoardTheme = BoardTheme.CLASSIC_WOOD
                            selectedPieceSet = PieceSet.CLASSIC_STAUNTON
                            syncEffects = true
                            onApplySettings(
                                currentSettings.copy(
                                    boardTheme = BoardTheme.CLASSIC_WOOD,
                                    pieceSet = PieceSet.CLASSIC_STAUNTON,
                                    syncThemeEffects = true
                                )
                            )
                        },
                        modifier = Modifier.testTag("reset_theme_button")
                    ) {
                        Icon(Icons.Default.RestartAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Reset Default", fontSize = 12.sp, color = Color(0xFF94A3B8))
                    }

                    // Done / Apply Button
                    Button(
                        onClick = {
                            onApplySettings(
                                currentSettings.copy(
                                    boardTheme = selectedBoardTheme,
                                    pieceSet = selectedPieceSet,
                                    syncThemeEffects = syncEffects
                                )
                            )
                            onDismiss()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.testTag("apply_theme_button")
                    ) {
                        Text("Apply & Close", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
