package frc.robot.subsystems;

import com.revrobotics.spark.SparkMax;
import com.revrobotics.spark.SparkLowLevel.MotorType;

import edu.wpi.first.wpilibj2.command.Command;
import edu.wpi.first.wpilibj2.command.ScheduleCommand;
import edu.wpi.first.wpilibj2.command.SubsystemBase;

import com.revrobotics.RelativeEncoder;
import com.revrobotics.spark.ClosedLoopSlot;
import com.revrobotics.spark.SparkBase.ControlType;
import com.revrobotics.spark.SparkClosedLoopController;

import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;

public class EndeffectorSubsystem extends SubsystemBase{

    private Boolean IntakeCoralStatus = false;

    SparkMax LEndeffectorSparkmax = new SparkMax(16, MotorType.kBrushless);
    SparkMax REndeffectorSparkmax = new SparkMax(19, MotorType.kBrushless);
    SparkMax IntakeArmSparkmax = new SparkMax(20, MotorType.kBrushless);
    SparkMax IntakeWheelsUSparkmax = new SparkMax(21, MotorType.kBrushless);
    SparkMax IntakeWheelsLSparkmax = new SparkMax(22, MotorType.kBrushless);

    private RelativeEncoder m_encoder = IntakeArmSparkmax.getEncoder();

    private SparkClosedLoopController m_pidController = IntakeArmSparkmax.getClosedLoopController();

    laserSubsystem laserSubsystem = new laserSubsystem();

    public EndeffectorSubsystem() {
        m_pidController.setReference(0, ControlType.kPosition);
    }

    public boolean getIntakeCoralStatus() {
        return IntakeCoralStatus;
      }

    public Command runIntakeCommand() {
        return (run(() -> {runIntake();}));
    }

    public void runIntake() {
        LEndeffectorSparkmax.set(0.5);
        REndeffectorSparkmax.set(0.5);
        if (laserSubsystem.logFrontLaserMeasurement() < 40) {
            IntakeCoralStatus = true;
            stopIntake();
        }
        else{
            IntakeCoralStatus = false;
            ScheduleCommand scheduleCommand = new ScheduleCommand(runIntakeCommand());scheduleCommand.schedule();
        }
    }

    public void stopIntake() {
        LEndeffectorSparkmax.set(0);
        REndeffectorSparkmax.set(0);
    }

    public void runEndEffector() {
        LEndeffectorSparkmax.set(0.5);
        REndeffectorSparkmax.set(0.5);
        if (laserSubsystem.logBackLaserMeasurement() > 40) {
            IntakeCoralStatus = false;
            stopEndEffector();
        }
        else{
            IntakeCoralStatus = true;
            ScheduleCommand scheduleCommand = new ScheduleCommand(runEndEffectorCommand());scheduleCommand.schedule();            
        }
    }

    public void stopEndEffector() {
        LEndeffectorSparkmax.set(0);
        REndeffectorSparkmax.set(0);
    }

    public Command runEndEffectorCommand() {
        return (run(() -> {runEndEffector();}));
    }
    
    //----------------------------------------------------------------------------------------------------

    public void runIntakeWheels() {
        IntakeWheelsUSparkmax.set(0.5);
        IntakeWheelsLSparkmax.set(0.5);
    }
    public void reverseIntakeWheels() {
        IntakeWheelsUSparkmax.set(-0.5);
        IntakeWheelsLSparkmax.set(-0.5);
    }
    public void stopIntakeWheels() {
        IntakeWheelsUSparkmax.set(0);
        IntakeWheelsLSparkmax.set(0);
    }

    public void openIntakeArm() {
    m_encoder.setPosition(2);
    }


}

