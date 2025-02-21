package frc.robot;

import choreo.auto.AutoFactory;
import choreo.auto.AutoRoutine;
import choreo.auto.AutoTrajectory;
import edu.wpi.first.wpilibj2.command.Commands;

public class AutoRoutines {
    private final AutoFactory m_factory;

    public AutoRoutines(AutoFactory factory) {
        m_factory = factory;
    }

    public AutoRoutine simplePathAuto() {
        // Create the routine with the name "SimplePath Auto"
        final AutoRoutine routine = m_factory.newRoutine("SimplePath Auto");

        // Load the trajectory named "SimplePath" from Choreo
        final AutoTrajectory simplePath = routine.trajectory("SimplePath");

        // Reset odometry at the start and run the path
        routine.active().onTrue(
            Commands.sequence(
                simplePath.resetOdometry(),  // Ensures odometry is synced with Choreo path
                simplePath.cmd()             // Follows the path
            )
        );

        // The markers in the "SimplePath" (like "Score") will trigger bound commands
        return routine;
    }
    
}
