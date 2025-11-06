// Copyright (c) FIRST and other WPILib contributors.
// Open Source Software; you can modify and/or share it under the terms of
// the WPILib BSD license file in the root directory of this project.

package frc.robot;

import edu.wpi.first.math.util.Units;
import edu.wpi.first.wpilibj.TimedRobot;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;
import edu.wpi.first.wpilibj2.command.Command;
import edu.wpi.first.wpilibj2.command.CommandScheduler;

public class Robot extends TimedRobot {
  private Command m_autonomousCommand;

  private final RobotContainer m_robotContainer;

  private final boolean kUseLimelight = false;
  
  // WebSocket server for Android client communication
  private RobotWebSocketServer m_webSocketServer;
  
  // Focus mode state
  private boolean m_focusModeEnabled = false;

  public Robot() {
    m_robotContainer = new RobotContainer();
    
    // Initialize WebSocket server
    try {
      m_webSocketServer = new RobotWebSocketServer(Constants.WebSocketServer.kServerPort);
      m_webSocketServer.start();
      System.out.println("WebSocket server started on port " + Constants.WebSocketServer.kServerPort);
    } catch (Exception e) {
      System.err.println("Failed to start WebSocket server: " + e.getMessage());
      e.printStackTrace();
    }
    
    // Add focus mode to SmartDashboard for control
    SmartDashboard.putBoolean("Focus Mode", false);
  }

  @Override
  public void robotPeriodic() {
    CommandScheduler.getInstance().run();
    
    // Check for focus mode changes from SmartDashboard
    boolean focusModeFromDashboard = SmartDashboard.getBoolean("Focus Mode", false);
    if (focusModeFromDashboard != m_focusModeEnabled) {
      m_focusModeEnabled = focusModeFromDashboard;
      
      // Broadcast focus mode change to all Android clients
      if (m_webSocketServer != null) {
        m_webSocketServer.broadcastFocusMode(m_focusModeEnabled);
      }
      
      System.out.println("Focus Mode " + (m_focusModeEnabled ? "ENABLED" : "DISABLED"));
    }

    /*
     * This example of adding Limelight is very simple and may not be sufficient for on-field use.
     * Users typically need to provide a standard deviation that scales with the distance to target
     * and changes with number of tags available.
     *
     * This example is sufficient to show that vision integration is possible, though exact implementation
     * of how to use vision should be tuned per-robot and to the team's specification.
     */
    if (kUseLimelight) {
      var driveState = m_robotContainer.drivetrain.getState();
      double headingDeg = driveState.Pose.getRotation().getDegrees();
      double omegaRps = Units.radiansToRotations(driveState.Speeds.omegaRadiansPerSecond);

      LimelightHelpers.SetRobotOrientation("limelight", headingDeg, 0, 0, 0, 0, 0);
      var llMeasurement = LimelightHelpers.getBotPoseEstimate_wpiBlue_MegaTag2("limelight");
      if (llMeasurement != null && llMeasurement.tagCount > 0 && Math.abs(omegaRps) < 2.0) {
        m_robotContainer.drivetrain.addVisionMeasurement(llMeasurement.pose, llMeasurement.timestampSeconds);
      }
    }
  }

  @Override
  public void disabledInit() {}

  @Override
  public void disabledPeriodic() {}

  @Override
  public void disabledExit() {}

  @Override
  public void autonomousInit() {
    m_autonomousCommand = m_robotContainer.getAutonomousCommand();

    if (m_autonomousCommand != null) {
      m_autonomousCommand.schedule();
    }
  }

  @Override
  public void autonomousPeriodic() {}

  @Override
  public void autonomousExit() {}

  @Override
  public void teleopInit() {
    if (m_autonomousCommand != null) {
      m_autonomousCommand.cancel();
    }
  }

  @Override
  public void teleopPeriodic() {}

  @Override
  public void teleopExit() {}

  @Override
  public void testInit() {
    CommandScheduler.getInstance().cancelAll();
  }

  @Override
  public void testPeriodic() {}

  @Override
  public void testExit() {}

  @Override
  public void simulationPeriodic() {}
  
  @Override
  public void close() {
    // Clean up WebSocket server
    if (m_webSocketServer != null) {
      try {
        m_webSocketServer.stop();
        System.out.println("WebSocket server stopped");
      } catch (Exception e) {
        System.err.println("Error stopping WebSocket server: " + e.getMessage());
      }
    }
    super.close();
  }
  
  /**
   * Manually set focus mode and broadcast to Android clients
   * 
   * @param enabled Whether focus mode should be enabled
   */
  public void setFocusMode(boolean enabled) {
    m_focusModeEnabled = enabled;
    SmartDashboard.putBoolean("Focus Mode", enabled);
    
    if (m_webSocketServer != null) {
      m_webSocketServer.broadcastFocusMode(enabled);
    }
  }
  
  /**
   * Get the current focus mode state
   * 
   * @return true if focus mode is enabled
   */
  public boolean isFocusModeEnabled() {
    return m_focusModeEnabled;
  }
}