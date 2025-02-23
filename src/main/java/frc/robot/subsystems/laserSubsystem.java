package frc.robot.subsystems;

import au.grapplerobotics.LaserCan;
import au.grapplerobotics.ConfigurationFailedException;
import edu.wpi.first.wpilibj2.command.SubsystemBase;
import frc.robot.Constants.Laser;


public class laserSubsystem extends SubsystemBase {
  private LaserCan fLaser;
  private LaserCan bLaser;


  public laserSubsystem() {
    fLaser = new LaserCan(Laser.fLaserId); // Front Laser
    bLaser = new LaserCan(Laser.bLaserId); // Back Laser


    try {
      // Configure FLaser.
      fLaser.setRangingMode(LaserCan.RangingMode.SHORT);
      fLaser.setRegionOfInterest(new LaserCan.RegionOfInterest(8, 8, 16, 16));
      fLaser.setTimingBudget(LaserCan.TimingBudget.TIMING_BUDGET_33MS);

      // Configure BLaser
      bLaser.setRangingMode(LaserCan.RangingMode.SHORT);
      bLaser.setRegionOfInterest(new LaserCan.RegionOfInterest(8, 8, 16, 16));
      bLaser.setTimingBudget(LaserCan.TimingBudget.TIMING_BUDGET_33MS);

    } catch (ConfigurationFailedException e) {
      System.out.println("Configuration failed! " + e);
    }
  }

  public  Integer logFrontLaserMeasurement() {
    LaserCan.Measurement measurement = fLaser.getMeasurement();
    if (measurement != null && measurement.status == LaserCan.LASERCAN_STATUS_VALID_MEASUREMENT) {
      System.out.println("Front Laser: The target is " + measurement.distance_mm + "mm away!");
        return measurement.distance_mm;
    } else {
      //System.out.println("Front Laser: Oh no! The target is out of range or unreliable!");
        return 110;
    }
  }


  public  Integer logBackLaserMeasurement() {
    LaserCan.Measurement measurement = bLaser.getMeasurement();
    if (measurement != null && measurement.status == LaserCan.LASERCAN_STATUS_VALID_MEASUREMENT) {
      System.out.println("Back Laser: The target is " + measurement.distance_mm + "mm away!");
        return measurement.distance_mm;
    } else {
      //System.out.println("Back Laser: Oh no! The target is out of range or unreliable!");
        return 110;
    }
  }

  
}
