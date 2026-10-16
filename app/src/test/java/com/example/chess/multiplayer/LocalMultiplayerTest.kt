package com.example.chess.multiplayer

import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.chess.domain.model.BoardPosition
import com.example.chess.engine.ChessEngine
import com.example.chess.engine.GameController
import com.example.chess.model.*
import com.example.chess.multiplayer.discovery.NetworkDiscoveryHelper
import com.example.chess.multiplayer.model.*
import com.example.chess.multiplayer.network.MultiplayerClient
import com.example.chess.multiplayer.network.MultiplayerServer
import com.example.chess.repository.SharedPreferencesSettingsRepository
import com.example.chess.viewmodel.ChessViewModel
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.net.ServerSocket
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class LocalMultiplayerTest {

    // ==================== Protocol & Serialization Tests ====================

    @Test
    fun testMessageSerializationAndDeserialization() {
        val original = MultiplayerMessage(
            version = 1,
            sessionId = "test-session-123",
            type = MultiplayerMessageType.MOVE,
            sequenceNumber = 42L,
            payload = "{\"ff\":4,\"fr\":1,\"tf\":4,\"tr\":3}"
        )

        val serialized = MultiplayerMessageSerializer.serialize(original)
        assertNotNull(serialized)
        assertTrue(serialized.contains("test-session-123"))
        assertTrue(serialized.contains("MOVE"))

        val deserialized = MultiplayerMessageSerializer.deserialize(serialized)
        assertNotNull(deserialized)
        assertEquals(original.version, deserialized!!.version)
        assertEquals(original.sessionId, deserialized.sessionId)
        assertEquals(original.type, deserialized.type)
        assertEquals(original.sequenceNumber, deserialized.sequenceNumber)
        assertEquals(original.payload, deserialized.payload)
    }

    @Test
    fun testOversizedMessageRejected() {
        val largePayload = "x".repeat(MultiplayerMessage.MAX_MESSAGE_SIZE + 10)
        val deserialized = MultiplayerMessageSerializer.deserialize(largePayload)
        assertNull("Oversized messages must be safely rejected", deserialized)
    }

    @Test
    fun testMalformedJsonMessageReturnsNull() {
        val garbage = "Not a json string at all {[[}}"
        val deserialized = MultiplayerMessageSerializer.deserialize(garbage)
        assertNull("Malformed JSON should return null without crashing", deserialized)
    }

    // ==================== Network Discovery & Room Code Tests ====================

    @Test
    fun testRoomCodeGenerationAndResolution() {
        val ip = "192.168.1.45"
        val code = NetworkDiscoveryHelper.generateRoomCode(ip)
        assertTrue(code.startsWith("CHESS-"))

        val resolved = NetworkDiscoveryHelper.resolveTargetAddress(code, localIp = "192.168.1.10")
        assertNotNull("Subnet room code must resolve against local subnet", resolved)
        assertEquals("192.168.1.45", resolved!!.first)
        assertEquals(NetworkDiscoveryHelper.DEFAULT_PORT, resolved.second)
    }

    @Test
    fun testDirectIpResolution() {
        val target = "192.168.0.99:9000"
        val resolved = NetworkDiscoveryHelper.resolveTargetAddress(target, localIp = "192.168.1.10")
        assertNotNull(resolved)
        assertEquals("192.168.0.99", resolved!!.first)
        assertEquals(9000, resolved.second)

        val targetNoPort = "10.0.0.5"
        val resolvedNoPort = NetworkDiscoveryHelper.resolveTargetAddress(targetNoPort, localIp = "10.0.0.2")
        assertNotNull(resolvedNoPort)
        assertEquals("10.0.0.5", resolvedNoPort!!.first)
        assertEquals(NetworkDiscoveryHelper.DEFAULT_PORT, resolvedNoPort.second)
    }

    @Test
    fun testInvalidAddressResolutionReturnsNull() {
        assertNull(NetworkDiscoveryHelper.resolveTargetAddress("", "192.168.1.1"))
        assertNull(NetworkDiscoveryHelper.resolveTargetAddress("   ", "192.168.1.1"))
        assertNull(NetworkDiscoveryHelper.resolveTargetAddress("INVALID_ROOM_CODE_NON_NUMERIC", "192.168.1.1"))
    }

    // ==================== Peer-to-Peer Socket Connection Tests ====================

    @Test
    fun testServerClientDirectConnectionAndMessaging() = runBlocking {
        // Find a random free port for loopback testing
        val tempSocket = ServerSocket(0)
        val testPort = tempSocket.localPort
        tempSocket.close()

        val serverReceivedLatch = CountDownLatch(1)
        val clientReceivedLatch = CountDownLatch(1)
        var serverReceivedMsg: MultiplayerMessage? = null
        var clientReceivedMsg: MultiplayerMessage? = null

        val server = MultiplayerServer(
            port = testPort,
            onClientConnected = {},
            onMessageReceived = { msg ->
                serverReceivedMsg = msg
                serverReceivedLatch.countDown()
            },
            onClientDisconnected = {},
            onError = {}
        )
        server.start(this)

        // Give server a brief moment to bind
        delay(100L)

        val client = MultiplayerClient(
            hostIp = "127.0.0.1",
            port = testPort,
            onConnected = {},
            onMessageReceived = { msg ->
                clientReceivedMsg = msg
                clientReceivedLatch.countDown()
            },
            onDisconnected = {},
            onError = {}
        )
        client.start(this)

        // Give client a moment to connect
        delay(150L)

        // Client sends message to server
        val clientMsg = MultiplayerMessage(
            sessionId = "sock-test",
            type = MultiplayerMessageType.JOIN_REQUEST,
            payload = "{\"client\":\"player2\"}"
        )
        val sentToServer = client.sendMessage(clientMsg)
        assertTrue("Client should successfully send to server", sentToServer)

        assertTrue("Server should receive message within timeout", serverReceivedLatch.await(3, TimeUnit.SECONDS))
        assertNotNull(serverReceivedMsg)
        assertEquals(MultiplayerMessageType.JOIN_REQUEST, serverReceivedMsg!!.type)

        // Server sends response to client
        val serverMsg = MultiplayerMessage(
            sessionId = "sock-test",
            type = MultiplayerMessageType.JOIN_ACCEPTED,
            payload = "{\"role\":\"BLACK\"}"
        )
        val sentToClient = server.sendMessage(serverMsg)
        assertTrue("Server should successfully send to client", sentToClient)

        assertTrue("Client should receive message within timeout", clientReceivedLatch.await(3, TimeUnit.SECONDS))
        assertNotNull(clientReceivedMsg)
        assertEquals(MultiplayerMessageType.JOIN_ACCEPTED, clientReceivedMsg!!.type)

        client.stop()
        server.stop()
    }

    // ==================== ViewModel Multiplayer Integration Tests ====================

    private fun createTestViewModel(app: Application, controller: GameController = GameController()): ChessViewModel {
        val prefs = app.getSharedPreferences("test_mp_prefs_" + System.currentTimeMillis(), Context.MODE_PRIVATE)
        val settingsRepo = SharedPreferencesSettingsRepository(prefs)
        return ChessViewModel(
            application = app,
            settingsRepository = settingsRepo,
            gameController = controller
        )
    }

    @Test
    fun testStartHostingSetsMultiplayerModeAndWhitePlayer() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val engine = ChessEngine()
        val controller = GameController(engine)
        val viewModel = createTestViewModel(app, controller)

        viewModel.startHostingMultiplayer()

        assertEquals(GameMode.LOCAL_MULTIPLAYER, viewModel.settings.value.mode)
        assertEquals(PieceColor.WHITE, viewModel.settings.value.playerColor)
        assertFalse("Host plays White, board should not be flipped", viewModel.isFlipped.value)

        val session = viewModel.multiplayerSessionState.value
        assertEquals(MultiplayerRole.HOST, session.role)
        assertEquals(MultiplayerStatus.HOSTING_LISTENING, session.status)
        assertTrue("Room code must be populated", session.roomCode.isNotEmpty())

        viewModel.cancelHostingMultiplayer()
        assertEquals(MultiplayerStatus.IDLE, viewModel.multiplayerSessionState.value.status)
    }

    @Test
    fun testJoinMultiplayerSetsClientRoleAndBlackPlayer() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val engine = ChessEngine()
        val controller = GameController(engine)
        val viewModel = createTestViewModel(app, controller)

        viewModel.joinMultiplayerMatch("192.168.1.120:8888")

        assertEquals(GameMode.LOCAL_MULTIPLAYER, viewModel.settings.value.mode)
        assertEquals(PieceColor.BLACK, viewModel.settings.value.playerColor)
        assertTrue("Client plays Black, board should be flipped", viewModel.isFlipped.value)

        val session = viewModel.multiplayerSessionState.value
        assertEquals(MultiplayerRole.CLIENT, session.role)
        assertEquals(MultiplayerStatus.CLIENT_CONNECTING, session.status)

        viewModel.leaveMultiplayerMatch()
        assertEquals(GameMode.PVC, viewModel.settings.value.mode)
    }

    @Test
    fun testMultiplayerTurnEnforcementPreventsOutOfTurnMoves() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val engine = ChessEngine()
        val controller = GameController(engine)
        val viewModel = createTestViewModel(app, controller)

        // Assume Client role (plays Black)
        viewModel.joinMultiplayerMatch("192.168.1.50")

        // Game starts with White's turn
        assertEquals(PieceColor.WHITE, viewModel.gameState.value.activeColor)

        // Client (Black) attempts to click White pawn (e2 = file 4, rank 1)
        viewModel.onSquareClicked(Square(4, 1))

        // No square should be selected because it's not the client's turn and not the client's color
        assertNull("Client playing Black cannot select pieces during White's turn", viewModel.selectedSquare.value)
        assertTrue(viewModel.legalMovesForSelected.value.isEmpty())

        viewModel.leaveMultiplayerMatch()
    }

    @Test
    fun testUndoRedoHintDisabledDuringMultiplayer() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val engine = ChessEngine()
        val controller = GameController(engine)
        val viewModel = createTestViewModel(app, controller)

        viewModel.startHostingMultiplayer()

        // Make an initial move on board (e2-e4)
        viewModel.onSquareClicked(Square(4, 1))
        viewModel.onSquareClicked(Square(4, 3))

        val moveCount = viewModel.gameState.value.moveHistory.size

        // Attempt Undo in multiplayer mode
        viewModel.undoMove()
        assertEquals("Undo must be blocked during multiplayer", moveCount, viewModel.gameState.value.moveHistory.size)

        // Attempt Redo in multiplayer mode
        viewModel.redoMove()
        assertEquals("Redo must be blocked during multiplayer", moveCount, viewModel.gameState.value.moveHistory.size)

        // Attempt Hint in multiplayer mode
        viewModel.getHint()
        assertTrue("Hint should not highlight moves in multiplayer", viewModel.legalMovesForSelected.value.isEmpty())

        viewModel.cancelHostingMultiplayer()
    }

    @Test
    fun testDrawOfferAndAcceptanceFlow() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val engine = ChessEngine()
        val controller = GameController(engine)
        val viewModel = createTestViewModel(app, controller)

        viewModel.startHostingMultiplayer()

        // Host offers draw
        viewModel.offerMultiplayerDraw()
        assertEquals(DrawOfferState.SENT, viewModel.multiplayerSessionState.value.drawOffer)
        assertEquals("Draw offer sent to opponent", viewModel.multiplayerNotice.value)

        // Simulate incoming draw acceptance
        viewModel.acceptMultiplayerDraw()
        assertTrue("Game should be drawn after acceptance", viewModel.gameState.value.isDraw)
        assertTrue("Game over dialog should display", viewModel.showGameOverDialog.value)

        viewModel.clearMultiplayerNotice()
        assertNull(viewModel.multiplayerNotice.value)

        viewModel.cancelHostingMultiplayer()
    }

    @Test
    fun testResignationFlowInMultiplayer() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val engine = ChessEngine()
        val controller = GameController(engine)
        val viewModel = createTestViewModel(app, controller)

        viewModel.startHostingMultiplayer()

        // Host (White) resigns
        viewModel.resignMultiplayer()

        assertTrue("Game should be over after resignation", viewModel.gameState.value.isGameOver)
        assertEquals("Black wins when White resigns", PieceColor.BLACK, viewModel.gameState.value.winner)
        assertTrue(viewModel.showGameOverDialog.value)

        viewModel.cancelHostingMultiplayer()
    }
}
