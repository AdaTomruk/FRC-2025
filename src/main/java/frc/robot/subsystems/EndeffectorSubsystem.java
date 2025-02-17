package frc.robot.subsystems;

import com.revrobotics.spark.SparkMax;
import com.revrobotics.spark.SparkLowLevel.MotorType;

import edu.wpi.first.wpilibj2.command.Command;
import edu.wpi.first.wpilibj2.command.ScheduleCommand;
import edu.wpi.first.wpilibj2.command.SubsystemBase;
import frc.robot.Constants.AlgaeEndeffector;
import frc.robot.Constants.CoralEndeffector;

import com.revrobotics.RelativeEncoder;
import com.revrobotics.spark.SparkBase.ControlType;
import com.revrobotics.spark.SparkClosedLoopController;

import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;

public class EndeffectorSubsystem extends SubsystemBase{

    private Boolean IntakeCoralStatus = false;

    private SparkMax LEndeffectorSparkmax = new SparkMax(CoralEndeffector.LeftEndeffectorSparkmaxId, MotorType.kBrushless);
    private SparkMax REndeffectorSparkmax = new SparkMax(CoralEndeffector.RightEndeffectorSparkmaxId, MotorType.kBrushless);
    private SparkMax IntakeArmSparkmax = new SparkMax(AlgaeEndeffector.IntakeArmSparkmaxid, MotorType.kBrushless);
    private SparkMax IntakeWheelsUSparkmax = new SparkMax(AlgaeEndeffector.IntakeWheelsUpperSparkmaxid, MotorType.kBrushless);
    private SparkMax IntakeWheelsLSparkmax = new SparkMax(AlgaeEndeffector.IntakeWheelsLowerSparkmaxid, MotorType.kBrushless);

    private RelativeEncoder m_encoder = IntakeArmSparkmax.getEncoder();

    private SparkClosedLoopController m_pidController = IntakeArmSparkmax.getClosedLoopController();

    laserSubsystem laserSubsystem = new laserSubsystem();

    public static boolean IntakeArmStatus = false;

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
        LEndeffectorSparkmax.set(CoralEndeffector.IntakeSpeed);
        REndeffectorSparkmax.set(CoralEndeffector.IntakeSpeed);
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
        LEndeffectorSparkmax.set(CoralEndeffector.kL1Speed);
        REndeffectorSparkmax.set(CoralEndeffector.kL1Speed);
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

    private void openIntakeArm() {
        m_encoder.setPosition(2);
        IntakeArmStatus = true;
    }

    private void closeIntakeArm() {
        m_encoder.setPosition(2);
        IntakeArmStatus = false;
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


}

