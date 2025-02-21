package frc.robot.Commands;

import edu.wpi.first.wpilibj2.command.Command;
import edu.wpi.first.wpilibj2.command.Commands;
import edu.wpi.first.wpilibj2.command.WaitUntilCommand;
import frc.robot.subsystems.ElevetorSubsystem;
import frc.robot.subsystems.EndeffectorSubsystem;

public class CoralCommands {

    // ✅ Intake Coral Command
    public static Command IntakeCoral(EndeffectorSubsystem endeffectorSubsystem) {
        return Commands.sequence(
            Commands.runOnce(endeffectorSubsystem::runIntake),
            new WaitUntilCommand(() -> !endeffectorSubsystem.IntakeStatus) // Waits until IntakeStatus is false
        );
    }

    // ✅ Score Coral Command
    public static Command scoreCoral(EndeffectorSubsystem endeffectorSubsystem) {
        return Commands.sequence(
            Commands.runOnce(endeffectorSubsystem::runEndEffector),
            new WaitUntilCommand(() -> !endeffectorSubsystem.EndEffectorStatus) // Waits until EndEffectorStatus is false
        );
    }

    // ✅ Score at Level 4 and Return to Stow
    public static Command scoreL4(ElevetorSubsystem elevetorSubsystem, EndeffectorSubsystem endeffectorSubsystem) {
        return Commands.sequence(
            // Move to L4
            Commands.runOnce(elevetorSubsystem::goToElevatorL4),
            new WaitUntilCommand(() -> !elevetorSubsystem.isElevetorActive),

            // Score Coral
            scoreCoral(endeffectorSubsystem),

            // Return to Stow
            Commands.runOnce(elevetorSubsystem::goToElevatorStow),
            new WaitUntilCommand(() -> !elevetorSubsystem.isElevetorActive)
        );
    }
}
