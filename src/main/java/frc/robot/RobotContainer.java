// Copyright (c) FIRST and other WPILib contributors.
// Open Source Software; you can modify and/or share it under the terms of
// the WPILib BSD license file in the root directory of this project.

package frc.robot;

import static edu.wpi.first.units.Units.*;

import com.ctre.phoenix6.swerve.SwerveModule.DriveRequestType;
import com.ctre.phoenix6.swerve.SwerveRequest;

import com.pathplanner.lib.auto.AutoBuilder;
import com.pathplanner.lib.auto.NamedCommands;
import com.pathplanner.lib.events.EventTrigger;

import edu.wpi.first.math.geometry.Rotation2d;
import edu.wpi.first.wpilibj.smartdashboard.SendableChooser;

import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;
import edu.wpi.first.wpilibj2.command.Command;
import edu.wpi.first.wpilibj2.command.Commands;
import edu.wpi.first.wpilibj2.command.button.CommandXboxController;
import edu.wpi.first.wpilibj2.command.sysid.SysIdRoutine.Direction;
import frc.robot.Commands.CoralCommands;
import frc.robot.generated.TunerConstants;
import frc.robot.subsystems.CommandSwerveDrivetrain;
import frc.robot.subsystems.ElevetorSubsystem;
import frc.robot.subsystems.EndeffectorSubsystem;


public class RobotContainer {
    private double MaxSpeed = TunerConstants.kSpeedAt12Volts.in(MetersPerSecond) * 0.25; // kSpeedAt12Volts desired top speed
    private double MaxAngularRate = RotationsPerSecond.of(0.75).in(RadiansPerSecond); // 3/4 of a rotation per second max angular velocity

    //instantiates the robot container
    public static EndeffectorSubsystem m_EndeffectorSubsystem = new EndeffectorSubsystem(); 
    public static ElevetorSubsystem m_ElevetorSubsystem = new ElevetorSubsystem(); 

    /* Setting up bindings for necessary control of the swerve drive platform */
    private final SwerveRequest.FieldCentric drive = new SwerveRequest.FieldCentric()
            .withDeadband(MaxSpeed * 0.1).withRotationalDeadband(MaxAngularRate * 0.1) // Add a 10% deadband
            .withDriveRequestType(DriveRequestType.OpenLoopVoltage); // Use open-loop control for drive motors
    private final SwerveRequest.SwerveDriveBrake brake = new SwerveRequest.SwerveDriveBrake();
    private final SwerveRequest.PointWheelsAt point = new SwerveRequest.PointWheelsAt();

    private final Telemetry logger = new Telemetry(MaxSpeed);

    private final CommandXboxController joystick = new CommandXboxController(0);

    public final CommandSwerveDrivetrain drivetrain = TunerConstants.createDrivetrain();
    
    /* Path follower */
    private final SendableChooser<Command> autoChooser;
    

    public RobotContainer() {
        autoChooser = AutoBuilder.buildAutoChooser("Tests");
        SmartDashboard.putData("Auto Mode", autoChooser);

        new EventTrigger("a").toggleOnTrue(CoralCommands.scoreL4(m_ElevetorSubsystem, m_EndeffectorSubsystem));

        configureBindings();
        configureSwerveBindings();
        autoFactory = drivetrain.createAutoFactory();
        autoFactory.bind("Score", CoralCommands.scoreL4(m_ElevetorSubsystem, m_EndeffectorSubsystem));
        autoRoutines = new AutoRoutines(autoFactory);
        autoChooser.addRoutine("SimplePath", autoRoutines::simplePathAuto);
        SmartDashboard.putData("Auto Chooser", autoChooser);
    }

    private void configureSwerveBindings() {
        // Note that X is defined as forward according to WPILib convention,
        // and Y is defined as to the left according to WPILib convention.
        drivetrain.setDefaultCommand(
            // Drivetrain will execute this command periodically
            drivetrain.applyRequest(() ->
                drive.withVelocityX(joystick.getLeftY() * MaxSpeed*m_ElevetorSubsystem.robotSpeedCoefficient) // Drive forward with negative Y (forward)
                    .withVelocityY(joystick.getLeftX() * MaxSpeed*m_ElevetorSubsystem.robotSpeedCoefficient) // Drive left with negative X (left)
                    .withRotationalRate(joystick.getRightX() * MaxAngularRate) // Drive counterclockwise with negative X (left)
            )
        );

        //joystick.a().whileTrue(drivetrain.applyRequest(() -> brake));
        //joystick.b().whileTrue(drivetrain.applyRequest(() ->point.withModuleDirection(new Rotation2d(-joystick.getLeftY(), -joystick.getLeftX()))));

        // Run SysId routines when holding back/start and X/Y.
        // Note that each routine should be run exactly once in a single log.
        joystick.back().and(joystick.y()).whileTrue(drivetrain.sysIdDynamic(Direction.kForward));
        joystick.back().and(joystick.x()).whileTrue(drivetrain.sysIdDynamic(Direction.kReverse));
        joystick.start().and(joystick.y()).whileTrue(drivetrain.sysIdQuasistatic(Direction.kForward));
        joystick.start().and(joystick.x()).whileTrue(drivetrain.sysIdQuasistatic(Direction.kReverse));

        // reset the field-centric heading on left bumper press
        //joystick.leftBumper().onTrue(drivetrain.runOnce(() -> drivetrain.seedFieldCentric()));

        drivetrain.registerTelemetry(logger::telemeterize);
    }

    private void configureBindings() {
        //Button dpadDownButton = new Button(() -> driverController.getPOV() == 180);

        joystick.a().onTrue(Commands.runOnce((m_EndeffectorSubsystem::runEndEffector)));
        joystick.b().onTrue(Commands.runOnce((m_EndeffectorSubsystem::runIntake)));
        joystick.x().onTrue(Commands.runOnce((m_EndeffectorSubsystem::openCloseIntakeArm)));
        
        joystick.leftBumper().onTrue(Commands.runOnce((m_EndeffectorSubsystem::reverseIntakeWheels)));
        joystick.rightBumper().onTrue(Commands.runOnce((m_EndeffectorSubsystem::runIntakeWheels)));
        joystick.rightBumper().onFalse(Commands.runOnce((m_EndeffectorSubsystem::stopIntakeWheels)));
        joystick.leftBumper().onFalse(Commands.runOnce((m_EndeffectorSubsystem::stopIntakeWheels)));

        joystick.pov(0).onTrue(Commands.runOnce((m_ElevetorSubsystem::goToElevatorStow)));
        joystick.pov(90).onTrue(Commands.runOnce((m_ElevetorSubsystem::goToprivateLevel2)));
        joystick.pov(180).onTrue(Commands.runOnce((m_ElevetorSubsystem::goToprivateLevel3)));
        joystick.pov(270).onTrue(Commands.runOnce((m_ElevetorSubsystem::goToElevatorL4)));

        joystick.rightTrigger().onTrue(Commands.runOnce(() -> m_ElevetorSubsystem.setElevatorPositionPositive(-joystick.getRightTriggerAxis())));
        joystick.leftTrigger().onTrue(Commands.runOnce(() -> m_ElevetorSubsystem.setElevatorPositionNegative(joystick.getLeftTriggerAxis())));
        joystick.rightTrigger().onFalse(Commands.runOnce(() -> m_ElevetorSubsystem.setElevetorPoscontrolTrue()));
        joystick.leftTrigger().onFalse(Commands.runOnce(() -> m_ElevetorSubsystem.setElevetorPoscontrolTrue()));


    }

    public Command getAutonomousCommand() {

        return autoChooser.getSelected();

    }
}
