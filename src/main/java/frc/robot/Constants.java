package frc.robot;

public class Constants {
    public static class CoralEndeffector {
        public static final int LEndeffectorSparkmaxId = 16;
        public static final int REndeffectorSparkmaxId = 19;
        
        public static final double IntakeSpeed = 0.3;
        public static final double kL1Speed = 0.4;
        public static final double kL23Speed = 0.4;
        public static final double kL4Speed = 0.4;
    }
    public static class Laser{
        public static final int fLaserId = 0;
        public static final int bLaserId = 1;
    }
    public static class AlgaeEndeffector{
        public static final int IntakeArmSparkmaxid = 20;
        public static final int IntakeWheelsUSparkmaxid = 21;
        public static final int IntakeWheelsLSparkmaxid = 22;

        public static final double IntakeWheelSpeed = 0.5;
        public static final double IntakeWheelReverseSpeed = -0.5;
        public static final double IntakeArmSpeed = 0.5;
        public static final double IntakeArmPosition = 0;
    }
    public static class Elevetor{
        public static final int ElevetorLSparkmaxId = 23;
        public static final int ElevetorRSparkmaxId = 24;
    }
    public static class Leds{

    }
}
