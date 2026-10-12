package com.example.chess.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.chess.model.*

@Composable
fun PlayerCard(
    playerColor: PieceColor,
    playerName: String,
    isAI: Boolean,
    aiDifficulty: AIDifficulty?,
    isThinking: Boolean,
    isCurrentTurn: Boolean,
    timeRemainingSeconds: Int,
    capturedPieces: List<Piece>,
    materialAdvantage: Int, // e.g. +3
    pieceSet: PieceSet = PieceSet.CLASSIC_STAUNTON,
    modifier: Modifier = Modifier
) {
    val isWhite = playerColor == PieceColor.WHITE
    val activeBorderColor = if (isCurrentTurn) Color(0xFF10B981) else Color(0xFF334155)

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(700, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "turn_alpha"
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .border(
                width = if (isCurrentTurn) 2.dp else 1.dp,
                color = if (isCurrentTurn) activeBorderColor.copy(alpha = pulseAlpha) else activeBorderColor,
                shape = RoundedCornerShape(12.dp)
            ),
        colors = CardDefaults.cardColors(
            containerColor = if (isCurrentTurn) Color(0xFF1E293B) else Color(0xFF111827)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Player Avatar & Details
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Color Badge + Avatar
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(if (isWhite) Color(0xFFF8FAFC) else Color(0xFF0F172A))
                        .border(1.5.dp, Color(0xFF64748B), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isAI) Icons.Default.Computer else Icons.Default.Person,
                        contentDescription = null,
                        tint = if (isWhite) Color(0xFF0F172A) else Color(0xFFF8FAFC),
                        modifier = Modifier.size(20.dp)
                    )
                }

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = playerName,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = Color(0xFFF8FAFC)
                        )
                        if (isAI && aiDifficulty != null) {
                            Surface(
                                color = when (aiDifficulty) {
                                    AIDifficulty.EASY -> Color(0xFF0284C7)
                                    AIDifficulty.MEDIUM -> Color(0xFFD97706)
                                    AIDifficulty.HARD -> Color(0xFFDC2626)
                                },
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = aiDifficulty.label,
                                    fontSize = 10.sp,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                )
                            }
                        }
                    }

                    // Captured Pieces Tray & Material Delta
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                        modifier = Modifier.padding(top = 2.dp)
                    ) {
                        // Display mini icons for captured pieces
                        val grouped = capturedPieces.groupBy { it.type }
                        for (type in listOf(PieceType.PAWN, PieceType.KNIGHT, PieceType.BISHOP, PieceType.ROOK, PieceType.QUEEN)) {
                            val count = grouped[type]?.size ?: 0
                            if (count > 0) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(modifier = Modifier.size(16.dp)) {
                                        ChessPieceView(
                                            type = type,
                                            color = playerColor.opposite(),
                                            pieceSet = pieceSet
                                        )
                                    }
                                    if (count > 1) {
                                        Text(
                                            text = count.toString(),
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF94A3B8)
                                        )
                                    }
                                }
                            }
                        }

                        if (materialAdvantage > 0) {
                            Text(
                                text = "+$materialAdvantage",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF38BDF8),
                                modifier = Modifier.padding(start = 4.dp)
                            )
                        }

                        if (isThinking) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(2.dp),
                                modifier = Modifier.padding(start = 6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Psychology,
                                    contentDescription = "Thinking",
                                    tint = Color(0xFF38BDF8),
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = "Analyzing...",
                                    fontSize = 11.sp,
                                    color = Color(0xFF38BDF8),
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            }

            // Timer Clock
            if (timeRemainingSeconds > 0) {
                val minutes = timeRemainingSeconds / 60
                val seconds = timeRemainingSeconds % 60
                val timeString = "%02d:%02d".format(minutes, seconds)
                val isLowTime = timeRemainingSeconds <= 30

                Surface(
                    color = when {
                        isLowTime && isCurrentTurn -> Color(0xFF7F1D1D)
                        isCurrentTurn -> Color(0xFF0F766E)
                        else -> Color(0xFF1E293B)
                    },
                    shape = RoundedCornerShape(8.dp),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (isCurrentTurn) Color(0xFF34D399) else Color(0xFF475569)
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Timer,
                            contentDescription = null,
                            tint = if (isCurrentTurn) Color(0xFFF8FAFC) else Color(0xFF94A3B8),
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = timeString,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = if (isLowTime && isCurrentTurn) Color(0xFFFCA5A5) else Color(0xFFF8FAFC)
                        )
                    }
                }
            } else if (isCurrentTurn) {
                Surface(
                    color = Color(0x3310B981),
                    shape = RoundedCornerShape(8.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF10B981))
                ) {
                    Text(
                        text = "TURN",
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        color = Color(0xFF34D399),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }
    }
}
