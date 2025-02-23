package frc.robot.subsystems;

import com.revrobotics.spark.SparkMax;
import com.revrobotics.spark.config.SparkBaseConfig.IdleMode;
import com.revrobotics.spark.config.SparkMaxConfig;
import com.revrobotics.spark.SparkLowLevel.MotorType;

import edu.wpi.first.wpilibj2.command.SubsystemBase;
import frc.robot.Constants;
import frc.robot.Constants.AlgaeEndeffector;
import frc.robot.Constants.CoralEndeffector;

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


public class EndeffectorSubsystem extends SubsystemBase{

    private PeriodicIO mPeriodicIO;

    private Boolean IntakeCoralStatus = false;

    private SparkMax LEndeffectorSparkmax = new SparkMax(CoralEndeffector.LeftEndeffectorSparkmaxId, MotorType.kBrushless);
    private SparkMax REndeffectorSparkmax = new SparkMax(CoralEndeffector.RightEndeffectorSparkmaxId, MotorType.kBrushless);
    private SparkMax IntakeArmSparkmax = new SparkMax(AlgaeEndeffector.IntakeArmSparkmaxid, MotorType.kBrushless);
    private SparkMax IntakeWheelsUSparkmax = new SparkMax(AlgaeEndeffector.IntakeWheelsUpperSparkmaxid, MotorType.kBrushless);
    private SparkMax IntakeWheelsLSparkmax = new SparkMax(AlgaeEndeffector.IntakeWheelsLowerSparkmaxid, MotorType.kBrushless);

    private RelativeEncoder IntakeArmEncoder;

    private SparkClosedLoopController IntakeArmPIDController; 

    laserSubsystem laserSubsystem = new laserSubsystem();

    public static boolean IntakeArmStatus = false;

    public static boolean IntakeStatus = false;
    public static boolean EndEffectorStatus = false;

    private TrapezoidProfile TrapezoidProfile;
    private TrapezoidProfile.State TrapezoidCurState = new TrapezoidProfile.State();
    private TrapezoidProfile.State TrapezoidGoalState = new TrapezoidProfile.State();
    private double prevUpdateTime = Timer.getFPGATimestamp();

    public EndeffectorSubsystem() {

        SmartDashboard.putNumber("Intake Arm Open Position", Constants.AlgaeEndeffector.kArmOpenPosition);
        SmartDashboard.putNumber("Intake Arm Closed Position", Constants.AlgaeEndeffector.kArmClosedPosition);

        mPeriodicIO = new PeriodicIO();

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

            TrapezoidProfile = new TrapezoidProfile(
                new TrapezoidProfile.Constraints(
                    Constants.AlgaeEndeffector.kMaxVelocity,
                    Constants.AlgaeEndeffector.kMaxAcceleration));
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

    public void runIntakeWheels() {
        IntakeWheelsUSparkmax.set(AlgaeEndeffector.IntakeWheelSpeed);
        IntakeWheelsLSparkmax.set(AlgaeEndeffector.IntakeWheelSpeed);
    }
    public void reverseIntakeWheels() {
        IntakeWheelsUSparkmax.set(AlgaeEndeffector.IntakeWheelReverseSpeed);
        IntakeWheelsLSparkmax.set(AlgaeEndeffector.IntakeWheelReverseSpeed);
    }
    public void stopIntakeWheels() {
        IntakeWheelsUSparkmax.set(0);
        IntakeWheelsLSparkmax.set(0);
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
    
        boolean is_arm_pos_control = true;
    
        ArmState state = ArmState.CLOSE;
      }

    //----------------------------------------------------------------------------------------------------

    public void writePeriodicOutputsArm() {
        double curTime = Timer.getFPGATimestamp();
        double dt = curTime - prevUpdateTime;
        prevUpdateTime = curTime;
        if(mPeriodicIO.is_arm_pos_control){
            TrapezoidGoalState.position = mPeriodicIO.arm_target;
            prevUpdateTime = curTime;
            TrapezoidCurState = TrapezoidProfile.calculate(dt, TrapezoidCurState, TrapezoidGoalState);

            IntakeArmPIDController.setReference(
                TrapezoidCurState.position,            // Target position
                SparkBase.ControlType.kPosition,       // Use position control
                ClosedLoopSlot.kSlot0,                 // Use default PID slot
                Constants.Elevator.kG,                 // Feedforward voltage (gravity compensation)
                ArbFFUnits.kVoltage);                  // Feedforward unit in volts
        }
    }

    @Override
    public void periodic(){
        writePeriodicOutputsArm();
        runPeriodicOutputsEndeffector();

        SmartDashboard.putBoolean("Intake Arm Status", IntakeArmStatus);
        SmartDashboard.putBoolean("Intake Status", IntakeStatus);
        SmartDashboard.putBoolean("EndEffector Status", EndEffectorStatus);
        SmartDashboard.putNumber("Intake Arm Position", TrapezoidCurState.position);
        SmartDashboard.putString("Coral Status", IntakeCoralStatus.toString());

        Constants.AlgaeEndeffector.kArmOpenPosition = SmartDashboard.getNumber("Intake Arm Open Position", Constants.AlgaeEndeffector.kArmOpenPosition);
        Constants.AlgaeEndeffector.kArmClosedPosition = SmartDashboard.getNumber("Intake Arm Closed Position", Constants.AlgaeEndeffector.kArmClosedPosition);

    }

}

