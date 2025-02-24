package frc.robot.subsystems;

import com.revrobotics.spark.SparkMax;
import com.revrobotics.spark.config.SparkBaseConfig.IdleMode;
import com.revrobotics.spark.config.SparkMaxConfig;
import com.revrobotics.spark.SparkLowLevel.MotorType;

import edu.wpi.first.wpilibj2.command.SubsystemBase;
import frc.robot.Constants;
import frc.robot.Constants.AlgaeEndeffector;
import frc.robot.Constants.CoralEndeffector;

import java.util.concurrent.BlockingDeque;

import com.revrobotics.RelativeEncoder;
import com.revrobotics.spark.ClosedLoopSlot;
import com.revrobotics.spark.SparkBase;
import com.revrobotics.spark.SparkBase.PersistMode;
import com.revrobotics.spark.SparkBase.ResetMode;
import com.revrobotics.spark.SparkClosedLoopController;
import com.revrobotics.spark.SparkClosedLoopController.ArbFFUnits;

import edu.wpi.first.math.trajectory.TrapezoidProfile;
import edu.wpi.first.wpilibj.Timer;

import edu.wpi.first.networktables.NetworkTableEntry;
import edu.wpi.first.wpilibj.smartdashboard.SendableChooser;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;


/**
 * Subsystem for controlling the robot's end effector mechanisms.
 * This includes intake arms, intake wheels, and end effector motors.
 * 
 * The subsystem manages:
 * - Two end effector motors (left and right)
 * - Intake arm motor for position control
 * - Upper and lower intake wheel motors
 * 
 * Features:
 * - PID control for precise positioning of intake arm
 * - Motion profiling using trapezoidal trajectories
 * - Automatic intake stopping based on laser sensor feedback
 * - SmartDashboard integration for runtime parameter tuning
 * 
 * Control States:
 * - Intake arm: Open/Close positions
 * - Intake wheels: Forward/Reverse/Stop
 * - End effector: Active/Inactive
 * 
 * Safety Features:
 * - Current limiting on all motors
 * - Brake mode when disabled
 * - Limit switch protection
 * 
 * Dependencies:
 * - Requires laser subsystem for object detection
 * - Uses SparkMax motor controllers
 * - Relies on Constants class for configuration parameters
 * 
 * @see LaserSubsystem
 * @see Constants.AlgaeEndeffector
 * @see Constants.CoralEndeffector
 * 
 * @version 1.0
 * @since 2025-02-24
 */

public class EndeffectorSubsystem extends SubsystemBase{

    boolean intakeWheelRunFlag = false;

    private PeriodicIO mPeriodicIO;

    private Boolean IntakeCoralStatus = false;

    private SparkMax LEndeffectorSparkmax = new SparkMax(CoralEndeffector.LeftEndeffectorSparkmaxId, MotorType.kBrushless);
    private SparkMax REndeffectorSparkmax = new SparkMax(CoralEndeffector.RightEndeffectorSparkmaxId, MotorType.kBrushless);
    private SparkMax IntakeArmSparkmax = new SparkMax(AlgaeEndeffector.IntakeArmSparkmaxid, MotorType.kBrushless);
    private SparkMax IntakeWheelUSparkmax = new SparkMax(AlgaeEndeffector.IntakeWheelUpperSparkmaxid, MotorType.kBrushless);
    private SparkMax IntakeWheelLSparkmax = new SparkMax(AlgaeEndeffector.IntakeWheelLowerSparkmaxid, MotorType.kBrushless);

    private RelativeEncoder IntakeArmEncoder;
    private RelativeEncoder IntakeWheelUEncoder;
    private RelativeEncoder RightEndeffectorEncoder;
    private RelativeEncoder LeftEndeffectorEncoder;

    private SparkClosedLoopController IntakeArmPIDController; 
    private SparkClosedLoopController IntakeWheelUPIDController;
    private SparkClosedLoopController RightEndeffectorPIDController;
    private SparkClosedLoopController LeftEndeffectorPIDController;

    laserSubsystem laserSubsystem = new laserSubsystem();

    public static boolean IntakeArmStatus = false;

    public static boolean IntakeStatus = false;
    public static boolean EndEffectorStatus = false;

    private TrapezoidProfile intakeWheelTrapezoidProfile;
    private TrapezoidProfile.State intakeWheelTrapezoidCurState = new TrapezoidProfile.State();
    private TrapezoidProfile.State intakeWheelTrapezoidGoalState = new TrapezoidProfile.State();
    private double intakeWheelprevUpdateTime = Timer.getFPGATimestamp();
    
    private TrapezoidProfile intakeArmTrapezoidProfile;
    private TrapezoidProfile.State intakeArmTrapezoidCurState = new TrapezoidProfile.State();
    private TrapezoidProfile.State intakeArmTrapezoidGoalState = new TrapezoidProfile.State();
    private double intakeArmprevUpdateTime = Timer.getFPGATimestamp();

    private TrapezoidProfile endEffectorLeftTrapezoidProfile;
    private TrapezoidProfile.State endEffectorLeftTrapezoidCurState = new TrapezoidProfile.State();
    private TrapezoidProfile.State endEffectorLeftTrapezoidGoalState = new TrapezoidProfile.State();
    private double endEffectorLeftprevUpdateTime = Timer.getFPGATimestamp();

    private TrapezoidProfile endEffectorRightTrapezoidProfile;
    private TrapezoidProfile.State endEffectorRightTrapezoidCurState = new TrapezoidProfile.State();
    private TrapezoidProfile.State endEffectorRightTrapezoidGoalState = new TrapezoidProfile.State();
    private double endEffectorRightprevUpdateTime = Timer.getFPGATimestamp();



    public EndeffectorSubsystem() {

        SmartDashboard.putNumber("Intake Arm Open Position", Constants.AlgaeEndeffector.kArmOpenPosition);
        SmartDashboard.putNumber("Intake Arm Closed Position", Constants.AlgaeEndeffector.kArmClosedPosition);

        mPeriodicIO = new PeriodicIO();
        configureSparkmax();
        
    }

    private void configureSparkmax(){
        //Intake Arm PID configuration
        SparkMaxConfig IntakeArmConfig = new SparkMaxConfig();

        IntakeArmConfig.closedLoop
        .pid(Constants.AlgaeEndeffector.kP, Constants.AlgaeEndeffector.kI, Constants.AlgaeEndeffector.kD)
        .iZone(Constants.AlgaeEndeffector.kIZone);

        IntakeArmConfig.smartCurrentLimit(Constants.AlgaeEndeffector.kMaxCurrent);

        IntakeArmConfig.idleMode(IdleMode.kBrake);
        IntakeArmConfig.limitSwitch.reverseLimitSwitchEnabled(true);

        IntakeArmEncoder = IntakeArmSparkmax.getEncoder();
        IntakeArmPIDController = IntakeArmSparkmax.getClosedLoopController();

        IntakeArmSparkmax.configure(
            IntakeArmConfig,
            ResetMode.kResetSafeParameters,
            PersistMode.kPersistParameters);

            intakeArmTrapezoidProfile = new TrapezoidProfile(
                new TrapezoidProfile.Constraints(
                    Constants.AlgaeEndeffector.kMaxVelocity,
                    Constants.AlgaeEndeffector.kMaxAcceleration));

        //----------------------------------------------------------------------------------------------------
        //Intake Wheels PID configuration
        SparkMaxConfig IntakeWheelConfig = new SparkMaxConfig();

        IntakeWheelConfig.closedLoop
        .pid(Constants.AlgaeEndeffector.kBP, Constants.AlgaeEndeffector.kBI, Constants.AlgaeEndeffector.kBD)
        .iZone(Constants.AlgaeEndeffector.kBIZone);

        IntakeWheelConfig.smartCurrentLimit(Constants.AlgaeEndeffector.kBMaxCurrent);

        IntakeWheelConfig.idleMode(IdleMode.kBrake);
        IntakeWheelConfig.limitSwitch.reverseLimitSwitchEnabled(true);

        // Intake Wheels Upper MOTOR
        IntakeWheelUEncoder = IntakeWheelUSparkmax.getEncoder();
        IntakeWheelUPIDController = IntakeWheelUSparkmax.getClosedLoopController();
        IntakeWheelUSparkmax.configure(
            IntakeWheelConfig,
            ResetMode.kResetSafeParameters,
            PersistMode.kPersistParameters);

        // Intake Wheels Lower MOTOR
        IntakeWheelUSparkmax.configure(
            IntakeWheelConfig.follow(IntakeWheelLSparkmax, true),
            ResetMode.kResetSafeParameters,
            PersistMode.kPersistParameters);

            intakeWheelTrapezoidProfile = new TrapezoidProfile(
            new TrapezoidProfile.Constraints(
                Constants.AlgaeEndeffector.kBMaxVelocity,
                Constants.AlgaeEndeffector.kBMaxAcceleration));

        //----------------------------------------------------------------------------------------------------
        //Endeffector Left PID configuration
        SparkMaxConfig EndeffectorLeftConfig = new SparkMaxConfig();

        EndeffectorLeftConfig.closedLoop
        .pid(Constants.CoralEndeffector.kP, Constants.CoralEndeffector.kI, Constants.CoralEndeffector.kD)
        .iZone(Constants.CoralEndeffector.kIZone);

        EndeffectorLeftConfig.smartCurrentLimit(Constants.CoralEndeffector.kMaxCurrent);

        EndeffectorLeftConfig.idleMode(IdleMode.kBrake);
        EndeffectorLeftConfig.limitSwitch.reverseLimitSwitchEnabled(true);

        // Endeffector Left MOTOR
        LeftEndeffectorEncoder = LEndeffectorSparkmax.getEncoder();
        LeftEndeffectorPIDController = LEndeffectorSparkmax.getClosedLoopController();
        LEndeffectorSparkmax.configure(
            EndeffectorLeftConfig,
            ResetMode.kResetSafeParameters,
            PersistMode.kPersistParameters);

            endEffectorLeftTrapezoidProfile = new TrapezoidProfile(
                new TrapezoidProfile.Constraints(
                    Constants.CoralEndeffector.kMaxVelocity,
                    Constants.CoralEndeffector.kMaxAcceleration));

        //----------------------------------------------------------------------------------------------------
        //Endeffector Right PID configuration
        SparkMaxConfig EndeffectorRightConfig = new SparkMaxConfig();

        EndeffectorRightConfig.closedLoop
        .pid(Constants.CoralEndeffector.kP, Constants.CoralEndeffector.kI, Constants.CoralEndeffector.kD)
        .iZone(Constants.CoralEndeffector.kIZone);

        EndeffectorRightConfig.smartCurrentLimit(Constants.CoralEndeffector.kMaxCurrent);

        EndeffectorRightConfig.idleMode(IdleMode.kBrake);
        EndeffectorRightConfig.limitSwitch.reverseLimitSwitchEnabled(true);
        
        // Endeffector Right MOTOR
        RightEndeffectorEncoder = REndeffectorSparkmax.getEncoder();
        RightEndeffectorPIDController = REndeffectorSparkmax.getClosedLoopController();
        REndeffectorSparkmax.configure(
            EndeffectorRightConfig,
            ResetMode.kResetSafeParameters,
            PersistMode.kPersistParameters);

            endEffectorRightTrapezoidProfile = new TrapezoidProfile(
                new TrapezoidProfile.Constraints(
                    Constants.CoralEndeffector.kMaxVelocity,
                    Constants.CoralEndeffector.kMaxAcceleration));

    }

    public boolean getIntakeCoralStatus() {
        return IntakeCoralStatus;
      }

    public void runIntake() {
        LEndeffectorSparkmax.set(CoralEndeffector.IntakeSpeed);
        REndeffectorSparkmax.set(-CoralEndeffector.IntakeSpeed);
        if (laserSubsystem.logFrontLaserMeasurement() < 40) {
            IntakeCoralStatus = true;
            IntakeStatus = false;
            stopIntake();
        }
        else{
            IntakeCoralStatus = false;
            IntakeStatus = true;
        }
    }

    public void stopIntake() {
        LEndeffectorSparkmax.set(0);
        REndeffectorSparkmax.set(0);
    }

    public void runEndEffector() {
        LEndeffectorSparkmax.setVoltage(CoralEndeffector.kL1RSpeed);
        REndeffectorSparkmax.setVoltage(-CoralEndeffector.kL1LSpeed);
        if (laserSubsystem.logFrontLaserMeasurement() > 40) {
            IntakeCoralStatus = false;
            EndEffectorStatus = false;
            stopEndEffector();
            System.out.println("EndEffector Stoped");
        }
        else{
            IntakeCoralStatus = true;
            EndEffectorStatus = true;
            System.out.println("EndEffector Running");
        }
    }

    public void stopEndEffector() {
        LEndeffectorSparkmax.set(0);
        REndeffectorSparkmax.set(0);
    }

    public void runPeriodicOutputsEndeffector() {
        if(IntakeStatus){
            runIntake();
        }
        if(EndEffectorStatus){
            runEndEffector();
        }
    }

    
    
    //----------------------------------------------------------------------------------------------------
    /*   Old code for intake wheels control, not used in the current version
    public void runIntakeWheels() {
        IntakeWheelUSparkmax.set(AlgaeEndeffector.IntakeWheelSpeed);
        IntakeWheelLSparkmax.set(AlgaeEndeffector.IntakeWheelSpeed);
    }
    public void reverseIntakeWheels() {
        IntakeWheelUSparkmax.set(AlgaeEndeffector.IntakeWheelReverseSpeed);
        IntakeWheelLSparkmax.set(AlgaeEndeffector.IntakeWheelReverseSpeed);
    }
    public void stopIntakeWheels() {
        IntakeWheelUSparkmax.set(0);
        IntakeWheelLSparkmax.set(0);

    }
    */

    public void runIntakeWheels() {
        if(intakeWheelRunFlag){
            mPeriodicIO.intake_Wheel_power = AlgaeEndeffector.IntakeWheelPower;
            mPeriodicIO.is_wheel_pos_control = false;
        }
        else{
            mPeriodicIO.is_wheel_pos_control = true;
            mPeriodicIO.wheel_target = IntakeWheelUEncoder.getPosition()-AlgaeEndeffector.IntakeWheelReverseAmount;
            System.out.println("Intake Wheels Running");
        }
    }

    public void stopIntakeWheels() {
        mPeriodicIO.is_wheel_pos_control = true;
        if(intakeWheelRunFlag){
            intakeWheelRunFlag = false;
        }
        else{
            intakeWheelRunFlag = true;
        }
    }

    //----------------------------------------------------------------------------------------------------

    private void openIntakeArm() {
        IntakeArmStatus = true;
        mPeriodicIO.state = ArmState.OPEN;
        mPeriodicIO.arm_target = Constants.AlgaeEndeffector.kArmOpenPosition;
    }

    private void closeIntakeArm() {
        IntakeArmStatus = false;
        mPeriodicIO.state = ArmState.CLOSE;
        mPeriodicIO.arm_target = Constants.AlgaeEndeffector.kArmClosedPosition;
    }

    public void openCloseIntakeArm() {
        if (IntakeArmStatus) {
            closeIntakeArm();
            System.out.println("Closing Intake Arm");
        } else {
            openIntakeArm();
            System.out.println("Opening Intake Arm");
        }
    }

    public enum ArmState {
        OPEN,
        CLOSE
      }

    private static class PeriodicIO {
        double arm_target = 0.0;
        double wheel_target = 0.0;
        double endeffector_target = 0.0;
    
        boolean is_arm_pos_control = true;
        boolean is_wheel_pos_control = true;
        boolean is_endeffector_pos_control = true;

        double intake_Wheel_power = 0.0;
    
        ArmState state = ArmState.CLOSE;
      }

    //----------------------------------------------------------------------------------------------------

    public void writePeriodicOutputsIntakeArm() {
        double curTime = Timer.getFPGATimestamp();
        double dt = curTime - intakeArmprevUpdateTime;
        intakeArmprevUpdateTime = curTime;
        if(mPeriodicIO.is_arm_pos_control){
            intakeArmTrapezoidGoalState.position = mPeriodicIO.arm_target;
            intakeArmprevUpdateTime = curTime;
            intakeArmTrapezoidCurState = intakeArmTrapezoidProfile.calculate(dt, intakeArmTrapezoidCurState, intakeArmTrapezoidGoalState);

            IntakeArmPIDController.setReference(
                intakeArmTrapezoidCurState.position,            // Target position
                SparkBase.ControlType.kPosition,       // Use position control
                ClosedLoopSlot.kSlot0,                 // Use default PID slot
                Constants.AlgaeEndeffector.kG,                 // Feedforward voltage (gravity compensation)
                ArbFFUnits.kVoltage);                  // Feedforward unit in volts
        }
    }

    public void writePeriodicOutputsIntakeWheels() {
        double curTime = Timer.getFPGATimestamp();
        double dt = curTime - intakeWheelprevUpdateTime;
        intakeWheelprevUpdateTime = curTime;
        if(mPeriodicIO.is_wheel_pos_control){
            intakeWheelTrapezoidGoalState.position = mPeriodicIO.wheel_target;
            intakeWheelprevUpdateTime = curTime;
            intakeWheelTrapezoidCurState = intakeWheelTrapezoidProfile.calculate(dt, intakeWheelTrapezoidCurState, intakeWheelTrapezoidGoalState);

            IntakeWheelUPIDController.setReference(
                intakeWheelTrapezoidCurState.position,            // Target position
                SparkBase.ControlType.kPosition,       // Use position control
                ClosedLoopSlot.kSlot0,                 // Use default PID slot
                Constants.AlgaeEndeffector.kG,                 // Feedforward voltage (gravity compensation)
                ArbFFUnits.kVoltage);                  // Feedforward unit in volts
        }
        else {
            intakeWheelTrapezoidCurState.position = IntakeWheelUEncoder.getPosition();  // Read current position
            intakeWheelTrapezoidCurState.velocity = 0;                                  // Stop motion profile updates
            IntakeWheelUSparkmax.set(mPeriodicIO.intake_Wheel_power);            // Set raw power (-1 to 1)
        }
    }

    @Override
    public void periodic(){
        writePeriodicOutputsIntakeArm();
        writePeriodicOutputsIntakeWheels();

        runPeriodicOutputsEndeffector();

        SmartDashboard.putBoolean("Intake Arm Status", IntakeArmStatus);
        SmartDashboard.putBoolean("Intake Status", IntakeStatus);
        SmartDashboard.putBoolean("EndEffector Status", EndEffectorStatus);
        SmartDashboard.putNumber("Intake Arm Position", intakeArmTrapezoidCurState.position);
        SmartDashboard.putString("Coral Status", IntakeCoralStatus.toString());

        Constants.AlgaeEndeffector.kArmOpenPosition = SmartDashboard.getNumber("Intake Arm Open Position", Constants.AlgaeEndeffector.kArmOpenPosition);
        Constants.AlgaeEndeffector.kArmClosedPosition = SmartDashboard.getNumber("Intake Arm Closed Position", Constants.AlgaeEndeffector.kArmClosedPosition);

    }

}

