# FRC-2025 Robot Code

This repository contains the robot code for the 2025 FRC season. The robot is designed for the FIRST Reefscape game with specialized mechanisms for handling coral and algae game pieces.

## Robot Features

### Drivetrain
- **Swerve Drive System**: CTRE Phoenix 6 based swerve drivetrain with TalonFX motors
- **Field-Oriented Control**: Driver can control robot relative to field or robot orientation
- **Precision Control**: Variable speed control with coefficients based on elevator position
- **Auto-Alignment**: PathPlanner integration for autonomous path following

### Elevator System
- **Multi-Level Positioning**: Precise positioning for different scoring levels
  - Stow Position (0.0")
  - Level 2 (9.0") 
  - Level 3 (25.14")
  - Level 4 (52.0")
  - Algae Low (configurable)
  - Algae High (configurable)
- **Safety Features**: Laser-based obstacle detection and speed reduction
- **Motion Profiling**: Trapezoidal velocity profiles for smooth movement

### End Effector System
- **Dual Game Piece Handling**: Separate mechanisms for coral and algae
- **Intake Arms**: Motorized arms with open/close positioning
- **Intake Wheels**: Upper and lower wheel system for game piece manipulation
- **Laser Detection**: Automatic intake stopping based on game piece detection

### Sensor Systems
- **Laser Rangefinders**: Front and back LaserCAN sensors for object detection
- **Encoders**: Precise position feedback on all motorized mechanisms
- **Gyroscope**: Pigeon 2 IMU for drivetrain field orientation

### Android Client Integration
- **WebSocket Server**: Real-time communication with Android companion apps
- **Focus Mode**: Special operating mode with notifications to connected clients
- **JSON API**: Structured message format for robot status updates
- See [WEBSOCKET_API.md](WEBSOCKET_API.md) for complete WebSocket API documentation

## Controls

### Driver Controller (Xbox Controller - Port 0)

#### Basic Movement
- **Left Stick**: Forward/backward and left/right translation
- **Right Stick**: Rotation control
- **Field/Robot Oriented**: Toggle between field-oriented and robot-oriented driving

#### Game Piece Manipulation
- **A Button**: Run End Effector (score coral/algae) 
- **B Button**: Run Intake System (collect game pieces)
- **X Button**: Open/Close Intake Arm
- **Left Bumper**: Toggle Focus Mode (sends notification to Android clients)
- **Right Bumper**: Run Intake Wheels

#### Elevator Control
- **D-Pad**: Quick elevator positioning
  - Up: Level 4 position
  - Down: Stow position
  - Left/Right: Level 2/3 positions
- **Triggers**: Manual elevator position control

### SmartDashboard Controls
Real-time tuning parameters available on SmartDashboard:
- **Focus Mode**: Toggle to enable/disable focus mode (broadcasts to Android clients)
- Elevator target heights for all levels
- Intake arm open/closed positions
- PID tuning parameters for all subsystems

## Dependencies

### Build System
- **Gradle**: 8.11 with GradleRIO 2025.3.1
- **Java**: Version 17 (required)
- **WPILib**: 2025 season libraries

### Vendor Libraries

#### Motor Controllers & Hardware
- **CTRE Phoenix 6** (v25.2.1): TalonFX motor controllers, Pigeon 2 IMU, CANcoders
- **REV Robotics REVLib** (v2025.0.2): SparkMax motor controllers and encoders
- **GrappleRobotics LaserCAN** (latest): Laser rangefinder sensors

#### Path Planning & Autonomous
- **PathPlannerLib** (v2025.2.3): Advanced path planning and following
- **ChoreoLib** (v2025.0.3): Trajectory generation and optimization

#### Testing & Utilities
- **JUnit 5** (v5.10.1): Unit testing framework
- **WPILib New Commands**: Command-based robot framework

#### Communication Libraries
- **Java-WebSocket** (v1.5.3): WebSocket server for Android client communication
- **Gson** (v2.10.1): JSON serialization for message formatting

### Hardware Requirements
- RoboRIO 2.0
- Pigeon 2 IMU (CAN ID 14)
- TalonFX motors for swerve modules
- SparkMax controllers for elevator and end effector
- LaserCAN sensors for object detection
- Xbox controller for driver input

## Build Instructions

1. Ensure Java 17 is installed
2. Clone this repository
3. Open in VS Code with WPILib extension
4. Run `./gradlew build` to compile
5. Deploy with `./gradlew deploy`

## Robot Configuration

- **Team Number**: 10234
- **CAN Bus**: Standard FRC CAN bus configuration
- **Motor IDs**: Defined in `Constants.java`
- **Control Mappings**: Configured in `RobotContainer.java`

## Flowcharts

### Drop Coral Flow Chart

![FRC-Intake-Flowchart-Sayfa -4 drawio](https://github.com/user-attachments/assets/0bbf42e5-fa2c-4dc2-ba29-c94def66c4af)

### Intake Coral Flow Chart

![FRC-Intake-Flowchart-Sayfa -3 drawio](https://github.com/user-attachments/assets/35fcbb2e-95d5-422e-9047-72559eea3023)

### Intake Algae Flow Chart

![FRC-Intake-Flowchart-Sayfa -6 drawio](https://github.com/user-attachments/assets/e124b098-f482-47da-9777-bdf41574c7fb)

### Drop Algae Flow Chart

![FRC-Intake-Flowchart-Sayfa -5 drawio](https://github.com/user-attachments/assets/12232a99-fd16-4ba4-a828-1e5e0b7d9739)

### Elevator Flow Chart

![FRC-Intake-Flowchart-Elevetor drawio](https://github.com/user-attachments/assets/5dd02e30-b146-4904-a64a-a51e7ecb1e76)
