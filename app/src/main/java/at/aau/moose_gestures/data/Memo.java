package at.aau.moose_gestures.data;

import static at.aau.moose_gestures.data.Consts.STRINGS.*;

import android.util.Log;

import androidx.annotation.NonNull;

import at.aau.moose_gestures.tools.Logs;

public class Memo {
    private static final String NAME = "Memo/";

    private String action;
    private String mode;
    private String value1;
    private String value2;

    public static Memo RB_STOP_MEMO = new Memo(SCROLL, Consts.TECHNIQUE.RATE_BASED, STOP, STOP);

    /**
     * Constructor
     * @param act Action Mostly "SCROLL"
     * @param md Mode DRAG or RT
     * @param v1 String value1
     * @param v2 String value2
     */
    public Memo(String act, String md, String v1, String v2) {
        action = act;
        mode = md;
        value1 = v1;
        value2 = v2;
    }

    public Memo(String act, Consts.TECHNIQUE tech, String v1, String v2) {
        this(act, tech.toString(), v1, v2);
    }

    public Memo(String act, Consts.TECHNIQUE tech, double v1, double v2) {
        this(act, tech.toString(), v1, v2);
    }

    /**
     * Constructor
     * @param act Action Mostly "SCROLL"
     * @param md Mode DRAG or RT
     * @param v1 Double value Movement along X
     * @param v2 Double value Movement along Y
     */
    public Memo(String act, String md, double v1, double v2) {
        action = act;
        mode = md;
        value1 = String.valueOf(v1);
        value2 = String.valueOf(v2);
    }

    /**
     * Basic consrtuctor
     */
    public Memo() {
        action = "";
        mode = "";
        value1 = "";
        value2 = "";
    }

    /**
     * Return action
     * @return String Action
     */
    public String getAction() {
        return action;
    }

    /**
     * Return mode
     * @return String Mode
     */
    public String getMode() {
        return mode;
    }

    /**
     * Convert and return the value 1
     * @return Int value 1
     */
    public int getValue1Int() {
        try {
            return (int) Double.parseDouble(value1);
        } catch (NumberFormatException e) {
            return 0;
        }

    }

    /**
     * Convert and return the value 2
     * @return Int value 2
     */
    public int getValue2Int() {
        try {
            return (int) Double.parseDouble(value2);
        } catch (NumberFormatException e) {
            return 0;
        }

    }

    /**
     * Convert and return the value 1
     * @return Double value 1
     */
    public int getValue1Double() {
        try {
            return (int) Double.parseDouble(value1);
        } catch (NumberFormatException e) {
            return 0;
        }

    }

    /**
     * Convert and return the value 2
     * @return Double value 2
     */
    public int getValue2Double() {
        try {
            return (int) Double.parseDouble(value2);
        } catch (NumberFormatException e) {
            return 0;
        }

    }


    /**
     * Get the Memo from String
     * @param mssg String
     * @return Memo
     */
    public static Memo valueOf(String mssg) {
        String TAG = NAME + "valueOf";

        Memo result = new Memo();
        if (mssg != null) {
            String[] parts = mssg.split(SP);
            Logs.d(TAG, parts.length);
            if (parts.length == 4) {
                result.action = parts[0];
                result.mode = parts[1];
                result.value1 = parts[2];
                result.value2 = parts[3];
            } else {
                Log.d(TAG, "Problem in parsing the memo!");
            }
        }

        return result;
    }

    /**
     * Get the String equivaluent
     * @return String
     */
    @NonNull
    @Override
    public String toString() {
        return action + SP + mode + SP + value1 + SP + value2;
    }
}
