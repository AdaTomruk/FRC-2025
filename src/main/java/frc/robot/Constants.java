package frc.robot;

public class Constants {
    public static class CoralEndeffector {
        public static final int LeftEndeffectorSparkmaxId = 16;
        public static final int RightEndeffectorSparkmaxId = 19;
        
        public static final double IntakeSpeed = 0.2;
        public static final double kL1LSpeed = 0.2;
        public static final double kL1RSpeed = 0.2;
        public static final double kL2Speed = 0.4;
        public static final double kL3Speed = 0.4;
        public static final double kL4Speed = 0.4;

        public static final double kP = 0.15;
        public static final double kI = 0;
        public static final double kD = 0.0;
        public static final double kIZone = 5.0;
        public static final double kG = 0.5;
        
        public static final double kMaxVelocity = 65;
        public static final double kMaxAcceleration = 200;
    }
    public static class Laser{
        public static final int fLaserId = 23;
        public static final int bLaserId = 24;
    }
    public static class AlgaeEndeffector{
        public static final int IntakeArmSparkmaxid = 20;
        public static final int IntakeWheelsUpperSparkmaxid = 17;
        public static final int IntakeWheelsLowerSparkmaxid = 18;

        public static final double IntakeWheelSpeed = 0.5;
        public static final double IntakeWheelReverseSpeed = -0.5;
        public static final double IntakeArmSpeed = 0.5;
        public static final double IntakeArmPosition = 0;

        //Alge arm PID
        public static final double kP = 0.15;
        public static final double kI = 0;
        public static final double kD = 0.0;
        public static final double kIZone = 5.0;
        public static final double kG = 0.5;

        public static final double kMaxVelocity = 65;
        public static final double kMaxAcceleration = 200;

        //Alge wheel PID
        public static final double kBP = 0.15;
        public static final double kBI = 0;
        public static final double kBD = 0.0;
        public static final double kBIZone = 5.0;
        public static final double kBG = 0.5;

        public static final double kBMaxVelocity = 65;
        public static final double kBMaxAcceleration = 200;

        public static final int kMaxCurrent = 40;

        public static  double kArmClosedPosition = 0.0;
        public static  double kArmOpenPosition = 15.0;
    }
    public static class Elevator{
        public static final int ElevetorLeftSparkmaxId = 21;
        public static final int ElevetorRightSparkmaxId = 22;

        public static final double kP = 0.15;
        public static final double kI = 0;
        public static final double kD = 0.0;
        public static final double kIZone = 5.0;
        public static final double kG = 0.5;
        public static final double kMaxVelocity = 65;
        public static final double kMaxAcceleration = 200;

        public static final int kMaxCurrent = 40;

        public static final double kMinHeight = 0.0;
        public static  double kStowHeight = 0.0;
        public static  double kL2Height = 9.0;
        public static  double kL3Height = 25.14;
        public static  double kL4Height = 52.0;
        public static  double kMaxHeight = 56.2;
        public static  double kGroundAlgaeHeight = 0.0;
        public static  double kScoreAlgaeHeight = 0.0;
        public static  double kLowAlgaeHeight = 24.8;
        public static  double kHighAlgaeHeight = 42.5;

        public static final double kDrivetrainL2ElevetorHeightDriveSpeedCoefficient = 0.5;
        public static final double kDrivetrainL3ElevetorHeightDriveSpeedCoefficient = 0.5;
        public static final double kDrivetrainL4ElevetorHeightDriveSpeedCoefficient = 0.5;
        public static final double kDrivetrainAlgaeLowElevetorHeightDriveSpeedCoefficient = 0.5;
        public static final double kDrivetrainAlgaeHighElevetorHeightDriveSpeedCoefficient = 0.5;
    }
    public static class Leds{

    }
}
