package frc.robot.subsystems;

import com.revrobotics.RelativeEncoder;
import com.revrobotics.spark.ClosedLoopSlot;
import com.revrobotics.spark.SparkBase;
import com.revrobotics.spark.SparkBase.PersistMode;
import com.revrobotics.spark.SparkClosedLoopController;
import com.revrobotics.spark.SparkClosedLoopController.ArbFFUnits;
import com.revrobotics.spark.SparkLowLevel.MotorType;
import com.revrobotics.spark.SparkMax;
import com.revrobotics.spark.config.SparkBaseConfig.IdleMode;
import com.revrobotics.spark.config.SparkMaxConfig;
import com.revrobotics.spark.SparkBase.ResetMode;

import edu.wpi.first.math.trajectory.TrapezoidProfile;
import edu.wpi.first.wpilibj.Timer;
import edu.wpi.first.wpilibj2.command.Command;
import edu.wpi.first.wpilibj2.command.SubsystemBase;
import frc.robot.Constants;
import frc.robot.Constants.CoralEndeffector;
import frc.robot.Constants.Elevator;

import frc.robot.subsystems.EndeffectorSubsystem;;

public class ElevetorSubsystem extends SubsystemBase{

    private static ElevetorSubsystem mInstance;
    private PeriodicIO mPeriodicIO;

    public static ElevetorSubsystem getInstance() {
        if (mInstance == null) {
          mInstance = new ElevetorSubsystem();
        }
        return mInstance;
      }

    private RelativeEncoder ElevetorLeftEncoder;
    private SparkClosedLoopController ElevetorLeftPIDController;   

    private SparkMax ElevetorLeftSparkmax = new SparkMax(Constants.Elevator.ElevetorLeftSparkmaxId, MotorType.kBrushless);
    private SparkMax ElevetorRightSparkmax = new SparkMax(Constants.Elevator.ElevetorRightSparkmaxId, MotorType.kBrushless);

    private TrapezoidProfile TrapezoidProfile;
    private TrapezoidProfile.State TrapezoidCurState = new TrapezoidProfile.State();
    private TrapezoidProfile.State TrapezoidGoalState = new TrapezoidProfile.State();
    private double prevUpdateTime = Timer.getFPGATimestamp();

    private laserSubsystem laserSubsystem = new laserSubsystem();

    public ElevetorSubsystem() {

        mPeriodicIO = new PeriodicIO();

        SparkMaxConfig elevatorConfig = new SparkMaxConfig();

        elevatorConfig.closedLoop
        .pid(Constants.Elevator.kP, Constants.Elevator.kI, Constants.Elevator.kD)
        .iZone(Constants.Elevator.kIZone);

        elevatorConfig.smartCurrentLimit(Constants.Elevator.kMaxCurrent);

        elevatorConfig.idleMode(IdleMode.kBrake);
        elevatorConfig.limitSwitch.reverseLimitSwitchEnabled(true);

        // LEFT ELEVATOR MOTOR
        ElevetorLeftEncoder = ElevetorLeftSparkmax.getEncoder();
        ElevetorLeftPIDController = ElevetorLeftSparkmax.getClosedLoopController();
        ElevetorLeftSparkmax.configure(
            elevatorConfig,
            ResetMode.kResetSafeParameters,
            PersistMode.kPersistParameters);

        // RIGHT ELEVATOR MOTOR
        ElevetorRightSparkmax.configure(
            elevatorConfig.follow(ElevetorLeftSparkmax, true),
            ResetMode.kResetSafeParameters,
            PersistMode.kPersistParameters);

            TrapezoidProfile = new TrapezoidProfile(
            new TrapezoidProfile.Constraints(
                Constants.Elevator.kMaxVelocity,
                Constants.Elevator.kMaxAcceleration));
    }
    
    public enum ElevatorState {
        NONE,
        STOW,
        L2,
        L3,
        L4,
        A1,
        A2
      }

    private static class PeriodicIO {
        double elevator_target = 0.0;
        double elevator_power = 0.0;
    
        boolean is_elevator_pos_control = false;
    
        ElevatorState state = ElevatorState.STOW;
      }
      
    public void writePeriodicOutputs() {
        double curTime = Timer.getFPGATimestamp();
        double dt = curTime - prevUpdateTime;
        prevUpdateTime = curTime;
        if(mPeriodicIO.is_elevator_pos_control){
            TrapezoidGoalState.position = mPeriodicIO.elevator_target;
            prevUpdateTime = curTime;
            TrapezoidCurState = TrapezoidProfile.calculate(dt, TrapezoidCurState, TrapezoidGoalState);

            ElevetorLeftPIDController.setReference(
                TrapezoidCurState.position,            // Target position
                SparkBase.ControlType.kPosition,       // Use position control
                ClosedLoopSlot.kSlot0,                 // Use default PID slot
                Constants.Elevator.kG,                 // Feedforward voltage (gravity compensation)
                ArbFFUnits.kVoltage);                  // Feedforward unit in volts
        }
        else {
            TrapezoidCurState.position = ElevetorLeftEncoder.getPosition();  // Read current position
            TrapezoidCurState.velocity = 0;                                  // Stop motion profile updates
            ElevetorLeftSparkmax.set(mPeriodicIO.elevator_power);            // Set raw power (-1 to 1)
        }

    }
    
    @Override
    public void periodic(){
        writePeriodicOutputs();
    }

    public ElevatorState getState() {
        return mPeriodicIO.state;
      }
      
    public void goToElevatorStow() {
        mPeriodicIO.is_elevator_pos_control = true;
        mPeriodicIO.elevator_target = Constants.Elevator.kStowHeight;
        mPeriodicIO.state = ElevatorState.STOW;
    }
    private void goToElevatorL2() {
        mPeriodicIO.is_elevator_pos_control = true;
        mPeriodicIO.elevator_target = Constants.Elevator.kL2Height;
        mPeriodicIO.state = ElevatorState.L2;
      }
      private void goToElevatorL3() {
        mPeriodicIO.is_elevator_pos_control = true;
        mPeriodicIO.elevator_target = Constants.Elevator.kL3Height;
        mPeriodicIO.state = ElevatorState.L3;
      }
    
      public void goToElevatorL4() {
        mPeriodicIO.is_elevator_pos_control = true;
        mPeriodicIO.elevator_target = Constants.Elevator.kL4Height;
        mPeriodicIO.state = ElevatorState.L4;
      }
    
      private void goToElevatorAlgaeLow() {
        mPeriodicIO.is_elevator_pos_control = true;
        mPeriodicIO.elevator_target = Constants.Elevator.kLowAlgaeHeight;
        mPeriodicIO.state = ElevatorState.A1;
      }
    
      private void goToElevatorAlgaeHigh() {
        mPeriodicIO.is_elevator_pos_control = true;
        mPeriodicIO.elevator_target = Constants.Elevator.kHighAlgaeHeight;
        mPeriodicIO.state = ElevatorState.A2;
      }

      public void goToprivateLevel2() {
        if(EndeffectorSubsystem.IntakeArmStatus == false){
            goToElevatorL2();
        }
        else{
            goToElevatorAlgaeLow();
        }
      }

      public void goToprivateLevel3() {
        if(EndeffectorSubsystem.IntakeArmStatus == false){
            goToElevatorL3();
        }
        else{
            goToElevatorAlgaeHigh();
        }
      }

    public Boolean isItSafe(){
        boolean isSafe = false;
        if(laserSubsystem.logFrontLaserMeasurement() < 40 && laserSubsystem.logBackLaserMeasurement() < 40){
            isSafe = true;
        }
        else if(laserSubsystem.logFrontLaserMeasurement() > 40 && laserSubsystem.logBackLaserMeasurement() > 40){
            isSafe = true;
        }
        else if(laserSubsystem.logFrontLaserMeasurement() < 40 && laserSubsystem.logBackLaserMeasurement() > 40){
            isSafe = true;
        }
        else if(laserSubsystem.logFrontLaserMeasurement() > 40 && laserSubsystem.logBackLaserMeasurement() < 40){
            isSafe = false;
        }

        return isSafe;
    }
}
