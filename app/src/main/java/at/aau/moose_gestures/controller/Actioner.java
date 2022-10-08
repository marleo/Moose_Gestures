package at.aau.moose_gestures.controller;

import static android.view.MotionEvent.ACTION_DOWN;
import static android.view.MotionEvent.ACTION_MOVE;
import static android.view.MotionEvent.ACTION_POINTER_DOWN;
import static android.view.MotionEvent.ACTION_POINTER_UP;
import static android.view.MotionEvent.ACTION_UP;
import static android.view.MotionEvent.INVALID_POINTER_ID;

import static at.aau.moose_gestures.data.Consts.TECHNIQUE.DRAG;
import static at.aau.moose_gestures.data.Consts.TECHNIQUE.FLICK;

import android.annotation.SuppressLint;
import android.graphics.PointF;
import android.os.CountDownTimer;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.webkit.WebView;

import at.aau.moose_gestures.data.Config;
import at.aau.moose_gestures.data.Consts.*;
import at.aau.moose_gestures.data.Memo;
import at.aau.moose_gestures.tools.Logs;
import at.aau.moose_gestures.tools.MetaLogInfo;


public class Actioner {
    private final String NAME = "Actioner/";
    // -------------------------------------------------------------------------------

    private static Actioner instance; // Singelton instance

    // Mode of scrolling
    private TECHNIQUE mActiveTechnique = DRAG;

    private final int PPI = 312; // For calculating movement in mm

    // Algorithm parameters
    private int leftmostId = INVALID_POINTER_ID; // Id of the left finger
    private int leftmostIndex = INVALID_POINTER_ID; // Index of the leftmost finger
    private int actionIndex = INVALID_POINTER_ID; // New finger's index
    private PointF lastPoint;
    private int nTouchPoints; // = touchPointCounter in Demi's code
    private int mActivePointerId = INVALID_POINTER_ID;
    private int mNumMovePoints = 0;
    private PointF mLeftmostTouchPoint;
    private PointF mLastTouchPoint;
    private double[] mLastVelocities = new double[]{};
    private int mTotalDistanceX = 0;
    private int mTotalDistanceY = 0;
    private boolean mAutoscroll = false;
    private long mTimeLastMoved;
    private boolean mContinueScroll = false;
    private double THRSH_MM = 1.0; // Threshold to ignore less than

    // Config
    private int mDragSensitivity = 2; // Count every n ACTION_MOVEs
    private double mDragGain = 20; // Gain factor for drag
    private double mRBGain = 1.5; // Gain factor for rate-based
    private int mRBSensititivity = 1; // Count every n ACTION_MOVEs (rate-based)
    private int mRBDenom = 50; // Denominator in RB's speed formula
    private double mFlickCoef = 0.3; // dX, dY returned from webView * coef -> Desktop

    //Mario
    private static final int NONE = 0;
    private static final int SWIPE = 1;
    private int mode = NONE;
    private float startX;
    private float startY;
    private float stopX;
    private float stopY;
    private static final int SWIPE_THRESHOLD = Config.SWIPE_THRESHOLD;

    //Tap
    private int leftPointerID = INVALID_POINTER_ID; // Id of the left finger
    private long actionStartTime; // Both for time keeping and checking if action is started
    private MetaLogInfo mMetaLogInfo = new MetaLogInfo();
    private final String TAG = "Moose_Actioner";
    private CountDownTimer tapTimer;


    // Is virtually pressed?
    private boolean vPressed = false;


    // Views
    private WebView mWebView;

    // -------------------------------------------------------------------------------

    /**
     * Get the Singleton instance
     * @return Actioner instance
     */
    public static Actioner get() {
        if (instance == null) instance = new Actioner();
        return instance;
    }

    public enum ACTION {
        PRESS_PRI,
        RELEASE_PRI,
        CANCEL
    }


    /**
     * Set the config
     * @param memo Memo from Desktop
     */
    public void config(Memo memo) {
        final String TAG = NAME + "config";
        Logs.d(TAG, memo);
        switch (memo.getMode()) {
        case STRINGS.TECHNIQUE: {
            mActiveTechnique = TECHNIQUE.get(memo.getValue1Int());
            Logs.d(TAG, "New Technique", mActiveTechnique.toString());

            break;
        }

        case STRINGS.SENSITIVITY: {
            if (mActiveTechnique.equals(TECHNIQUE.DRAG))
                mDragSensitivity = memo.getValue1Double();
            if (mActiveTechnique.equals(TECHNIQUE.RATE_BASED))
                mRBSensititivity = memo.getValue1Double();

            break;
        }

        case STRINGS.GAIN: {
            if (mActiveTechnique.equals(TECHNIQUE.DRAG))
                mDragGain = memo.getValue1Double();
            if (mActiveTechnique.equals(TECHNIQUE.RATE_BASED))
                mRBGain = memo.getValue1Double();

            break;
        }

        case STRINGS.DENOM: {
            mRBDenom = memo.getValue1Int();
            break;
        }

        case STRINGS.COEF: {
            mFlickCoef = memo.getValue1Double();
            break;
        }
        }
    }

    /**
     * Set the WebView
     * @param view View (got from MainAActivity)
     * @param pagePath Path to the html file
     */
    @SuppressLint("ClickableViewAccessibility")
    public void setWebView(View view, String pagePath) {
        String TAG = NAME + "setWebView";

        mWebView = (WebView) view;
        mWebView.getSettings().setAllowFileAccess(true);
        mWebView.loadUrl(pagePath);
        mWebView.scrollTo(200000, 200000);

        // Scrolling listners
        mWebView.setOnTouchListener((v, event) -> false);
        mWebView.setOnScrollChangeListener(new flickWebViewScrollListener());
    }

    /**
     * Perform the action
     * @param event MotionEvent to process and perform
     * @param mid Unique id for the event
     */
    public void scroll(MotionEvent event, int mid) {
        switch(event.getAction() & MotionEvent.ACTION_MASK) {
            case MotionEvent.ACTION_POINTER_DOWN: {
                mode = SWIPE;
                startY = event.getY();
                startX = event.getX();
                break;
            }
            case MotionEvent.ACTION_POINTER_UP: {
                mode = NONE;
                if(Math.abs(startY - stopY) > SWIPE_THRESHOLD) {
                    Memo memo;
                    if(startY > stopY) {    //swipe up
                        memo = new Memo(STRINGS.SCROLL, "swipeUp", 100, 100);
                    } else {     //swipe down
                        memo = new Memo(STRINGS.SCROLL, "swipeDown", 0, 0);
                    }
                    if(Config.MONITOR_JUMP_MODE.equals("SWIPE"))
                        Networker.get().sendMemo(memo);
                } else if(Math.abs(startX - stopX) > SWIPE_THRESHOLD) {
                    Memo memo;
                    if(startX > stopX) {     //swipe left
                        memo = new Memo(STRINGS.SCROLL, "swipeLeft", 100, 0);
                    } else {    //swipe right
                        memo = new Memo(STRINGS.SCROLL, "swipeRight", 0, 100);
                    }
                    if(Config.MONITOR_JUMP_MODE.equals("SWIPE"))
                        Networker.get().sendMemo(memo);
                }
            }
            case MotionEvent.ACTION_MOVE: {
                if(mode == SWIPE) {
                    stopY = event.getY(0);
                    stopX = event.getX(0);
                }
                break;
            }
        }
    }

    public void tapLClick(MotionEvent me, int meId) {
        int leftIndex, actionIndex;
        float leftDY;

        switch (me.getActionMasked()) {
            // Only one finger is on the screen
            case MotionEvent.ACTION_DOWN:
                leftIndex = 0; // The only finger
                leftPointerID = me.getPointerId(leftIndex); // ID
                // Set the start coords
                mMetaLogInfo.startPointerCoords = getPointerCoords(me, leftIndex);
                actionStartTime = System.currentTimeMillis(); // Set the start TAP time

                mMetaLogInfo.startMeId = meId; // Set the start id

                // Pressed
                vPressed = true; // Flag
                //mssgPublisher.onNext(ACTION.PRESS_PRI.name()); // Send the action
                Log.d(TAG, "--------------- Pressed ---------------");
                startTapTimer();

                break;

            // More fingers are added
            case MotionEvent.ACTION_POINTER_DOWN:
                actionIndex = me.getActionIndex(); // Which pointer is down?
                // If new finger on the left
                if (isLeftMost(me, actionIndex)) {
                    leftPointerID =  me.getPointerId(actionIndex); // Set ID
                    actionStartTime = System.currentTimeMillis(); // Set the start TAP time
                    // Update the start coords
                    me.getPointerCoords(actionIndex, mMetaLogInfo.startPointerCoords);
                    mMetaLogInfo.startMeId = meId; // Set the start id

                    // Pressed
                    vPressed = true; // Flag
                    //mssgPublisher.onNext(ACTION.PRESS_PRI.name()); // Send the action
                    Log.d(TAG, "--------------- Pressed ---------------");
                    startTapTimer();
                }
                break;

            // Movement...
            case MotionEvent.ACTION_MOVE:
                if (leftPointerID != INVALID_POINTER_ID) { // We have a leftmost finger
//                printPointers(me); // TEST
                    leftIndex = me.findPointerIndex(leftPointerID);
                    if (leftIndex != -1) { // ID found

                        // Always calculated the amount of movement of the leftmost finger
                        leftDY = me.getY(leftIndex) - mMetaLogInfo.startPointerCoords.y;

                        Log.d(TAG, String.format("getY = %.2f | startY = %.2f",
                                me.getY(leftIndex), mMetaLogInfo.startPointerCoords.y));
                        Log.d(TAG, String.format("dY = %.2f | MIN = %.2f",
                                leftDY, Config.SWIPE_LCLICK_DY_MIN));
                        // Did it move too much?
                        if (leftDY >= Config.TAP_LCLICK_DIST_MAX) { // SWIPED!
                            Log.d(TAG, "******** leftDY: " + leftDY);
                            vPressed = false; // Flag
                            Log.d(TAG, "--------------- Cancelled by Distance ---------------");
                            //mssgPublisher.onNext(ACTION.CANCEL.name()); // Send the action

                            // Log if not already logged
                            if (mMetaLogInfo.startMeId != -1) {
                                // End attribs
                                mMetaLogInfo.duration = (int)
                                        (System.currentTimeMillis() - actionStartTime);
                                mMetaLogInfo.endMeId = meId;
                                mMetaLogInfo.endPointerCoords = getPointerCoords(me, 0);
                                mMetaLogInfo.setDs(); // Set dX and dY based on
                                mMetaLogInfo.relCan = 1; // Cancelled
                                Log.d(TAG, "start id= " + mMetaLogInfo.startMeId);
                                Log.d(TAG, "end id= " + mMetaLogInfo.endMeId);

                                mMetaLogInfo.startMeId = -1; // Reset the start id
                                mMetaLogInfo.endMeId = -1; // Reset the end id

                            }

                        }

                    }
                }

                break;

            // Second, third, ... fingers are up
            case MotionEvent.ACTION_POINTER_UP:
                // Check if the active finger has gone up
                actionIndex = me.getActionIndex();
                if (me.getPointerId(actionIndex) == leftPointerID) { // Leftmost finger is UP

                    mMetaLogInfo.endPointerCoords = getPointerCoords(me, actionIndex);
                    double dist = distance(
                            mMetaLogInfo.startPointerCoords,
                            mMetaLogInfo.endPointerCoords);
                    int duration = (int) (System.currentTimeMillis() - actionStartTime);

                    Log.d(TAG, "duration = " + duration +
                            " | MAX = " + Config.TAP_LCLICK_TIMEOUT);
                    Log.d(TAG, String.format("distance = %.2f | MAX = %.2f",
                            dist, Config.TAP_LCLICK_DIST_MAX));

                    if (vPressed) {
                        Log.d(TAG, "*************** 1");
                        if (dist <= Config.TAP_LCLICK_DIST_MAX) {
                            Log.d(TAG, "--------------- Released --------------- (Tap)");
                            //mssgPublisher.onNext(Strs.ACT_RELEASE_PRI); // Publilsh the event
                            Memo memo = new Memo(STRINGS.SCROLL, "tap", 0, 0);
                            Networker.get().sendMemo(memo);

                            // Save log info
                            mMetaLogInfo.duration = duration;
                            mMetaLogInfo.endMeId = meId;
                            mMetaLogInfo.setDs();
                            mMetaLogInfo.relCan = 0;
                        }
                    }

                    // Reset everything
                    leftPointerID = INVALID_POINTER_ID;
                    vPressed = false;
                    mMetaLogInfo.startMeId = -1;
                    mMetaLogInfo.endMeId = -1;
                }
                break;

            // Last finger is up
            case MotionEvent.ACTION_UP:
                // Was it a single-finger TAP?
                if (leftPointerID == 0) {
                    actionIndex = me.findPointerIndex(leftPointerID);
                    mMetaLogInfo.endPointerCoords = getPointerCoords(me, actionIndex);
                    double dist = distance(
                            mMetaLogInfo.startPointerCoords,
                            mMetaLogInfo.endPointerCoords);
                    int duration = (int) (System.currentTimeMillis() - actionStartTime);

                    Log.d(TAG, "duration = " + duration +
                            " | MAX = " + Config.TAP_LCLICK_TIMEOUT);
                    Log.d(TAG, String.format("distance = %.2f | MAX = %.2f",
                            dist, Config.TAP_LCLICK_DIST_MAX));

                    if (vPressed) {
                        Log.d(TAG, "*************** 2");

                        if (dist <= Config.TAP_LCLICK_DIST_MAX &&
                                duration <= Config.TAP_LCLICK_TIMEOUT) {
                            Log.d(TAG, "--------------- Released --------------- (Tap)");
                            //mssgPublisher.onNext(Strs.ACT_RELEASE_PRI); // Publilsh the event
                            Memo memo = new Memo(STRINGS.SCROLL, "tap", 0, 0);
                            Networker.get().sendMemo(memo);

                            // Save log info
                            mMetaLogInfo.duration = duration;
                            mMetaLogInfo.setDs();
                            mMetaLogInfo.endMeId = meId;

                        }
                    }
                }

                // Reset everything
                leftPointerID = INVALID_POINTER_ID;
                vPressed = false;
                mMetaLogInfo.startMeId = -1;
                mMetaLogInfo.endMeId = -1;

                break;

        }
    }

    private void startTapTimer() {
        tapTimer = new CountDownTimer(Config.TAP_LCLICK_TIMEOUT, 100) {
            @Override
            public void onTick(long millisUntilFinished) {}

            @Override
            public void onFinish() {
                if (vPressed) {
                    Log.d(TAG, "--------------- Cancelled by Time ---------------");
                    //mssgPublisher.onNext(Strs.ACT_CANCEL); // Send a CANCEL message
                    vPressed = false;

                    // Save log info
                    mMetaLogInfo.duration = Config.TAP_LCLICK_TIMEOUT;
                    mMetaLogInfo.endMeId = -1;
                    mMetaLogInfo.setDs();
                    mMetaLogInfo.relCan = 1; // Cancelled
                    Log.d(TAG, "start id= " + mMetaLogInfo.startMeId);
                    Log.d(TAG, "end id= " + mMetaLogInfo.endMeId);
                }
            }
        }.start();
    }



    /******
     * Class for managing scroll in webView
     */
    private class flickWebViewScrollListener implements View.OnScrollChangeListener {
        final String TAG = NAME + "flickWebViewScrollListener";

        @Override
        public void onScrollChange(View v,
                                   int scrollX, int scrollY, int oldScrollX, int oldScrollY) {
            if (oldScrollY != scrollY) Log.d(TAG, "Y= " + oldScrollY + " -> " + scrollY);
            if (oldScrollX != scrollX) Log.d(TAG, "X= " + oldScrollX + " -> " + scrollX);
            final double dY = (scrollY - oldScrollY) * mFlickCoef * (-1);
            final double dX = (scrollX - oldScrollX) * mFlickCoef * (-1);

            Networker.get().sendMemo(new Memo(STRINGS.SCROLL, FLICK, dY, dX));
        }
    }


    /**
     * Get a new MotionEvent to send to webView for Flick
     * @param oldEvent Old event to base
     * @return New MotionEvent
     */
    private MotionEvent getNewEvent(MotionEvent oldEvent) {
        final int newPointerCount = 1;
        if (mActivePointerId != INVALID_POINTER_ID) {
            final int activeIndex = oldEvent.findPointerIndex(mActivePointerId);
            int newAction = ACTION_DOWN;

            switch (oldEvent.getActionMasked()) {
            case ACTION_POINTER_DOWN: newAction = ACTION_DOWN;
                break;
            case ACTION_POINTER_UP: newAction = ACTION_UP;
                break;
            case ACTION_MOVE: newAction = ACTION_MOVE;
                break;
            }

            final MotionEvent.PointerProperties[] newProps =
                    new MotionEvent.PointerProperties[newPointerCount];
            newProps[0] = new MotionEvent.PointerProperties();
            oldEvent.getPointerProperties(activeIndex, newProps[0]);

            final MotionEvent.PointerCoords[] newCoords =
                    new MotionEvent.PointerCoords[newPointerCount];
            newCoords[0] = new MotionEvent.PointerCoords();
            oldEvent.getPointerCoords(activeIndex, newCoords[0]);

            final MotionEvent newEvent = MotionEvent.obtain(
                    oldEvent.getDownTime(), oldEvent.getEventTime(),
                    newAction, newPointerCount,
                    newProps, newCoords,
                    oldEvent.getMetaState(), oldEvent.getButtonState(),
                    oldEvent.getXPrecision(), oldEvent.getYPrecision(),
                    oldEvent.getDeviceId(), oldEvent.getEdgeFlags(),
                    oldEvent.getSource(), oldEvent.getFlags()
            );

            return newEvent;
        } else {
            return MotionEvent.obtain(oldEvent);
        }
    }

    private void resetFlick() {
        mLastVelocities = new double[]{0.0, 0.0, 0.0};
        mTotalDistanceX = 0;
        mTotalDistanceY = 0;
        mTimeLastMoved = System.currentTimeMillis();
        if (mAutoscroll) {
            stopScroll();
            mAutoscroll = false;
        }
    }

    /**
     * Stop any scrolling (no matter the technique)
     */
    private void stopScroll() {
        Networker.get().sendMemo(Memo.RB_STOP_MEMO);
    }

    /**
     * Check if a pointer is leftmost
     * @param me MortionEvent
     * @param pointerIndex index of the pointer to check
     * @return boolean
     */
    public boolean isLeftMost(MotionEvent me, int pointerIndex) {
        return findLeftMostIndex(me) == pointerIndex;
    }

    /**
     * Find the index of leftmost pointer
     * @param me MotionEvent
     * @return Index of the leftmost pointer
     */
    public int findLeftMostIndex(MotionEvent me) {
        String TAG = NAME + "findLeftMostIndex";

        int nPointers = me.getPointerCount();
        Logs.d(TAG, "nPointers", me.getPointerCount());
        if (nPointers == 0) return -1;
        if (nPointers == 1) return 0;

        // > 1 pointers (POINTER_DOWN or POINTER_UP)
        int lmIndex = 0;
        for (int pix = 0; pix < me.getPointerCount(); pix++) {
            if (me.getX(pix) < me.getX(lmIndex)) lmIndex = pix;
        }

        return lmIndex;
    }

    /**
     * Find the id of the leftmost pointer
     * @param me MotionEvent
     * @return Id of the leftmost pointer
     */
    private int findLeftMostId(MotionEvent me) {
        int lmIndex = findLeftMostIndex(me);
        if (lmIndex == -1) return INVALID_POINTER_ID;
        else return me.getPointerId(lmIndex);
    }

    /**
     * Update the leftmost properties and lastPoint
     */
    private void updatePointers(MotionEvent me) {
        String TAG = NAME + "updatePointers";

        leftmostIndex = findLeftMostIndex(me);
        leftmostId = me.getPointerId(leftmostIndex);
        lastPoint = new PointF(me.getX(leftmostIndex), me.getY(leftmostIndex));

        Logs.d(TAG, "ind|id|point", leftmostIndex, leftmostId, lastPoint.x);
    }

    /**
     * Truly GET the PointerCoords!
     * @param me MotionEvent
     * @param pointerIndex Pointer index
     * @return PointerCoords
     */
    public MotionEvent.PointerCoords getPointerCoords(MotionEvent me, int pointerIndex) {
        MotionEvent.PointerCoords result = new MotionEvent.PointerCoords();
        me.getPointerCoords(pointerIndex, result);
        return result;
    }

    private double px2mm(double px) {
        return (px / PPI) * 25.4;
    }

    /**
     * Calculate the Euclidean distance between two coords
     * @param pc1 PointerCoords 1
     * @param pc2 PointerCoords 2
     * @return Double distance
     */
    public double distance(MotionEvent.PointerCoords pc1,
                           MotionEvent.PointerCoords pc2) {
        return Math.sqrt(Math.pow(pc1.x - pc2.x, 2) + Math.pow(pc1.y - pc2.y, 2));
    }

}
