package at.aau.moose_gestures.data;

import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.MotionEvent;

public class Config {

    //================================================================================
    // MonitorJump
    public static String MONITOR_JUMP_MODE = "SWIPE"; // TAP or SWIPE or SLIDE
    public static boolean MONITOR_JUMP_DEBUG = true; // Debug mode

    //TAP
    public static int TAP_WIDTH_LEFT = 180; // Width of the tap area in px
    public static int TAP_WIDTH_RIGHT = 100; // Width of the tap area in px
    public static int TAP_HEIGHT = 0; // Height of the tap area in px - 0 = full screen
    public static String TAP_PLACEMENT = "CENTER"; // CENTER, TOP or BOTTOM

    //SLIDE
    public static int SLIDE_WIDTH = 200; // Width of the slide area in px
    public static int SLIDE_HEIGHT = 750; // Height of the slide area in px - 0 = full screen
    public static String SLIDE_SIDE = "RIGHT"; // LEFT or RIGHT
    public static String SLIDE_PLACEMENT = "CENTER"; // CENTER, TOP or BOTTOM
    public static int SLIDE_MIN_DISTANCE_Y = 50; // Minimum distance in px on Y-Axis - best at 100 - increase over screenheight to disable
    public static int SLIDE_MIN_DISTANCE_X = 50; // Minimum distance in px on X-Axis - best at 50 - increase over screenwidth to disable
    public static int SLIDE_MIN_THRESHOLD_X = 0; // Minimum Travel distance (for bezel case)
    public static int SLIDE_BEZEL_TOLERANCE = 10; // if motionEvent.getRawX() can't reach device border
    public static boolean SLIDE_DIRECTION = true; // true = up->right/right->right, false = down->right/right->left

    public static int SWIPE_THRESHOLD = 100;
    //================================================================================

    private static final String TAG = "Moose_Config";
    //=======================================================

    // Server
    public static final String SERVER_IP = "192.168.2.1";
//    public static final String SERVER_IP = "192.168.178.34";
    public static final int SERVER_Port = 8000;
//    public static final int TIMEOUT = 2 * 60 * 1000; // 2 min

    // Thresholds ------------------------------------
    public static final int SWIPE_LCLICK_DY_MIN_MM = 3; // mm
    public static final int TAP_LCLICK_DIST_MAX_MM = 5; // mm
    public static float SWIPE_LCLICK_DY_MIN; // px
    public static float TAP_LCLICK_DIST_MAX; // px

    public static final int TAP_LCLICK_TIMEOUT = 200; // ms

    public static final int PALM_AREA_Y = 1080; // px (from the top)

    // -----------------------------------------------

    public static float multip;

    // Sizes  ----------------------------------------
    public static final int TAP_REGION_H_MM = 80; // mm
    public static float _tapRegionH; // px
    // -----------------------------------------------

    /**
     * Set the pixel equivalent of mm values
     * and set the multip for later use
     * @param dm DisplayMetrics of the current device
     */
    public static void setPxValues(DisplayMetrics dm) {
        multip = TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_MM, 1, dm);

        SWIPE_LCLICK_DY_MIN = SWIPE_LCLICK_DY_MIN_MM * multip;
        TAP_LCLICK_DIST_MAX = TAP_LCLICK_DIST_MAX_MM * multip;

        _tapRegionH = TAP_REGION_H_MM * multip;

        Log.d(TAG, "Constants ============");
        Log.d(TAG, "Min dY = " + SWIPE_LCLICK_DY_MIN + " px");
        Log.d(TAG, "======================");
    }

    /**
     * Convert pixel value to mm
     * @param pxValue float value in px
     * @return float - value in mm (if multiplicant not set => same as input)
     */
    public static float pxToMM(float pxValue) {
        if (multip > 0) return pxValue / multip;
        else return pxValue;
    }

    /**
     * Given an action int, returns a string description
     * @param action MotionEvent action
     * @return String description of the input action
     */
    public static String actionToString(int action) {
        switch (action) {
            case MotionEvent.ACTION_DOWN: return "ACTION_DOWN";
            case MotionEvent.ACTION_MOVE: return "ACTION_MOVE";
            case MotionEvent.ACTION_POINTER_DOWN: return "ACTION_POINTER_DOWN";
            case MotionEvent.ACTION_UP: return "ACTION_UP";
            case MotionEvent.ACTION_POINTER_UP: return "ACTION_POINTER_UP";
            case MotionEvent.ACTION_OUTSIDE: return "ACTION_OUTSIDE";
            case MotionEvent.ACTION_CANCEL: return "ACTION_CANCEL";
        }
        return "";
    }

    /**
     * Get the String for a PointerCoords object
     * @param pc PointerCoords
     * @return String of pc
     */
    public static String pointerCoordsToString(MotionEvent.PointerCoords pc) {
//        return "coord= (" + pc.x + "," + pc.y + ")" + " - " +
//                "orientation= " + pc.orientation + " - " +
//                "pressure= " + pc.pressure + " - " +
//                "size= " + pc.size + " - " +
//                "touchMajor= " + pc.touchMajor + " - " +
//                "touchMinor= " + pc.touchMinor;

        return "coord= (" + pc.x + "," + pc.y + ")";
    }

}
