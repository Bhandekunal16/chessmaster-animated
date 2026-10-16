package com.example.chess.multiplayer.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.chess.multiplayer.model.DiscoveredHost
import com.example.chess.multiplayer.model.MultiplayerRole
import com.example.chess.multiplayer.model.MultiplayerSessionState
import com.example.chess.multiplayer.model.MultiplayerStatus

@Composable
fun LocalMultiplayerDialog(
    sessionState: MultiplayerSessionState,
    discoveredHosts: List<DiscoveredHost>,
    onStartHosting: () -> Unit,
    onCancelHosting: () -> Unit,
    onJoinGame: (address: String) -> Unit,
    onStartDiscovery: () -> Unit,
    onStopDiscovery: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableStateOf(if (sessionState.role == MultiplayerRole.CLIENT) 1 else 0) }
    var inputAddress by remember { mutableStateOf("") }
    val context = LocalContext.current

    LaunchedEffect(selectedTab) {
        if (selectedTab == 1) {
            onStartDiscovery()
        } else {
            onStopDiscovery()
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            onStopDiscovery()
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = modifier
                .fillMaxWidth(0.92f)
                .fillMaxHeight(0.85f)
                .testTag("multiplayer_dialog"),
            shape = RoundedCornerShape(20.dp),
            color = Color(0xFF0F172A),
            tonalElevation = 8.dp,
            border = BorderStroke(1.dp, Color(0xFF334155))
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
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFF0284C7).copy(alpha = 0.2f),
                            modifier = Modifier.size(38.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Wifi,
                                    contentDescription = null,
                                    tint = Color(0xFF38BDF8),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Column {
                            Text(
                                text = "Wi-Fi Multiplayer",
                                fontSize = 19.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFF8FAFC)
                            )
                            Text(
                                text = "Play peer-to-peer on the same Wi-Fi",
                                fontSize = 12.sp,
                                color = Color(0xFF94A3B8)
                            )
                        }
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("close_multiplayer_dialog")
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color(0xFFCBD5E1))
                    }
                }

                Spacer(Modifier.height(16.dp))

                // Tabs: Host Game / Join Game
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = Color(0xFF1E293B),
                    contentColor = Color(0xFF38BDF8),
                    indicator = { tabPositions ->
                        TabRowDefaults.SecondaryIndicator(
                            Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                            color = Color(0xFF38BDF8)
                        )
                    },
                    divider = {},
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = {
                            Text(
                                "Host Match (White)",
                                fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal,
                                color = if (selectedTab == 0) Color(0xFF38BDF8) else Color(0xFF94A3B8)
                            )
                        },
                        modifier = Modifier.testTag("host_game_tab")
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = {
                            Text(
                                "Join Match (Black)",
                                fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal,
                                color = if (selectedTab == 1) Color(0xFF38BDF8) else Color(0xFF94A3B8)
                            )
                        },
                        modifier = Modifier.testTag("join_game_tab")
                    )
                }

                Spacer(Modifier.height(16.dp))

                // Error Banner
                if (sessionState.errorMessage != null) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFF7F1D1D).copy(alpha = 0.4f),
                        border = BorderStroke(1.dp, Color(0xFFEF4444)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.padding(12.dp)
                        ) {
                            Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = Color(0xFFF87171))
                            Text(
                                text = sessionState.errorMessage,
                                fontSize = 12.sp,
                                color = Color(0xFFFECACA),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                // Tab Content
                if (selectedTab == 0) {
                    HostTabContent(
                        sessionState = sessionState,
                        onStartHosting = onStartHosting,
                        onCancelHosting = onCancelHosting,
                        onCopy = { text, label ->
                            val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            cm.setPrimaryClip(ClipData.newPlainText(label, text))
                            Toast.makeText(context, "Copied $label: $text", Toast.LENGTH_SHORT).show()
                        }
                    )
                } else {
                    JoinTabContent(
                        sessionState = sessionState,
                        discoveredHosts = discoveredHosts,
                        inputAddress = inputAddress,
                        onAddressChange = { inputAddress = it },
                        onJoin = { onJoinGame(it) }
                    )
                }
            }
        }
    }
}

@Composable
private fun HostTabContent(
    sessionState: MultiplayerSessionState,
    onStartHosting: () -> Unit,
    onCancelHosting: () -> Unit,
    onCopy: (String, String) -> Unit
) {
    val isHosting = sessionState.status == MultiplayerStatus.HOSTING_LISTENING ||
            sessionState.status == MultiplayerStatus.CONNECTED ||
            sessionState.status == MultiplayerStatus.GAME_IN_PROGRESS

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        if (!isHosting) {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                border = BorderStroke(1.dp, Color(0xFF334155)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Host a Chess Match",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "Your device will act as the host server and play White. The other device will connect over Wi-Fi and play Black.",
                        fontSize = 13.sp,
                        color = Color(0xFF94A3B8)
                    )
                    Spacer(Modifier.height(8.dp))
                    Button(
                        onClick = onStartHosting,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("start_hosting_button")
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Create Game Session", fontWeight = FontWeight.Bold)
                    }
                }
            }
        } else {
            // Hosting Active
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                border = BorderStroke(1.dp, Color(0xFF0284C7).copy(alpha = 0.6f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CircularProgressIndicator(
                            color = Color(0xFF38BDF8),
                            strokeWidth = 2.5.dp,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = if (sessionState.status == MultiplayerStatus.HOSTING_LISTENING)
                                "Waiting for Player 2 to join..." else "Player 2 Connected!",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (sessionState.status == MultiplayerStatus.HOSTING_LISTENING)
                                Color(0xFF38BDF8) else Color(0xFF34D399)
                        )
                    }

                    HorizontalDivider(color = Color(0xFF334155))

                    // Room Code Box
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("ROOM CODE", fontSize = 11.sp, color = Color(0xFF94A3B8), fontWeight = FontWeight.SemiBold)
                            Text(sessionState.roomCode, fontSize = 22.sp, color = Color(0xFF38BDF8), fontWeight = FontWeight.ExtraBold)
                        }
                        OutlinedButton(
                            onClick = { onCopy(sessionState.roomCode, "Room Code") },
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, Color(0xFF38BDF8)),
                            modifier = Modifier.testTag("copy_room_code_button")
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = null, tint = Color(0xFF38BDF8), modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Copy", color = Color(0xFF38BDF8), fontSize = 12.sp)
                        }
                    }

                    // Local IP & Port Box
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("HOST IP & PORT", fontSize = 11.sp, color = Color(0xFF94A3B8), fontWeight = FontWeight.SemiBold)
                            Text("${sessionState.localIp}:${sessionState.port}", fontSize = 15.sp, color = Color.White, fontWeight = FontWeight.Medium)
                        }
                        OutlinedButton(
                            onClick = { onCopy("${sessionState.localIp}:${sessionState.port}", "Host Address") },
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, Color(0xFF64748B))
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = null, tint = Color(0xFFCBD5E1), modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Copy", color = Color(0xFFCBD5E1), fontSize = 12.sp)
                        }
                    }

                    Text(
                        text = "Broadcasting session on local Wi-Fi. Other devices in 'Join Match' can discover this game automatically, or join by entering the Room Code / IP.",
                        fontSize = 11.5.sp,
                        color = Color(0xFF64748B)
                    )

                    Button(
                        onClick = onCancelHosting,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF334155)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .testTag("cancel_hosting_button")
                    ) {
                        Text("Cancel Session", color = Color(0xFFF1F5F9))
                    }
                }
            }
        }
    }
}

@Composable
private fun JoinTabContent(
    sessionState: MultiplayerSessionState,
    discoveredHosts: List<DiscoveredHost>,
    inputAddress: String,
    onAddressChange: (String) -> Unit,
    onJoin: (String) -> Unit
) {
    val isConnecting = sessionState.status == MultiplayerStatus.CLIENT_CONNECTING

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Discovered Matches Section
        Text(
            text = "Nearby Games on Wi-Fi",
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFFF8FAFC)
        )

        if (discoveredHosts.isEmpty()) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = Color(0xFF1E293B),
                border = BorderStroke(1.dp, Color(0xFF334155)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.padding(14.dp)
                ) {
                    CircularProgressIndicator(
                        color = Color(0xFF38BDF8),
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "Searching local network for hosts...",
                        fontSize = 12.5.sp,
                        color = Color(0xFF94A3B8)
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 140.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(discoveredHosts) { host ->
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFF1E293B),
                        border = BorderStroke(1.dp, Color(0xFF0284C7)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onJoin("${host.ip}:${host.port}") }
                            .testTag("discovered_host_${host.roomCode}")
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(host.hostName, fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
                                Text("${host.roomCode} • ${host.ip}:${host.port}", color = Color(0xFF38BDF8), fontSize = 12.sp)
                            }
                            Button(
                                onClick = { onJoin("${host.ip}:${host.port}") },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                            ) {
                                Text("Join", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }

        HorizontalDivider(color = Color(0xFF334155))

        // Manual Join Section
        Text(
            text = "Or Enter Address Manually",
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFFF8FAFC)
        )

        OutlinedTextField(
            value = inputAddress,
            onValueChange = onAddressChange,
            label = { Text("Room Code or Host IP (e.g. 192.168.1.50)") },
            placeholder = { Text("CHESS-1042 or 192.168.1.50") },
            singleLine = true,
            shape = RoundedCornerShape(10.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color(0xFFCBD5E1),
                focusedBorderColor = Color(0xFF38BDF8),
                unfocusedBorderColor = Color(0xFF475569)
            ),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("join_ip_input")
        )

        Button(
            onClick = { onJoin(inputAddress) },
            enabled = inputAddress.isNotBlank() && !isConnecting,
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("connect_button")
        ) {
            if (isConnecting) {
                CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Connecting...")
            } else {
                Icon(Icons.Default.Login, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Connect to Match", fontWeight = FontWeight.Bold)
            }
        }
    }
}
