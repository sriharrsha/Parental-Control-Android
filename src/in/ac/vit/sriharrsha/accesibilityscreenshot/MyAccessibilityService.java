package in.ac.vit.sriharrsha.accesibilityscreenshot;

import android.accessibilityservice.AccessibilityService;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;

import in.ac.vit.sriharrsha.accesibilityscreenshot.env.Logger;


public class MyAccessibilityService extends AccessibilityService {
    private static final int REQUEST_MEDIA_PROJECTION = 1;
    private static final Logger LOGGER = new Logger();
    private final TensorflowImageListener tfScreenshotListener = new TensorflowImageListener();
    private HandlerThread backgroundThread;

    /**
     * A {@link Handler} for running tasks in the background.
     */
    private Handler backgroundHandler;

    /**
     * An additional thread for running inference so as not to block the camera.
     */
    private HandlerThread inferenceThread;

    /**
     * A {@link Handler} for running tasks in the background.
     */
    private Handler inferenceHandler;

    public MyAccessibilityService() {
        super();
        LOGGER.i("Getting assets.");
        tfScreenshotListener. initialize(getAssets(), inferenceHandler);
        LOGGER.i("Tensorflow initialized.");

    }

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        startBackgroundThread();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        stopBackgroundThread();
    }

    @Override
    public void onInterrupt() {
        stopBackgroundThread();
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        final int eventType = event.getEventType();
        Process sh = null;
        String eventText = null;
        switch (eventType) {
            case AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED:
                Log.d("ACCESSIBILITY :", "Content Changed");

                try {
                    sh = Runtime.getRuntime().exec("su", null, null);
                    OutputStream os = sh.getOutputStream();
                    os.write(("/system/bin/screencap -p " + Environment.getExternalStorageDirectory() + File.separator + "accessibility.png").getBytes("ASCII"));
                    Log.d("APPSERVICE", Environment.getExternalStorageDirectory() + File.separator + "accessibility.png");
                    os.flush();
                    os.close();
                    sh.waitFor();
                    BitmapFactory.Options op = new BitmapFactory.Options();
                    op.inPreferredConfig = Bitmap.Config.ARGB_8888;
                    Bitmap screenshot = BitmapFactory.decodeFile(Environment.getExternalStorageDirectory() + File.separator + "accessibility.png");
                    tfScreenshotListener.classifyScreenshot(screenshot);



                } catch (InterruptedException e) {
                    e.printStackTrace();
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                break;
            default: Log.d("ACCESSIBILITY :","Normal Event");

        }
    }



    /**
     * Starts a background thread and its {@link Handler}.
     */
    private void startBackgroundThread() {
        backgroundThread = new HandlerThread("ImageListener");
        backgroundThread.start();
        backgroundHandler = new Handler(backgroundThread.getLooper());
        inferenceThread = new HandlerThread("InferenceThread");
        inferenceThread.start();
        inferenceHandler = new Handler(inferenceThread.getLooper());
    }

    /**
     * Stops the background thread and its {@link Handler}.
     */
    private void stopBackgroundThread() {
        backgroundThread.quitSafely();
        inferenceThread.quitSafely();
        try {
            backgroundThread.join();
            backgroundThread = null;
            backgroundHandler = null;

            inferenceThread.join();
            inferenceThread = null;
            inferenceThread = null;
        } catch (final InterruptedException e) {
            LOGGER.e(e, "Exception!");
        }
    }



}
