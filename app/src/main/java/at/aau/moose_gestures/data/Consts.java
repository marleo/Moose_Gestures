package at.aau.moose_gestures.data;

public class Consts {

    public static class STRINGS {
        public static final String SP = ";";
        public static final String INTRO = "INTRO";
        public static final String MOOSE = "MOOSE";
        public final static String TECHNIQUE = "TECHNIQUE";
        public static final String SCROLL = "SCROLL";
        public static final String DRAG = "DRAG";
        public static final String RB = "RABA";
        public static final String STOP = "STOP";
        public final static String CONFIG = "CONFIG";
        public final static String SENSITIVITY = "SENSITIVITY";
        public final static String GAIN = "GAIN";
        public final static String DENOM = "DENOM";
        public final static String COEF = "COEF";
        public final static String LOG = "LOG";
        public final static String PID = "PID";
        public final static String BLOCK_TRIAL = "BLOCK_TRIAL";
        public final static String TSK = "TSK";
    }

    public static class INTS {
        public static final int CLOSE_DLG = 0;
        public static final int SHOW_DLG = 1;
    }

    public enum TASK {
        VERTICAL, TWO_DIM;
        private static final TASK[] values = values();
        public static TASK get(int ord) {
            if (ord < values.length) return values[ord];
            else return values[0];
        }
    }

    public enum TECHNIQUE {
        DRAG, RATE_BASED, FLICK, MOUSE;
        private static final TECHNIQUE[] values = values();
        public static TECHNIQUE get(int ord) {
            if (ord < values.length) return values[ord];
            else return values[0];
        }
    }
}
