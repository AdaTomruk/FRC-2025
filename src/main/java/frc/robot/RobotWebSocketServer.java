package frc.robot;

import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;

import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;

import com.google.gson.Gson;

/**
 * WebSocket server for communicating with Android clients.
 * Sends JSON messages about robot status including focus mode state.
 */
public class RobotWebSocketServer extends WebSocketServer {
    private final Gson gson = new Gson();
    
    public RobotWebSocketServer(int port) {
        super(new InetSocketAddress(port));
        System.out.println("RobotWebSocketServer initialized on port " + port);
    }

    @Override
    public void onOpen(WebSocket conn, ClientHandshake handshake) {
        System.out.println("New WebSocket connection from: " + conn.getRemoteSocketAddress());
        
        // Send welcome message
        Map<String, Object> welcomeMessage = new HashMap<>();
        welcomeMessage.put("type", "connected");
        welcomeMessage.put("message", "Connected to FRC Robot");
        welcomeMessage.put("timestamp", System.currentTimeMillis());
        
        sendJsonMessage(conn, welcomeMessage);
    }

    @Override
    public void onClose(WebSocket conn, int code, String reason, boolean remote) {
        System.out.println("WebSocket closed: " + conn.getRemoteSocketAddress() + " - " + reason);
    }

    @Override
    public void onMessage(WebSocket conn, String message) {
        System.out.println("Received message from " + conn.getRemoteSocketAddress() + ": " + message);
    }

    @Override
    public void onError(WebSocket conn, Exception ex) {
        System.err.println("WebSocket error: " + ex.getMessage());
        ex.printStackTrace();
    }

    @Override
    public void onStart() {
        System.out.println("WebSocket server started successfully");
        setConnectionLostTimeout(0);
        setConnectionLostTimeout(100);
    }

    /**
     * Send a JSON message to a specific WebSocket connection
     */
    private void sendJsonMessage(WebSocket conn, Map<String, Object> data) {
        try {
            String json = gson.toJson(data);
            conn.send(json);
        } catch (Exception e) {
            System.err.println("Error sending JSON message: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Broadcast focus mode status to all connected clients
     * 
     * @param enabled Whether focus mode is enabled
     */
    public void broadcastFocusMode(boolean enabled) {
        Map<String, Object> message = new HashMap<>();
        message.put("type", Constants.WebSocketServer.kFocusModeMessageType);
        message.put("enabled", enabled);
        message.put("timestamp", System.currentTimeMillis());
        
        String json = gson.toJson(message);
        broadcast(json);
        
        System.out.println("Broadcasting focus mode: " + enabled + " to " + 
                          getConnections().size() + " clients");
    }

    /**
     * Send robot status update to all connected clients
     * 
     * @param statusData Map containing status information
     */
    public void broadcastStatus(Map<String, Object> statusData) {
        statusData.put("type", "status");
        statusData.put("timestamp", System.currentTimeMillis());
        
        String json = gson.toJson(statusData);
        broadcast(json);
    }
}
