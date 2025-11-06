package frc.robot;

import static org.junit.jupiter.api.Assertions.*;

import java.net.URI;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

/**
 * Test the RobotWebSocketServer functionality
 */
public class RobotWebSocketServerTest {
    private RobotWebSocketServer server;
    private static final int TEST_PORT = 5811; // Use a different port for testing
    private TestWebSocketClient client;
    private Gson gson = new Gson();
    
    @BeforeEach
    public void setup() throws Exception {
        // Start the server
        server = new RobotWebSocketServer(TEST_PORT);
        server.start();
        
        // Wait for server to start
        Thread.sleep(500);
    }
    
    @AfterEach
    public void teardown() throws Exception {
        if (client != null && client.isOpen()) {
            client.close();
        }
        if (server != null) {
            server.stop();
        }
        Thread.sleep(500);
    }
    
    @Test
    public void testServerStartsSuccessfully() {
        assertNotNull(server);
        assertEquals(TEST_PORT, server.getPort());
    }
    
    @Test
    public void testClientCanConnect() throws Exception {
        CountDownLatch connectionLatch = new CountDownLatch(1);
        
        client = new TestWebSocketClient(new URI("ws://localhost:" + TEST_PORT)) {
            @Override
            public void onOpen(ServerHandshake handshakedata) {
                super.onOpen(handshakedata);
                connectionLatch.countDown();
            }
        };
        
        client.connect();
        
        // Wait for connection to be established
        assertTrue(connectionLatch.await(5, TimeUnit.SECONDS), "Client should connect within 5 seconds");
        assertTrue(client.isOpen(), "Client should be connected");
    }
    
    @Test
    public void testWelcomeMessageReceived() throws Exception {
        CountDownLatch messageLatch = new CountDownLatch(1);
        final String[] receivedMessage = new String[1];
        
        client = new TestWebSocketClient(new URI("ws://localhost:" + TEST_PORT)) {
            @Override
            public void onMessage(String message) {
                receivedMessage[0] = message;
                messageLatch.countDown();
            }
        };
        
        client.connect();
        
        // Wait for welcome message
        assertTrue(messageLatch.await(5, TimeUnit.SECONDS), "Welcome message should be received");
        assertNotNull(receivedMessage[0]);
        
        // Parse and verify the JSON message
        JsonObject json = gson.fromJson(receivedMessage[0], JsonObject.class);
        assertEquals("connected", json.get("type").getAsString());
        assertTrue(json.has("message"));
        assertTrue(json.has("timestamp"));
    }
    
    @Test
    public void testFocusModeMessageBroadcast() throws Exception {
        CountDownLatch messageLatch = new CountDownLatch(2); // Wait for welcome + focus mode message
        final String[] lastMessage = new String[1];
        
        client = new TestWebSocketClient(new URI("ws://localhost:" + TEST_PORT)) {
            @Override
            public void onMessage(String message) {
                lastMessage[0] = message;
                messageLatch.countDown();
            }
        };
        
        client.connect();
        
        // Wait for client to connect
        Thread.sleep(1000);
        
        // Broadcast focus mode enabled
        server.broadcastFocusMode(true);
        
        // Wait for messages
        assertTrue(messageLatch.await(5, TimeUnit.SECONDS), "Messages should be received");
        assertNotNull(lastMessage[0]);
        
        // Parse and verify the focus mode message
        JsonObject json = gson.fromJson(lastMessage[0], JsonObject.class);
        assertEquals(Constants.WebSocketServer.kFocusModeMessageType, json.get("type").getAsString());
        assertTrue(json.get("enabled").getAsBoolean());
        assertTrue(json.has("timestamp"));
    }
    
    /**
     * Simple WebSocket client for testing
     */
    private static class TestWebSocketClient extends WebSocketClient {
        public TestWebSocketClient(URI serverUri) {
            super(serverUri);
        }
        
        @Override
        public void onOpen(ServerHandshake handshakedata) {
            System.out.println("Test client connected");
        }
        
        @Override
        public void onMessage(String message) {
            System.out.println("Test client received: " + message);
        }
        
        @Override
        public void onClose(int code, String reason, boolean remote) {
            System.out.println("Test client disconnected: " + reason);
        }
        
        @Override
        public void onError(Exception ex) {
            System.err.println("Test client error: " + ex.getMessage());
        }
    }
}
