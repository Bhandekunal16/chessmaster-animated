package com.example.chess.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.chess.model.*

@Composable
fun PawnPromotionDialog(
    color: PieceColor,
    pieceSet: PieceSet = PieceSet.CLASSIC_STAUNTON,
    onSelect: (PieceType) -> Unit
) {
    Dialog(onDismissRequest = { /* Must select */ }) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
            border = androidx.compose.foundation.BorderStroke(2.dp, Color(0xFF38BDF8)),
            modifier = Modifier.padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Pawn Promotion",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFF8FAFC)
                )
                Text(
                    text = "Choose a piece to empower your pawn",
                    fontSize = 13.sp,
                    color = Color(0xFF94A3B8),
                    modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val options = listOf(
                        PieceType.QUEEN to "Queen",
                        PieceType.KNIGHT to "Knight",
                        PieceType.ROOK to "Rook",
                        PieceType.BISHOP to "Bishop"
                    )

                    options.forEach { (type, name) ->
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFF0F172A))
                                .border(1.dp, Color(0xFF475569), RoundedCornerShape(12.dp))
                                .clickable { onSelect(type) }
                                .padding(vertical = 12.dp, horizontal = 4.dp)
                                .testTag("promote_${type.name}")
                        ) {
                            Box(modifier = Modifier.size(48.dp)) {
                                ChessPieceView(type = type, color = color, pieceSet = pieceSet)
                            }
                            Text(
                                text = name,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFFE2E8F0),
                                modifier = Modifier.padding(top = 6.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun GameOverDialog(
    gameState: GameState,
    onRematch: () -> Unit,
    onDismiss: () -> Unit,
    onExportPgn: () -> Unit
) {
    val title: String
    val description: String
    val iconColor: Color

    when {
        gameState.isCheckmate -> {
            val winnerName = if (gameState.winner == PieceColor.WHITE) "White" else "Black"
            title = "Checkmate!"
            description = "$winnerName claims the victory."
            iconColor = Color(0xFFF59E0B)
        }
        gameState.isStalemate -> {
            title = "Stalemate"
            description = "The player has no legal moves and is not in check. Game drawn."
            iconColor = Color(0xFF38BDF8)
        }
        gameState.isDraw -> {
            title = "Draw"
            description = gameState.drawReason ?: "Game drawn."
            iconColor = Color(0xFF94A3B8)
        }
        else -> return
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
            border = androidx.compose.foundation.BorderStroke(2.dp, iconColor),
            modifier = Modifier.padding(16.dp).testTag("game_over_dialog")
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(iconColor.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (gameState.isCheckmate) Icons.Default.EmojiEvents else Icons.Default.Handshake,
                        contentDescription = null,
                        tint = iconColor,
                        modifier = Modifier.size(36.dp)
                    )
                }

                Text(
                    text = title,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.padding(top = 16.dp)
                )

                Text(
                    text = description,
                    fontSize = 14.sp,
                    color = Color(0xFFCBD5E1),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 8.dp, bottom = 20.dp)
                )

                // Stats summary
                Surface(
                    color = Color(0xFF0F172A),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 20.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Total Moves", fontSize = 11.sp, color = Color(0xFF94A3B8))
                            Text("${gameState.moveHistory.size}", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Result", fontSize = 11.sp, color = Color(0xFF94A3B8))
                            Text(
                                when {
                                    gameState.winner == PieceColor.WHITE -> "1 - 0"
                                    gameState.winner == PieceColor.BLACK -> "0 - 1"
                                    else -> "½ - ½"
                                },
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = iconColor
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Review", color = Color(0xFF94A3B8))
                    }
                    Button(
                        onClick = onRematch,
                        modifier = Modifier.weight(1f).testTag("rematch_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
                    ) {
                        Text("Rematch", fontWeight = FontWeight.Bold)
                    }
                }

                TextButton(
                    onClick = onExportPgn,
                    modifier = Modifier.padding(top = 8.dp)
                ) {
                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Export PGN", fontSize = 13.sp, color = Color(0xFF38BDF8))
                }
            }
        }
    }
}

@Composable
fun PgnDialog(
    pgnText: String,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
            modifier = Modifier.padding(16.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "PGN Game Export",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                Surface(
                    color = Color(0xFF0F172A),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .padding(vertical = 12.dp)
                ) {
                    LazyColumn(modifier = Modifier.padding(10.dp)) {
                        item {
                            Text(
                                text = pgnText,
                                fontSize = 12.sp,
                                color = Color(0xFFCBD5E1),
                                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Close", color = Color(0xFF94A3B8))
                    }
                    Button(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clip = ClipData.newPlainText("Chess PGN", pgnText)
                            clipboard.setPrimaryClip(clip)
                            Toast.makeText(context, "PGN copied to clipboard!", Toast.LENGTH_SHORT).show()
                            onDismiss()
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7))
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Copy PGN")
                    }
                }
            }
        }
    }
}
