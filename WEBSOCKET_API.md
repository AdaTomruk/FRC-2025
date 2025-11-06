# WebSocket API Documentation

## Overview

The FRC robot now includes a WebSocket server that allows Android clients to connect and receive real-time updates about the robot's status, including focus mode state changes.

## Connection Details

- **Protocol**: WebSocket (ws://)
- **Default Port**: 5810
- **Server Address**: `ws://<robot-ip>:5810`
  - For RoboRIO: typically `ws://10.102.34.2:5810` (replace with your team number)
  - For simulation: `ws://localhost:5810`

## Message Format

All messages are sent as JSON strings.

### Message Types

#### 1. Connection Welcome Message

Sent automatically when a client connects.

```json
{
  "type": "connected",
  "message": "Connected to FRC Robot",
  "timestamp": 1699291234567
}
```

#### 2. Focus Mode Status

Sent when focus mode is toggled on or off.

```json
{
  "type": "focus_mode",
  "enabled": true,
  "timestamp": 1699291234567
}
```

**Fields:**
- `type`: Always "focus_mode"
- `enabled`: Boolean indicating if focus mode is active
- `timestamp`: Unix timestamp in milliseconds

#### 3. Robot Status (Future)

General status updates can be sent using the `broadcastStatus()` method.

```json
{
  "type": "status",
  "timestamp": 1699291234567,
  "data": {
    // Custom status fields
  }
}
```

## Controlling Focus Mode

### From Driver Station

Focus mode can be controlled via SmartDashboard:
1. Open SmartDashboard
2. Find the "Focus Mode" boolean widget
3. Toggle to enable/disable

### From Xbox Controller

Press the **Left Bumper** button to toggle focus mode on/off.

### Programmatically

In Java code, you can control focus mode:

```java
// Enable focus mode
Robot.setFocusMode(true);

// Disable focus mode
Robot.setFocusMode(false);

// Check current state
boolean isFocused = Robot.isFocusModeEnabled();
```

## Android Client Example

Here's a simple example of connecting from an Android app using a WebSocket library like OkHttp:

```kotlin
import okhttp3.*

class RobotWebSocketListener : WebSocketListener() {
    override fun onOpen(webSocket: WebSocket, response: Response) {
        println("Connected to robot")
    }
    
    override fun onMessage(webSocket: WebSocket, text: String) {
        // Parse JSON message
        val json = JSONObject(text)
        val type = json.getString("type")
        
        when (type) {
            "connected" -> {
                println("Robot connection established")
            }
            "focus_mode" -> {
                val enabled = json.getBoolean("enabled")
                println("Focus mode: $enabled")
                // Update UI accordingly
            }
        }
    }
    
    override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
        println("Connection failed: ${t.message}")
    }
}

// Connect to robot
val client = OkHttpClient()
val request = Request.Builder()
    .url("ws://10.102.34.2:5810")
    .build()
val listener = RobotWebSocketListener()
client.newWebSocket(request, listener)
```

## Configuration

The WebSocket server port can be changed in `Constants.java`:

```java
public static class WebSocketServer {
    public static final int kServerPort = 5810;  // Change this to use a different port
    public static final String kFocusModeMessageType = "focus_mode";
}
```

## Troubleshooting

### Cannot Connect to Robot

1. Verify the robot is powered on and the code is deployed
2. Check that you're using the correct IP address (team number based: 10.TE.AM.2)
3. Ensure no firewall is blocking port 5810
4. Check robot logs for WebSocket server startup messages

### Messages Not Received

1. Verify the client is connected (check the `onOpen` callback)
2. Check robot console output for broadcast messages
3. Ensure focus mode is actually being toggled (check SmartDashboard)

### Server Fails to Start

1. Check if another process is using port 5810
2. Review robot logs for error messages
3. Try changing the port number in Constants.java

## Security Considerations

- The WebSocket server does not require authentication
- All clients on the same network can connect
- Messages are sent in plain text (not encrypted)
- For competition use, ensure your network is properly secured

## Future Enhancements

Potential additions to the WebSocket API:
- Battery voltage updates
- Match time remaining
- Current autonomous routine
- Subsystem status
- Diagnostic information
- Bi-directional communication (commands from Android)
