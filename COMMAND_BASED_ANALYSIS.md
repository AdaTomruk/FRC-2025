# Command-Based Programming Analysis and Best Practices

## Table of Contents
1. [Identified Mistakes in Command-Based System](#identified-mistakes)
2. [How to Improve These Issues](#improvements)
3. [Simplifying Repeating Patterns with OOP](#oop-simplification)
4. [Command-Based Programming Best Practices](#best-practices)

---

## Identified Mistakes in Command-Based System

### 1. **Misuse of `Commands.runOnce()` in RobotContainer**

**Location:** `RobotContainer.java` lines 94-116

**Problem:**
```java
joystick.a().onTrue(Commands.runOnce((m_EndeffectorSubsystem::runEndEffector)));
joystick.b().onTrue(Commands.runOnce((m_EndeffectorSubsystem::runIntake)));
joystick.x().onTrue(Commands.runOnce((m_EndeffectorSubsystem::openCloseIntakeArm)));
```

**Issues:**
- `Commands.runOnce()` executes a single action and immediately ends
- These subsystem methods don't actually complete instantly - they just set flags
- The command doesn't require the subsystem, so multiple conflicting commands can run simultaneously
- No proper command lifecycle (initialize, execute, end, isFinished)

**Why it's wrong:**
- Commands should **require** their subsystems to prevent concurrent access
- WPILib command scheduler won't manage subsystem state properly
- Race conditions can occur when multiple buttons are pressed

---

### 2. **Subsystem State Management Done Outside Commands**

**Location:** `EndeffectorSubsystem.java` and `ElevetorSubsystem.java`

**Problem:**
```java
public static boolean IntakeStatus = false;
public static boolean EndEffectorStatus = false;
public static boolean IntakeArmStatus = false;
```

**Issues:**
- Subsystems are managing their own state machines using boolean flags
- State transitions happen in periodic() methods instead of command execution
- Static variables break encapsulation and make testing difficult
- Violates the principle that commands should control subsystem behavior

**Why it's wrong:**
- Commands should manage state, not subsystems
- Makes it impossible to know what command is controlling the subsystem
- Command interruption won't work properly

---

### 3. **Direct Subsystem Method Calls Instead of Commands**

**Location:** `RobotContainer.java` lines 108-111

**Problem:**
```java
joystick.pov(0).onTrue(Commands.runOnce((m_ElevetorSubsystem::goToElevatorStow)));
joystick.pov(90).onTrue(Commands.runOnce((m_ElevetorSubsystem::goToprivateLevel2)));
```

**Issues:**
- These methods set target positions but don't wait for completion
- No way to interrupt or cancel the motion
- Can't compose these into larger autonomous routines
- No feedback when the action is complete

---

### 4. **Singleton Pattern Misuse**

**Location:** `ElevetorSubsystem.java` lines 25-33

**Problem:**
```java
private static ElevetorSubsystem mInstance;

public static ElevetorSubsystem getInstance() {
    if (mInstance == null) {
      mInstance = new ElevetorSubsystem();
    }
    return mInstance;
}
```

**Issues:**
- Singleton is created but never used (RobotContainer instantiates directly)
- Inconsistent pattern - EndeffectorSubsystem doesn't use singleton
- Makes testing difficult
- Not recommended in modern WPILib

**Why it's wrong:**
- WPILib 2020+ recommends instantiating subsystems in RobotContainer
- Singletons make unit testing nearly impossible
- Creates hidden dependencies

---

### 5. **Public Mutable State in Subsystems**

**Location:** Multiple subsystems

**Problem:**
```java
public static double robotSpeedCoefficient = 1.0; // ElevetorSubsystem.java
public static boolean IntakeArmStatus = false;     // EndeffectorSubsystem.java
```

**Issues:**
- Public static mutable state breaks encapsulation
- Any code can modify these variables
- Creates hidden coupling between classes
- Makes debugging extremely difficult

---

### 6. **Periodic Logic That Should Be Commands**

**Location:** `EndeffectorSubsystem.java` lines 288-295

**Problem:**
```java
public void runPeriodicOutputsEndeffector() {
    if(IntakeStatus){
        runIntake();
    }
    if(EndEffectorStatus){
        runEndEffector();
    }
}
```

**Issues:**
- Business logic in periodic() instead of commands
- Periodic should only handle hardware I/O and telemetry
- Makes the subsystem control its own high-level behavior

---

### 7. **Missing Subsystem Requirements in Commands**

**Location:** `CoralCommands.java`

**Problem:**
```java
public static Command IntakeCoral(EndeffectorSubsystem endeffectorSubsystem) {
    return Commands.sequence(
        Commands.runOnce(endeffectorSubsystem::runIntake),
        new WaitUntilCommand(() -> !endeffectorSubsystem.IntakeStatus)
    );
}
```

**Issues:**
- `runOnce` doesn't require the subsystem
- WaitUntilCommand doesn't require the subsystem
- Multiple IntakeCoral commands could run simultaneously
- Command scheduler won't prevent conflicts

---

### 8. **Cross-Subsystem Dependencies**

**Location:** `ElevetorSubsystem.java` lines 218-234

**Problem:**
```java
public void goToprivateLevel2() {
    if(EndeffectorSubsystem.IntakeArmStatus == false){
        goToElevatorL2();
    }
    else{
        goToElevatorAlgaeLow();
    }
}
```

**Issues:**
- ElevetorSubsystem directly accesses EndeffectorSubsystem state
- Creates tight coupling between subsystems
- Should be handled in command composition in RobotContainer

---

### 9. **Vision Measurement Logic in Robot.java**

**Location:** `Robot.java` lines 35-45

**Problem:**
```java
if (kUseLimelight) {
    var driveState = m_robotContainer.drivetrain.getState();
    // ... vision processing in Robot.java
    m_robotContainer.drivetrain.addVisionMeasurement(llMeasurement.pose, llMeasurement.timestampSeconds);
}
```

**Issues:**
- Business logic in Robot.java instead of a subsystem
- Accessing subsystem through RobotContainer
- Should be in a VisionSubsystem

---

### 10. **Inline Lambda Button Bindings**

**Location:** `RobotContainer.java` lines 113-116

**Problem:**
```java
joystick.rightTrigger().onTrue(Commands.runOnce(() -> 
    m_ElevetorSubsystem.setElevatorPositionPositive(-joystick.getRightTriggerAxis())));
```

**Issues:**
- Complex logic in binding instead of dedicated command
- Mixing button binding with business logic
- Difficult to test
- Difficult to reuse

---

## Improvements

### How to Fix Mistake #1: Create Proper Commands

**Before:**
```java
joystick.a().onTrue(Commands.runOnce((m_EndeffectorSubsystem::runEndEffector)));
```

**After:** Create a proper command class
```java
// In commands/RunEndEffectorCommand.java
public class RunEndEffectorCommand extends Command {
    private final EndeffectorSubsystem m_subsystem;
    
    public RunEndEffectorCommand(EndeffectorSubsystem subsystem) {
        m_subsystem = subsystem;
        addRequirements(subsystem); // Critical!
    }
    
    @Override
    public void initialize() {
        m_subsystem.startEndEffector();
    }
    
    @Override
    public void execute() {
        // Command continues until laser sensor detects completion
    }
    
    @Override
    public void end(boolean interrupted) {
        m_subsystem.stopEndEffector();
    }
    
    @Override
    public boolean isFinished() {
        return m_subsystem.isEndEffectorComplete();
    }
}

// In RobotContainer.java
joystick.a().onTrue(new RunEndEffectorCommand(m_EndeffectorSubsystem));
```

---

### How to Fix Mistake #2: Remove State Flags from Subsystems

**Before:**
```java
public static boolean IntakeStatus = false;

public void runIntake() {
    LEndeffectorSparkmax.set(CoralEndeffector.IntakeSpeed);
    if (laserSubsystem.logFrontLaserMeasurement() < 40) {
        IntakeStatus = false;
        stopIntake();
    } else {
        IntakeStatus = true;
    }
}
```

**After:** Let commands manage state
```java
// In subsystem - only expose primitives
public void setIntakeSpeed(double speed) {
    LEndeffectorSparkmax.set(speed);
    REndeffectorSparkmax.set(-speed);
}

public void stopIntake() {
    LEndeffectorSparkmax.set(0);
    REndeffectorSparkmax.set(0);
}

public boolean hasGamePiece() {
    return laserSubsystem.logFrontLaserMeasurement() < 40;
}

// In command - manages the state
public class IntakeCommand extends Command {
    private final EndeffectorSubsystem m_subsystem;
    
    public IntakeCommand(EndeffectorSubsystem subsystem) {
        m_subsystem = subsystem;
        addRequirements(subsystem);
    }
    
    @Override
    public void initialize() {
        m_subsystem.setIntakeSpeed(CoralEndeffector.IntakeSpeed);
    }
    
    @Override
    public void end(boolean interrupted) {
        m_subsystem.stopIntake();
    }
    
    @Override
    public boolean isFinished() {
        return m_subsystem.hasGamePiece();
    }
}
```

---

### How to Fix Mistake #3: Use Command Factories

**Before:**
```java
joystick.pov(0).onTrue(Commands.runOnce((m_ElevetorSubsystem::goToElevatorStow)));
```

**After:** Create a factory method that returns a proper command
```java
// In ElevetorSubsystem.java
public Command goToPositionCommand(double targetPosition) {
    return Commands.run(
        () -> setTargetPosition(targetPosition),
        this // Requires this subsystem
    ).until(() -> atPosition(targetPosition))
    .withName("ElevatorToPosition");
}

// In RobotContainer.java
joystick.pov(0).onTrue(m_ElevetorSubsystem.goToPositionCommand(Constants.Elevator.kStowHeight));
```

---

### How to Fix Mistake #4: Remove Singleton Pattern

**Before:**
```java
private static ElevetorSubsystem mInstance;
public static ElevetorSubsystem getInstance() { ... }
```

**After:**
```java
// Just remove the singleton pattern entirely
// Instantiate in RobotContainer only:
private final ElevetorSubsystem m_ElevetorSubsystem = new ElevetorSubsystem();
```

---

### How to Fix Mistake #5: Encapsulate State

**Before:**
```java
public static double robotSpeedCoefficient = 1.0;
```

**After:**
```java
// In subsystem
private double speedCoefficient = 1.0;

public double getSpeedCoefficient() {
    return speedCoefficient;
}

// Better: Don't expose this at all - calculate in the drivetrain default command
```

---

### How to Fix Mistake #6: Move Logic to Commands

**Before:**
```java
@Override
public void periodic() {
    if(IntakeStatus){
        runIntake();
    }
}
```

**After:**
```java
@Override
public void periodic() {
    // Only telemetry and low-level I/O
    SmartDashboard.putNumber("Elevator Position", getCurrentPosition());
    // No business logic!
}
```

---

### How to Fix Mistake #7: Add Requirements to Commands

**Before:**
```java
return Commands.sequence(
    Commands.runOnce(endeffectorSubsystem::runIntake),
    new WaitUntilCommand(() -> !endeffectorSubsystem.IntakeStatus)
);
```

**After:**
```java
return Commands.sequence(
    Commands.runOnce(endeffectorSubsystem::startIntake, endeffectorSubsystem),
    Commands.waitUntil(endeffectorSubsystem::hasGamePiece)
).withRequirements(endeffectorSubsystem);
```

Or better yet, use a proper command class as shown in Fix #2.

---

### How to Fix Mistake #8: Handle Cross-Subsystem Logic in Commands

**Before:**
```java
// In ElevetorSubsystem
public void goToprivateLevel2() {
    if(EndeffectorSubsystem.IntakeArmStatus == false){
        goToElevatorL2();
    } else {
        goToElevatorAlgaeLow();
    }
}
```

**After:**
```java
// In RobotContainer
private Command goToLevel2Command() {
    return Commands.either(
        m_ElevetorSubsystem.goToPositionCommand(Constants.Elevator.kL2Height),
        m_ElevetorSubsystem.goToPositionCommand(Constants.Elevator.kLowAlgaeHeight),
        m_EndeffectorSubsystem::isArmOpen
    );
}

joystick.pov(90).onTrue(goToLevel2Command());
```

---

### How to Fix Mistake #9: Create a Vision Subsystem

**Before:**
```java
// In Robot.java robotPeriodic()
if (kUseLimelight) {
    var driveState = m_robotContainer.drivetrain.getState();
    // ...
}
```

**After:**
```java
// Create VisionSubsystem.java
public class VisionSubsystem extends SubsystemBase {
    private final CommandSwerveDrivetrain m_drivetrain;
    
    public VisionSubsystem(CommandSwerveDrivetrain drivetrain) {
        m_drivetrain = drivetrain;
    }
    
    @Override
    public void periodic() {
        var driveState = m_drivetrain.getState();
        double headingDeg = driveState.Pose.getRotation().getDegrees();
        double omegaRps = Units.radiansToRotations(driveState.Speeds.omegaRadiansPerSecond);
        
        LimelightHelpers.SetRobotOrientation("limelight", headingDeg, 0, 0, 0, 0, 0);
        var llMeasurement = LimelightHelpers.getBotPoseEstimate_wpiBlue_MegaTag2("limelight");
        
        if (llMeasurement != null && llMeasurement.tagCount > 0 && Math.abs(omegaRps) < 2.0) {
            m_drivetrain.addVisionMeasurement(llMeasurement.pose, llMeasurement.timestampSeconds);
        }
    }
}

// Instantiate in RobotContainer
private final VisionSubsystem m_visionSubsystem = new VisionSubsystem(drivetrain);
```

---

### How to Fix Mistake #10: Extract Command Classes

**Before:**
```java
joystick.rightTrigger().onTrue(Commands.runOnce(() -> 
    m_ElevetorSubsystem.setElevatorPositionPositive(-joystick.getRightTriggerAxis())));
```

**After:**
```java
// Create ManualElevatorCommand.java
public class ManualElevatorCommand extends Command {
    private final ElevetorSubsystem m_elevator;
    private final DoubleSupplier m_speedSupplier;
    
    public ManualElevatorCommand(ElevetorSubsystem elevator, DoubleSupplier speedSupplier) {
        m_elevator = elevator;
        m_speedSupplier = speedSupplier;
        addRequirements(elevator);
    }
    
    @Override
    public void execute() {
        m_elevator.setManualSpeed(m_speedSupplier.getAsDouble());
    }
    
    @Override
    public void end(boolean interrupted) {
        m_elevator.stop();
    }
}

// In RobotContainer
joystick.rightTrigger().whileTrue(
    new ManualElevatorCommand(m_ElevetorSubsystem, joystick::getRightTriggerAxis)
);
```

---

## Simplifying Repeating Patterns with OOP

### Pattern 1: Elevator Position Commands

**Current Code (Repetitive):**
```java
public void goToElevatorStow() {
    mPeriodicIO.is_elevator_pos_control = true;
    mPeriodicIO.elevator_target = -Constants.Elevator.kStowHeight;
    mPeriodicIO.state = ElevatorState.STOW;
    robotSpeedCoefficient = 1.0;
}

private void goToElevatorL2() {
    mPeriodicIO.is_elevator_pos_control = true;
    mPeriodicIO.elevator_target = -Constants.Elevator.kL2Height;
    mPeriodicIO.state = ElevatorState.L2;
    robotSpeedCoefficient = Constants.Elevator.kDrivetrainL2ElevetorHeightDriveSpeedCoefficient;
}

// ... 5 more similar methods
```

**Improved with OOP:**
```java
// Create an ElevatorPosition enum or class
public enum ElevatorPosition {
    STOW(Constants.Elevator.kStowHeight, 1.0),
    L2(Constants.Elevator.kL2Height, Constants.Elevator.kDrivetrainL2ElevetorHeightDriveSpeedCoefficient),
    L3(Constants.Elevator.kL3Height, Constants.Elevator.kDrivetrainL3ElevetorHeightDriveSpeedCoefficient),
    L4(Constants.Elevator.kL4Height, Constants.Elevator.kDrivetrainL4ElevetorHeightDriveSpeedCoefficient),
    ALGAE_LOW(Constants.Elevator.kLowAlgaeHeight, Constants.Elevator.kDrivetrainAlgaeLowElevetorHeightDriveSpeedCoefficient),
    ALGAE_HIGH(Constants.Elevator.kHighAlgaeHeight, Constants.Elevator.kDrivetrainAlgaeHighElevetorHeightDriveSpeedCoefficient);
    
    public final double height;
    public final double speedCoefficient;
    
    ElevatorPosition(double height, double speedCoefficient) {
        this.height = height;
        this.speedCoefficient = speedCoefficient;
    }
}

// Single method replaces 6 methods
public Command goToPositionCommand(ElevatorPosition position) {
    return Commands.runOnce(
        () -> {
            mPeriodicIO.is_elevator_pos_control = true;
            mPeriodicIO.elevator_target = -position.height;
            robotSpeedCoefficient = position.speedCoefficient;
        },
        this
    ).andThen(Commands.waitUntil(() -> atPosition(position.height)))
    .withName("ElevatorTo" + position.name());
}

// Usage in RobotContainer
joystick.pov(0).onTrue(m_ElevetorSubsystem.goToPositionCommand(ElevatorPosition.STOW));
joystick.pov(270).onTrue(m_ElevetorSubsystem.goToPositionCommand(ElevatorPosition.L4));
```

---

### Pattern 2: Motor Configuration

**Current Code (Repetitive):**
```java
// Repeated 4+ times for different motors
SparkMaxConfig IntakeArmConfig = new SparkMaxConfig();
IntakeArmConfig.closedLoop
    .pid(Constants.AlgaeEndeffector.kP, Constants.AlgaeEndeffector.kI, Constants.AlgaeEndeffector.kD)
    .iZone(Constants.AlgaeEndeffector.kIZone);
IntakeArmConfig.smartCurrentLimit(Constants.AlgaeEndeffector.kMaxCurrent);
IntakeArmConfig.idleMode(IdleMode.kBrake);
IntakeArmConfig.limitSwitch.reverseLimitSwitchEnabled(true);
IntakeArmEncoder = IntakeArmSparkmax.getEncoder();
IntakeArmPIDController = IntakeArmSparkmax.getClosedLoopController();
IntakeArmSparkmax.configure(IntakeArmConfig, ResetMode.kResetSafeParameters, PersistMode.kPersistParameters);
```

**Improved with Factory Pattern:**
```java
// Create a motor configuration factory
public class SparkMaxFactory {
    public static class PIDConfig {
        public double kP, kI, kD, kIZone;
        public int currentLimit;
        
        public PIDConfig(double kP, double kI, double kD, double kIZone, int currentLimit) {
            this.kP = kP;
            this.kI = kI;
            this.kD = kD;
            this.kIZone = kIZone;
            this.currentLimit = currentLimit;
        }
    }
    
    public static SparkMax createPIDMotor(
        int canId, 
        MotorType type, 
        PIDConfig config, 
        boolean enableReverseLimitSwitch
    ) {
        SparkMax motor = new SparkMax(canId, type);
        SparkMaxConfig motorConfig = new SparkMaxConfig();
        
        motorConfig.closedLoop
            .pid(config.kP, config.kI, config.kD)
            .iZone(config.kIZone);
        motorConfig.smartCurrentLimit(config.currentLimit);
        motorConfig.idleMode(IdleMode.kBrake);
        
        if (enableReverseLimitSwitch) {
            motorConfig.limitSwitch.reverseLimitSwitchEnabled(true);
        }
        
        motor.configure(motorConfig, ResetMode.kResetSafeParameters, PersistMode.kPersistParameters);
        return motor;
    }
}

// Usage - single line per motor!
private SparkMax IntakeArmSparkmax = SparkMaxFactory.createPIDMotor(
    AlgaeEndeffector.IntakeArmSparkmaxid,
    MotorType.kBrushless,
    new PIDConfig(AlgaeEndeffector.kP, AlgaeEndeffector.kI, AlgaeEndeffector.kD, 
                  AlgaeEndeffector.kIZone, AlgaeEndeffector.kMaxCurrent),
    true
);
```

---

### Pattern 3: Trapezoidal Profile Management

**Current Code (Repetitive):**
```java
// Repeated 4 times for different mechanisms
private TrapezoidProfile intakeArmTrapezoidProfile;
private TrapezoidProfile.State intakeArmTrapezoidCurState = new TrapezoidProfile.State();
private TrapezoidProfile.State intakeArmTrapezoidGoalState = new TrapezoidProfile.State();
private double intakeArmprevUpdateTime = Timer.getFPGATimestamp();

// ... repeated initialization and update logic
```

**Improved with Helper Class:**
```java
// Create a ProfiledMechanism helper class
public class ProfiledMechanism {
    private final TrapezoidProfile profile;
    private TrapezoidProfile.State currentState;
    private TrapezoidProfile.State goalState;
    private double prevUpdateTime;
    
    public ProfiledMechanism(double maxVelocity, double maxAcceleration) {
        this.profile = new TrapezoidProfile(
            new TrapezoidProfile.Constraints(maxVelocity, maxAcceleration)
        );
        this.currentState = new TrapezoidProfile.State();
        this.goalState = new TrapezoidProfile.State();
        this.prevUpdateTime = Timer.getFPGATimestamp();
    }
    
    public TrapezoidProfile.State update(double targetPosition) {
        double currentTime = Timer.getFPGATimestamp();
        double dt = currentTime - prevUpdateTime;
        prevUpdateTime = currentTime;
        
        goalState.position = targetPosition;
        currentState = profile.calculate(dt, currentState, goalState);
        return currentState;
    }
    
    public void resetToPosition(double position) {
        currentState.position = position;
        currentState.velocity = 0;
    }
}

// Usage in subsystem - much cleaner!
private ProfiledMechanism intakeArmProfile = new ProfiledMechanism(
    Constants.AlgaeEndeffector.kMaxVelocity,
    Constants.AlgaeEndeffector.kMaxAcceleration
);

// In writePeriodicOutputs
var state = intakeArmProfile.update(mPeriodicIO.arm_target);
IntakeArmPIDController.setReference(
    state.position,
    SparkBase.ControlType.kPosition,
    ClosedLoopSlot.kSlot0,
    Constants.AlgaeEndeffector.kG,
    ArbFFUnits.kVoltage
);
```

---

### Pattern 4: Button Binding Duplication

**Current Code (Repetitive):**
```java
joystick.pov(0).onTrue(Commands.runOnce((m_ElevetorSubsystem::goToElevatorStow)));
joystick.pov(90).onTrue(Commands.runOnce((m_ElevetorSubsystem::goToprivateLevel2)));
joystick.pov(180).onTrue(Commands.runOnce((m_ElevetorSubsystem::goToprivateLevel3)));
joystick.pov(270).onTrue(Commands.runOnce((m_ElevetorSubsystem::goToElevatorL4)));
```

**Improved with Helper Method:**
```java
private void configurePOVBindings() {
    Map<Integer, ElevatorPosition> povBindings = Map.of(
        0, ElevatorPosition.STOW,
        90, ElevatorPosition.L2,
        180, ElevatorPosition.L3,
        270, ElevatorPosition.L4
    );
    
    povBindings.forEach((angle, position) -> 
        joystick.pov(angle).onTrue(m_ElevetorSubsystem.goToPositionCommand(position))
    );
}
```

---

## Command-Based Programming Best Practices

### 1. **Subsystem Design Principles**

#### Rule: Subsystems are hardware abstractions
```java
✅ GOOD: Subsystem exposes primitive operations
public void setIntakeSpeed(double speed) { ... }
public double getCurrentPosition() { ... }
public boolean atTarget() { ... }

❌ BAD: Subsystem has high-level logic
public void autoIntakeSequence() {
    // Complex multi-step logic here
}
```

#### Rule: One subsystem per mechanism
- Each physical mechanism gets its own subsystem
- Subsystems should not directly control other subsystems
- Cross-subsystem coordination happens in commands

---

### 2. **Command Design Principles**

#### Rule: Commands control behavior, subsystems control hardware
```java
✅ GOOD: Command orchestrates subsystem
public class IntakeCommand extends Command {
    public void initialize() { subsystem.startIntake(); }
    public void execute() { /* monitor */ }
    public void end() { subsystem.stopIntake(); }
    public boolean isFinished() { return subsystem.hasGamePiece(); }
}

❌ BAD: Subsystem contains business logic
public void autoIntake() {
    startIntake();
    while (!hasGamePiece()) { /* wait */ }
    stopIntake();
}
```

#### Rule: Always require subsystems
```java
✅ GOOD:
public IntakeCommand(Subsystem subsystem) {
    addRequirements(subsystem); // Prevents conflicts!
}

❌ BAD:
public IntakeCommand(Subsystem subsystem) {
    // No requirements - multiple commands can run!
}
```

#### Rule: Use factory methods for simple commands
```java
✅ GOOD: Factory method in subsystem
public Command setPositionCommand(double position) {
    return Commands.runOnce(() -> setTarget(position), this)
        .andThen(Commands.waitUntil(() -> atPosition()))
        .withName("SetPosition");
}

✅ ALSO GOOD: Separate command class for complex commands
public class IntakeCommand extends Command { ... }
```

---

### 3. **RobotContainer Principles**

#### Rule: RobotContainer is the composition root
```java
✅ GOOD: All subsystems instantiated here
public class RobotContainer {
    private final DriveSubsystem m_drive = new DriveSubsystem();
    private final IntakeSubsystem m_intake = new IntakeSubsystem();
    
    public RobotContainer() {
        configureBindings();
    }
}

❌ BAD: Using singletons
public class IntakeSubsystem {
    private static IntakeSubsystem instance;
    public static getInstance() { ... }
}
```

#### Rule: Button bindings should be declarative
```java
✅ GOOD: Clear and readable
joystick.a().onTrue(new IntakeCommand(m_intake));
joystick.b().whileTrue(new ShootCommand(m_shooter));

❌ BAD: Logic in bindings
joystick.a().onTrue(Commands.runOnce(() -> {
    if (m_intake.hasGamePiece()) {
        m_shooter.shoot();
    } else {
        m_intake.start();
    }
}));
```

---

### 4. **State Management**

#### Rule: Commands manage state, not subsystems
```java
✅ GOOD: Command tracks state
public class SequenceCommand extends Command {
    private enum State { STEP1, STEP2, STEP3 }
    private State currentState = State.STEP1;
    
    public void execute() {
        switch(currentState) {
            case STEP1: /* ... */ currentState = State.STEP2; break;
            // ...
        }
    }
}

❌ BAD: Subsystem has state machine
public class Subsystem extends SubsystemBase {
    private enum State { STATE1, STATE2 }
    private State currentState;
    
    public void periodic() {
        switch(currentState) { /* ... */ }
    }
}
```

---

### 5. **Command Composition**

#### Rule: Use command decorators for common patterns
```java
✅ GOOD: Compose commands with decorators
Command autoSequence = Commands.sequence(
    driveSubsystem.pathFollowCommand(path1),
    Commands.parallel(
        armSubsystem.moveToPositionCommand(ArmPosition.SCORE),
        intakeSubsystem.prepareToScoreCommand()
    ),
    shooterSubsystem.scoreCommand(),
    driveSubsystem.pathFollowCommand(path2)
).withTimeout(15.0);

❌ BAD: Monolithic command
public class AutoCommand extends SequentialCommandGroup {
    public AutoCommand() {
        addCommands(
            new DriveCommand(),
            new CustomWaitCommand(),
            new ArmMoveCommand(),
            // Hard to test, hard to reuse
        );
    }
}
```

---

### 6. **Testing and Simulation**

#### Rule: Design for testability
```java
✅ GOOD: Dependency injection
public class Subsystem extends SubsystemBase {
    private final Motor motor;
    
    public Subsystem(Motor motor) {
        this.motor = motor; // Can mock for testing
    }
}

❌ BAD: Hard-coded dependencies
public class Subsystem extends SubsystemBase {
    private final Motor motor = new Motor(5); // Can't test
}
```

---

### 7. **Constants Management**

#### Rule: Group constants logically
```java
✅ GOOD:
public final class Constants {
    public static final class Elevator {
        public static final int kLeftMotorID = 10;
        public static final int kRightMotorID = 11;
        public static final double kP = 0.1;
        // All elevator constants together
    }
    
    public static final class Intake {
        // All intake constants together
    }
}

❌ BAD: Constants scattered everywhere
public class Subsystem {
    private static final int MOTOR_ID = 10; // Hidden in class
}
```

---

### 8. **Default Commands**

#### Rule: Set default commands for continuous operation
```java
✅ GOOD: Default command for drivetrain
public RobotContainer() {
    m_drivetrain.setDefaultCommand(
        new TeleopDriveCommand(
            m_drivetrain,
            () -> -controller.getLeftY(),
            () -> -controller.getLeftX(),
            () -> -controller.getRightX()
        )
    );
}

❌ BAD: No default command, subsystem idles
// Subsystem does nothing when no command is running
```

---

### 9. **Autonomous Design**

#### Rule: Build autonomous from reusable commands
```java
✅ GOOD: Compose from building blocks
public Command getAutonomousCommand() {
    return Commands.sequence(
        resetOdometryCommand(),
        driveToPositionCommand(position1),
        scoreGamePieceCommand(),
        driveToPositionCommand(position2)
    );
}

❌ BAD: Monolithic auto command
public class AutoCommand extends Command {
    // 500 lines of sequential logic
}
```

---

### 10. **Performance Considerations**

#### Rule: Minimize work in periodic()
```java
✅ GOOD: Only essential I/O and telemetry
public void periodic() {
    // Read sensors
    currentPosition = encoder.getPosition();
    
    // Update telemetry
    SmartDashboard.putNumber("Position", currentPosition);
}

❌ BAD: Heavy computation in periodic
public void periodic() {
    // Complex calculations
    var result = expensiveComputation();
    
    // Network I/O
    networkTable.putValue("key", value);
    
    // Runs every 20ms - too slow!
}
```

---

## Additional Resources

### Official WPILib Documentation
- [Command-Based Programming](https://docs.wpilib.org/en/stable/docs/software/commandbased/commands.html)
- [Subsystems](https://docs.wpilib.org/en/stable/docs/software/commandbased/subsystems.html)
- [Command Compositions](https://docs.wpilib.org/en/stable/docs/software/commandbased/command-compositions.html)

### Community Best Practices
- [Chief Delphi: Command-Based Best Practices 2025](https://www.chiefdelphi.com/t/command-based-best-practices-for-2025-community-feedback/465602)
- WPILib Example Projects
- Team 254 Programming Resources

---

## Summary of Key Changes Needed

### High Priority
1. ✅ Create proper Command classes instead of using `Commands.runOnce()`
2. ✅ Add subsystem requirements to all commands
3. ✅ Move state management from subsystems to commands
4. ✅ Remove singleton pattern from ElevetorSubsystem
5. ✅ Encapsulate public static variables

### Medium Priority
6. ✅ Extract vision logic to VisionSubsystem
7. ✅ Create command factories for common operations
8. ✅ Simplify button bindings
9. ✅ Handle cross-subsystem dependencies in RobotContainer

### Low Priority (Code Quality)
10. ✅ Create helper classes for repeating patterns
11. ✅ Use enums for elevator positions
12. ✅ Create motor configuration factory
13. ✅ Extract inline lambdas to named commands

---

## Conclusion

This codebase has many common mistakes that teams make when learning command-based programming. The core issues are:

1. **Subsystems doing too much** - They manage state and behavior instead of just hardware
2. **Commands doing too little** - Using runOnce() instead of proper command lifecycle
3. **Missing requirements** - Commands don't require subsystems, causing conflicts
4. **Poor separation of concerns** - Business logic scattered across Robot, RobotContainer, and Subsystems

By following the WPILib command-based paradigm correctly:
- Commands become composable and testable
- Subsystems become simple hardware abstractions
- The command scheduler can properly manage conflicts
- Code becomes much easier to understand and maintain

Focus on these principles:
- **Commands control behavior**
- **Subsystems control hardware**
- **RobotContainer composes everything**
- **Always require subsystems**
- **Keep periodic() minimal**
