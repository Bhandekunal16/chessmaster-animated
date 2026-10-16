package com.example.chess.multiplayer.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Handshake
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun MultiplayerDisconnectionDialog(
    reason: String,
    onRetry: () -> Unit,
    onLeaveMatch: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onLeaveMatch,
        shape = RoundedCornerShape(16.dp),
        containerColor = Color(0xFF0F172A),
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFF59E0B))
                Text("Connection Interrupted", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            }
        },
        text = {
            Text(
                text = "Wi-Fi connection with opponent was lost ($reason). The current board state has been preserved.",
                color = Color(0xFFCBD5E1),
                fontSize = 14.sp
            )
        },
        confirmButton = {
            Button(
                onClick = onRetry,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.testTag("multiplayer_reconnect_button")
            ) {
                Text("Retry / Reconnect", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onLeaveMatch,
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, Color(0xFF64748B)),
                modifier = Modifier.testTag("multiplayer_leave_button")
            ) {
                Text("Leave to Menu", color = Color(0xFF94A3B8))
            }
        }
    )
}

@Composable
fun DrawOfferDialog(
    onAccept: () -> Unit,
    onDecline: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDecline,
        shape = RoundedCornerShape(16.dp),
        containerColor = Color(0xFF0F172A),
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Default.Handshake, contentDescription = null, tint = Color(0xFF38BDF8))
                Text("Draw Offered", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            }
        },
        text = {
            Text(
                text = "Your opponent has offered a draw. Do you agree to end the match in a draw?",
                color = Color(0xFFCBD5E1),
                fontSize = 14.sp
            )
        },
        confirmButton = {
            Button(
                onClick = onAccept,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.testTag("accept_draw_dialog_button")
            ) {
                Text("Accept Draw", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDecline,
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, Color(0xFF64748B)),
                modifier = Modifier.testTag("decline_draw_dialog_button")
            ) {
                Text("Decline", color = Color(0xFF94A3B8))
            }
        }
    )
}
