package at.aau.moose_gestures.tools;

import android.util.Log;

/**
 * A kinda wrapper class for Log
 */
public class Logs {

//    public static void d(String tag, String mssg) {
//        Log.d(tag, mssg);
//    }
//
//    public static void d(String tag, Memo memo) {
//        Logs.d(tag, memo.toString());
//    }

//    public static void d(String tag, String name, int... params) {
//        if (params.length > 0) {
//            StringBuilder sb = new StringBuilder().append(">>").append(name).append(": ");
//            for(int p : params) {
//                sb.append(p).append(" | ");
//            }
//            Log.d(tag, sb.toString());
//        }
//    }

//    public static void d(String tag, int... params) {
//        if (params.length > 0) {
//            StringBuilder sb = new StringBuilder().append(">>");
//            for(int p : params) {
//                sb.append(p).append(" | ");
//            }
//            Log.d(tag, sb.toString());
//        }
//    }

    public static void d(String tag, Object... params) {
        if (params.length > 0) {
            StringBuilder sb = new StringBuilder();
            for(Object p : params) {
                sb.append(p).append(" | ");
            }
            Log.d(tag, sb.toString());
        }
    }

//    public static void d(String tag, String name, double... params) {
//        if (params.length > 0) {
//            StringBuilder sb = new StringBuilder().append(">>").append(name).append(": ");
//            for(double p : params) {
//                sb.append(p).append(" | ");
//            }
//            Log.d(tag, sb.toString());
//        }
//    }
//
//    public static void d(String tag, String name, String... params) {
//        if (params.length > 0) {
//            StringBuilder sb = new StringBuilder().append(">>").append(name).append(": ");
//            for(String p : params) {
//                sb.append(p).append(" | ");
//            }
//            Log.d(tag, sb.toString());
//        }
//    }


}
