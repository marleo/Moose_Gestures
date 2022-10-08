package at.aau.moose_gestures.views;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.os.Vibrator;
import android.provider.Settings;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;

import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import at.aau.moose_gestures.R;
import at.aau.moose_gestures.controller.Actioner;
import at.aau.moose_gestures.controller.AdminManager;
import at.aau.moose_gestures.controller.Networker;
import at.aau.moose_gestures.data.Config;
import at.aau.moose_gestures.data.Consts.*;
import at.aau.moose_gestures.data.Memo;

public class MainActivity extends AppCompatActivity {

    final static String TAG = "MainActivity/";
    // -------------------------------------------------------------------------------

    static boolean isAdmin = false; // is the app admin?
    static final int OVERLAY_PERMISSION_CODE = 2; // code for overlay permission intent

    private ExecutorService executorService; // for running threads
    private AlertDialog.Builder dialogBuilder; // for creating dialogs
    private AlertDialog dialog; // dialog for everyting!
    // -------------------------------------------------------------------------------

    // Main Handler
    @SuppressLint("HandlerLeak")
    private Handler mainHandler = new Handler() {
        @Override
        public void handleMessage(@NonNull Message mssg) {
            Log.d(TAG, "handleMessage: " + mssg.what);
            if (mssg.what == INTS.CLOSE_DLG) {
                if (dialog != null) dialog.dismiss();
                drawUI();
            }

//            if (mssg.what == INTS.SHOW_DLG) {
//                showDialog("Connecting to desktop...");
//            }
        }
    };
    // -------------------------------------------------------------------------------

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        requestWindowFeature(Window.FEATURE_NO_TITLE); // For removing the status bar
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Config.setPxValues(getResources().getDisplayMetrics());

        // Set webView in Actioner
        String pagePath = getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) + "/index.html";
        Actioner.get().setWebView(findViewById(R.id.webView), pagePath);

        // Setting
        executorService = Executors.newSingleThreadExecutor();
        dialogBuilder = new AlertDialog.Builder(this);
        Networker.get().setVibrator((Vibrator) getSystemService(VIBRATOR_SERVICE));
        Networker.get().setMainHandler(mainHandler);

        // Init
        checkAdmin();

        showDialog("Connecting to desktop...");
        Networker.get().connect();
    }

    /**
     * Show an AlertDialog
     * @param mssg Message to show
     */
    private void showDialog(String mssg) {
        dialog = dialogBuilder.create();
        dialog.setMessage(mssg);
        dialog.setCancelable(false);
        dialog.show();
    }

    /**
     * Draw the main UI
     */
    private void drawUI() {
        // Get the overlay permission (possible only with admin)
        if (!Settings.canDrawOverlays(this)) {
            Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + getPackageName()));
            startActivityForResult(intent, OVERLAY_PERMISSION_CODE);
        } else {
            drawTouchViewGroup();
        }
    }

    /**
     * Make sure the app has adimin permissions
     */
    private void checkAdmin() {

        //-- Get the admin permission [for removing the status bar]
        DevicePolicyManager mDPM = (DevicePolicyManager)
                getSystemService(Context.DEVICE_POLICY_SERVICE);
        ComponentName adminManager = new ComponentName(this, AdminManager.class);
        isAdmin = mDPM.isAdminActive(adminManager);
        if (!isAdmin) {
            // Launch the activity to have the user enable our admin.
            Intent intent = new Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN);
            intent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, adminManager);
            startActivityForResult(intent, 1);
        }

    }

    /**
     * Set isAdmin from outside the activity
     * @param isa is admin?
     */
    public static void setIsAdim(boolean isa) {
        isAdmin = isa;
    }

    /**
     * Draw the custom view (to apear under the status bar)
     */
    @SuppressLint("ClickableViewAccessibility")
    public void drawTouchViewGroup() {
        getWindow().getDecorView().setBackgroundColor(Color.WHITE);
        WindowManager winManager = (WindowManager) getSystemService(Context.WINDOW_SERVICE);

        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY, //TYPE_PHONE originally
                //WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE, // |
                //WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN |
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                PixelFormat.TRANSLUCENT);

        params.gravity = Gravity.TOP;
        params.width = WindowManager.LayoutParams.MATCH_PARENT;
        params.height = WindowManager.LayoutParams.MATCH_PARENT;

        TouchViewGroup view = new TouchViewGroup(this);

        view.setBackgroundColor(Color.WHITE);

        if(Objects.equals(Config.MONITOR_JUMP_MODE, "TAP")) {
            Button leftBtn = new Button(this);
            leftBtn.setText("L");
            leftBtn.setOnTouchListener ((view1, motionEvent) -> {
                if(motionEvent.getAction() == MotionEvent.ACTION_UP) {
                    Log.d("MONITOR", "TAP: " + motionEvent.getX() + " " + motionEvent.getY());
                    Log.d("MONITOR", "LEFT");
                    Memo memo = new Memo(STRINGS.SCROLL, "tapLeft", motionEvent.getX(), motionEvent.getY());
                    Networker.get().sendMemo(memo);
                    return true;
                }
                return false;
            });

            Button rightBtn = new Button(this);
            rightBtn.setText("R");
            rightBtn.setOnTouchListener ((view1, motionEvent) -> {
                if(motionEvent.getAction() == MotionEvent.ACTION_UP) {
                    Log.d("MONITOR", "TAP: " + motionEvent.getX() + " " + motionEvent.getY());
                    Log.d("MONITOR", "RIGHT");
                    Memo memo = new Memo(STRINGS.SCROLL, "tapRight", motionEvent.getX(), motionEvent.getY());
                    Networker.get().sendMemo(memo);
                    return true;
                }
                return false;
            });

            final FrameLayout.LayoutParams lBtn = new FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.WRAP_CONTENT,
                    FrameLayout.LayoutParams.WRAP_CONTENT);

            leftBtn.setLayoutParams(lBtn);
            if(Objects.equals(Config.TAP_PLACEMENT, "CENTER")) {
                lBtn.gravity = Gravity.CENTER | Gravity.LEFT;
            } else if(Objects.equals(Config.TAP_PLACEMENT, "TOP")) {
                lBtn.gravity = Gravity.TOP | Gravity.LEFT;
            } else if(Objects.equals(Config.TAP_PLACEMENT, "BOTTOM")) {
                lBtn.gravity = Gravity.BOTTOM | Gravity.LEFT;
            }
            if(Config.TAP_HEIGHT == 0) {
                lBtn.height = WindowManager.LayoutParams.MATCH_PARENT;
            } else {
                lBtn.height = Config.TAP_HEIGHT;
            }
            lBtn.width = Config.TAP_WIDTH_LEFT;

            final FrameLayout.LayoutParams rBtn = new FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.WRAP_CONTENT,
                    FrameLayout.LayoutParams.WRAP_CONTENT);

            rightBtn.setLayoutParams(rBtn);
            if(Objects.equals(Config.TAP_PLACEMENT, "CENTER")) {
                rBtn.gravity = Gravity.CENTER | Gravity.RIGHT;
            } else if(Objects.equals(Config.TAP_PLACEMENT, "TOP")) {
                rBtn.gravity = Gravity.TOP | Gravity.RIGHT;
            } else if(Objects.equals(Config.TAP_PLACEMENT, "BOTTOM")) {
                rBtn.gravity = Gravity.BOTTOM | Gravity.RIGHT;
            }
            if(Config.TAP_HEIGHT == 0) {
                rBtn.height = WindowManager.LayoutParams.MATCH_PARENT;
            } else {
                rBtn.height = Config.TAP_HEIGHT;
            }
            rBtn.width = Config.TAP_WIDTH_RIGHT;

            if(Config.MONITOR_JUMP_DEBUG) {
                leftBtn.setBackgroundColor(Color.RED);
                leftBtn.setTextColor(Color.WHITE);
                rightBtn.setTextColor(Color.WHITE);
                rightBtn.setBackgroundColor(Color.RED);
            } else {
                leftBtn.setBackgroundColor(Color.TRANSPARENT);
                leftBtn.setTextColor(Color.TRANSPARENT);
                rightBtn.setTextColor(Color.TRANSPARENT);
                rightBtn.setBackgroundColor(Color.TRANSPARENT);
            }

            view.addView(leftBtn);
            view.addView(rightBtn);
        } else if(Config.MONITOR_JUMP_MODE.equals("SLIDE")) { //TODO: COPY HERE
            final float[] slideStartY = {0};
            final float[] slideEndY = {0};
            final float[] slideStartX = {0};
            final float[] slideEndX = {0};
            final float[] slideStartRawX = {0};
            final float[] slideEndRawX = {0};


            Button slideBtn = new Button(this);
            slideBtn.setText("R");
            slideBtn.setOnTouchListener ((view1, motionEvent) -> {
                //check if user is sliding up or down more than Config.SLIDE_MIN_DISTANCE
                if(motionEvent.getAction() == MotionEvent.ACTION_DOWN) {
                    slideStartX[0] = motionEvent.getX();
                    slideStartRawX[0] = motionEvent.getRawX();
                    slideStartY[0] = motionEvent.getY();
                    return true;
                } else if(motionEvent.getAction() == MotionEvent.ACTION_UP) {
                    slideEndX[0] = motionEvent.getX();
                    slideEndY[0] = motionEvent.getY();
                    slideEndRawX[0] = motionEvent.getRawX();
                    if(Math.abs(slideEndY[0] - slideStartY[0]) > Config.SLIDE_MIN_DISTANCE_Y) {
                        Memo memo;
                        if(slideEndY[0] > slideStartY[0]
                                ||  slideStartRawX[0] >= Resources.getSystem().getDisplayMetrics().widthPixels - Config.SLIDE_BEZEL_TOLERANCE
                                &&  Math.abs(slideStartX[0] - slideEndX[0]) > Config.SLIDE_MIN_THRESHOLD_X) {
                            if(Config.SLIDE_DIRECTION) {
                                Log.d("MONITOR", "SLIDE DOWN");
                                memo = new Memo(STRINGS.SCROLL, "slideDown", motionEvent.getX(), motionEvent.getY());
                            } else {
                                Log.d("MONITOR", "SLIDE UP");
                                memo = new Memo(STRINGS.SCROLL, "slideUp", motionEvent.getX(), motionEvent.getY());
                            }
                            Networker.get().sendMemo(memo);
                        } else {
                            if(Config.SLIDE_DIRECTION) {
                                Log.d("MONITOR", "SLIDE UP");
                                memo = new Memo(STRINGS.SCROLL, "slideUp", motionEvent.getX(), motionEvent.getY());
                            } else {
                                Log.d("MONITOR", "SLIDE DOWN");
                                memo = new Memo(STRINGS.SCROLL, "slideDown", motionEvent.getX(), motionEvent.getY());
                            }
                            Networker.get().sendMemo(memo);
                        }
                    } else if(Math.abs(slideEndX[0] - slideStartX[0]) > Config.SLIDE_MIN_DISTANCE_X
                            || (slideEndRawX[0] >= Resources.getSystem().getDisplayMetrics().widthPixels - Config.SLIDE_BEZEL_TOLERANCE
                            &&  Math.abs(slideEndX[0] - slideStartX[0]) > Config.SLIDE_MIN_THRESHOLD_X)) {

                        Log.d("MONITOR", "EndX: " + motionEvent.getRawX() + "Width: " + Resources.getSystem().getDisplayMetrics().widthPixels);
                        Memo memo;
                        if(slideEndX[0] > slideStartX[0]) {
                            if(!Config.SLIDE_DIRECTION) {
                                Log.d("MONITOR", "SLIDE DOWN");
                                memo = new Memo(STRINGS.SCROLL, "slideDown", motionEvent.getX(), motionEvent.getY());
                            } else {
                                Log.d("MONITOR", "SLIDE UP");
                                memo = new Memo(STRINGS.SCROLL, "slideUp", motionEvent.getX(), motionEvent.getY());
                            }
                            Networker.get().sendMemo(memo);
                        } else {
                            if(!Config.SLIDE_DIRECTION) {
                                Log.d("MONITOR", "SLIDE UP");
                                memo = new Memo(STRINGS.SCROLL, "slideUp", motionEvent.getX(), motionEvent.getY());
                            } else {
                                Log.d("MONITOR", "SLIDE DOWN");
                                memo = new Memo(STRINGS.SCROLL, "slideDown", motionEvent.getX(), motionEvent.getY());
                            }
                            Networker.get().sendMemo(memo);
                        }
                    }
                    return true;
                }
                return false;
            });

            final FrameLayout.LayoutParams slideParams = new FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.WRAP_CONTENT,
                    FrameLayout.LayoutParams.WRAP_CONTENT);

            slideBtn.setLayoutParams(slideParams);

            if(Objects.equals(Config.SLIDE_PLACEMENT, "CENTER")) {
                if(Config.SLIDE_SIDE.equals("LEFT")) {
                    slideParams.gravity = Gravity.CENTER | Gravity.LEFT;
                } else if(Config.SLIDE_SIDE.equals("RIGHT")) {
                    slideParams.gravity = Gravity.CENTER | Gravity.RIGHT;
                }
            } else if(Objects.equals(Config.SLIDE_PLACEMENT, "TOP")) {
                if(Config.SLIDE_SIDE.equals("LEFT")) {
                    slideParams.gravity = Gravity.TOP | Gravity.LEFT;
                } else if(Config.SLIDE_SIDE.equals("RIGHT")) {
                    slideParams.gravity = Gravity.TOP | Gravity.RIGHT;
                }
            } else if(Objects.equals(Config.SLIDE_PLACEMENT, "BOTTOM")) {
                if(Config.SLIDE_SIDE.equals("LEFT")) {
                    slideParams.gravity = Gravity.BOTTOM | Gravity.LEFT;
                } else if(Config.SLIDE_SIDE.equals("RIGHT")) {
                    slideParams.gravity = Gravity.BOTTOM | Gravity.RIGHT;
                }
            }

            if(Config.SLIDE_HEIGHT == 0) {
                slideParams.height = WindowManager.LayoutParams.MATCH_PARENT;
            } else {
                slideParams.height = Config.SLIDE_HEIGHT;
            }

            slideParams.width = Config.SLIDE_WIDTH;

            if(Config.MONITOR_JUMP_DEBUG) {
                slideBtn.setBackgroundColor(Color.RED);
                slideBtn.setTextColor(Color.WHITE);
            } else {
                slideBtn.setBackgroundColor(Color.TRANSPARENT);
                slideBtn.setTextColor(Color.TRANSPARENT);
            }

            view.addView(slideBtn);
        }

        assert winManager != null;
        winManager.addView(view, params);
    }

    /**
     * Custom view class
     */
    private class TouchViewGroup extends FrameLayout {

        public TouchViewGroup(Context context) {
            super(context);
        }

        /**
         * Intercept the touches on the view
         * @param ev - MotionEvent
         * @return Always true (to pass the events to children)
         */
        @Override
        public boolean onInterceptTouchEvent(MotionEvent ev) {
            if (ev.getY() <= getStBarHeight()) {
                // Redraw the layout
                getWindow().setFlags(
                        WindowManager.LayoutParams.FLAG_FULLSCREEN,
                        WindowManager.LayoutParams.FLAG_FULLSCREEN);
                startActivity(getIntent());
            }

            return false;
        }

        @SuppressLint("ClickableViewAccessibility")
        @Override
        public boolean onTouchEvent(MotionEvent event) {
            Log.d("Motion",event.toString());
            Actioner.get().tapLClick(event, 0);
            Actioner.get().scroll(event, 0);
            return super.onTouchEvent(event);
        }
    }

    /**
     * Get the height of status bar
     * @return int (px)
     */
    private int getStBarHeight() {
        int result = 0;
        int resourceId = getResources().getIdentifier(
                "status_bar_height", "dimen", "android");
        if (resourceId > 0) {
            result = getResources().getDimensionPixelSize(resourceId);
        }

        return result;
    }


    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        int action = event.getAction();
        int keyCode = event.getKeyCode();
//        switch (keyCode) {
//        case KeyEvent.KEYCODE_VOLUME_UP:
//            if (action == KeyEvent.ACTION_DOWN) {
//                Actioner.get().setmActiveTechnique(TECHNIQUE.RATE_BASED);
//            }
//            return true;
//        case KeyEvent.KEYCODE_VOLUME_DOWN:
//            if (action == KeyEvent.ACTION_DOWN) {
//                Actioner.get().setmActiveTechnique(TECHNIQUE.DRAG);
//            }
//            return true;
//        default:
//            return super.dispatchKeyEvent(event);
//        }

        return false;
    }

}