package com.example.apriltagshandler2;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.media.Image;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.widget.ImageView;
import android.widget.Toast;
import android.graphics.Matrix;
import androidx.annotation.NonNull;
import androidx.annotation.OptIn;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ExperimentalGetImage;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.google.common.util.concurrent.ListenableFuture;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {
    private static final int CAMERA_REQUEST_CODE = 200;
    private PreviewView previewView;
    private ExecutorService cameraExecutor;

    static {
        if (!OpenCVLoader.initDebug()) {
            Log.e("OpenCV", "Unable to load OpenCV!");
        } else {
            Log.d("OpenCV", "OpenCV loaded Successfully!");
        }
    }

    @SuppressLint("HardwareIds")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        previewView = findViewById(R.id.previewView);
        cameraExecutor = Executors.newSingleThreadExecutor();

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA},
                    CAMERA_REQUEST_CODE);
        } else {
            startCamera();
        }

        String androidId = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);
        Log.d("Device Id", androidId);

        try {
            // Initialize AprilTagNative
            AprilTagNative.apriltag_init("tag36h11", 0, 1.0, 0.0, 1);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CAMERA_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startCamera();
            }
        }
    }

    private void startCamera() {
        Log.d("METHOD", "private void startCamera()");
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture =
                ProcessCameraProvider.getInstance(this);

        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                bindPreviewAndAnalysis(cameraProvider);
            } catch (ExecutionException | InterruptedException e) {
                Toast.makeText(this, "Error starting camera: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void bindPreviewAndAnalysis(@NonNull ProcessCameraProvider cameraProvider) {
        Preview preview = new Preview.Builder().build();
        preview.setSurfaceProvider(previewView.getSurfaceProvider());

        CameraSelector cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA;

        ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build();

        imageAnalysis.setAnalyzer(cameraExecutor, new ImageAnalysis.Analyzer() {
            @OptIn(markerClass = ExperimentalGetImage.class)
            @Override
            public void analyze(@NonNull ImageProxy imageProxy) {
                Log.d("METHOD", "public void analyze(@NonNull ImageProxy imageProxy)");
                // Convert ImageProxy to Image
                Image image = imageProxy.getImage();
                if (image != null) {
                    processImage(image);
                }
                imageProxy.close();
            }
        });

        cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageAnalysis);
    }

    private void processImage(Image image) {
        Log.d("METHOD", "private void processImage(Image image)");
        ByteBuffer buffer = image.getPlanes()[0].getBuffer();
        byte[] bytes = new byte[buffer.remaining()];
        buffer.get(bytes);

        Mat mat = new Mat(image.getHeight(), image.getWidth(), CvType.CV_8UC1);
        mat.put(0, 0, bytes);

        Imgproc.cvtColor(mat, mat, Imgproc.COLOR_YUV2GRAY_420);

        // Convert the Mat to a byte array
        byte[] imageData = new byte[(int) (mat.total() * mat.elemSize())];
        mat.get(0, 0, imageData);

        // Call the native method to detect AprilTags
        ArrayList<AprilTagDetection> detections = AprilTagNative.apriltag_detect_yuv(imageData, mat.width(), mat.height());

        if (detections.isEmpty()) {
            Log.d("AprilTagDetection", "No AprilTags detected.");
        } else {
            Log.d("AprilTagDetection", detections.size() + " AprilTags detected.");
        }

        // Draw rectangles around detected AprilTags
        for (AprilTagDetection detection : detections) {
            Point pt1 = new Point(detection.p[0], detection.p[1]);
            Point pt2 = new Point(detection.p[4], detection.p[5]);
            Imgproc.rectangle(mat, pt1, pt2, new Scalar(0, 255, 0), 2);
            Log.d("AprilTagDetection", "Tag detected at: (" + pt1 + ", " + pt2 + ")");
        }

        // If you want to display the processed frame, you can do so by converting the Mat back to a bitmap and displaying it in an ImageView
        Bitmap bitmap = Bitmap.createBitmap(mat.cols(), mat.rows(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(mat, bitmap);

        // Rotate the bitmap according to the orientation
        bitmap = rotateBitmap(bitmap); // Adjust the angle as necessary

        Bitmap finalBitmap = bitmap;
        runOnUiThread(() -> {
            ImageView imageView = findViewById(R.id.imageView);
            imageView.setImageBitmap(finalBitmap);
        });
    }

    private Bitmap rotateBitmap(Bitmap source) {
        Matrix matrix = new Matrix();
        matrix.postRotate((float) 90);
        return Bitmap.createBitmap(source, 0, 0, source.getWidth(), source.getHeight(), matrix, true);
    }

    @Override
    protected void onPause() {
        super.onPause();
        cameraExecutor.shutdown();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        cameraExecutor.shutdown();
    }
}
