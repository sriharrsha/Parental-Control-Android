package in.ac.vit.sriharrsha.accesibilityscreenshot;


import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.media.Image;
import android.media.Image.Plane;
import android.media.ImageReader;
import android.media.ImageReader.OnImageAvailableListener;
import android.os.Handler;
import android.os.Trace;

import junit.framework.Assert;

import in.ac.vit.sriharrsha.accesibilityscreenshot.env.ImageUtils;
import in.ac.vit.sriharrsha.accesibilityscreenshot.env.Logger;

import java.util.List;

/**
 * Class that takes in preview frames and converts the image to Bitmaps to process with Tensorflow.
 */
public class TensorflowImageListener{
    private static final Logger LOGGER = new Logger();

    private static final boolean SAVE_PREVIEW_BITMAP = true;

    private static final String MODEL_FILE = "file:///android_asset/tensorflow_inception_graph.pb";
    private static final String LABEL_FILE =
            "file:///android_asset/imagenet_comp_graph_label_strings.txt";

    private static final int NUM_CLASSES = 1001;
    private static final int INPUT_SIZE = 224;
    private static final int IMAGE_MEAN = 117;

    // TODO(andrewharp): Get orientation programatically.
    private final int screenRotation = 90;

    private final TensorflowClassifier tensorflow = new TensorflowClassifier();

    private int previewWidth = 0;
    private int previewHeight = 0;
    private byte[][] yuvBytes;
    private int[] rgbBytes = null;
    private Bitmap rgbFrameBitmap = null;
    private Bitmap croppedBitmap = null;

    private boolean computing = false;
    private Handler handler;

   // private RecognitionScoreView scoreView;

    public void initialize(final AssetManager assetManager,
           // final RecognitionScoreView scoreView,
                           final Handler handler) {
        tensorflow.initializeTensorflow(
                assetManager, MODEL_FILE, LABEL_FILE, NUM_CLASSES, INPUT_SIZE, IMAGE_MEAN);
//        this.scoreView = scoreView;
        this.handler = handler;
    }

    private void drawResizedBitmap(final Bitmap src, final Bitmap dst) {
        Assert.assertEquals(dst.getWidth(), dst.getHeight());
        final float minDim = Math.min(src.getWidth(), src.getHeight());

        final Matrix matrix = new Matrix();

        // We only want the center square out of the original rectangle.
        final float translateX = -Math.max(0, (src.getWidth() - minDim) / 2);
        final float translateY = -Math.max(0, (src.getHeight() - minDim) / 2);
        matrix.preTranslate(translateX, translateY);

        final float scaleFactor = dst.getHeight() / minDim;
        matrix.postScale(scaleFactor, scaleFactor);

        // Rotate around the center if necessary.
        if (screenRotation != 0) {
            matrix.postTranslate(-dst.getWidth() / 2.0f, -dst.getHeight() / 2.0f);
            matrix.postRotate(screenRotation);
            matrix.postTranslate(dst.getWidth() / 2.0f, dst.getHeight() / 2.0f);
        }

        final Canvas canvas = new Canvas(dst);
        canvas.drawBitmap(src, matrix, null);
    }

    public void classifyScreenshot(Bitmap screenshot) {
            Trace.beginSection("imageAvailable");
            // Initialize the storage bitmaps once when the resolution is known.
            if (previewWidth != screenshot.getWidth() || previewHeight != screenshot.getHeight()) {
                previewWidth = screenshot.getWidth();
                previewHeight = screenshot.getHeight();
                LOGGER.i("Initializing at size %dx%d", previewWidth, previewHeight);
                rgbBytes = new int[previewWidth * previewHeight];
                screenshot=Bitmap.createBitmap(screenshot.getWidth(), screenshot.getHeight(), Config.ARGB_8888);
                croppedBitmap = Bitmap.createBitmap(INPUT_SIZE, INPUT_SIZE, Config.ARGB_8888);
            }

        rgbFrameBitmap.setPixels(rgbBytes, 0, previewWidth, 0, 0, previewWidth, previewHeight);
        drawResizedBitmap(screenshot, croppedBitmap);

        // For examining the actual TF input.
        if (SAVE_PREVIEW_BITMAP) {
            ImageUtils.saveBitmap(croppedBitmap);
        }

        handler.post(
                new Runnable() {
                    @Override
                    public void run() {
                        final List<Classifier.Recognition> results = tensorflow.recognizeImage(croppedBitmap);

                        LOGGER.v("%d results", results.size());
                        for (final Classifier.Recognition result : results) {
                            LOGGER.v("Result: " + result.getTitle());
                        }
                        //scoreView.setResults(results);
                        computing = false;
                    }
                });

        Trace.endSection();
    }
}