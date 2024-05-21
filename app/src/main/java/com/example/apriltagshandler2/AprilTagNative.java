package com.example.apriltagshandler2;

import android.graphics.Bitmap;
import android.util.Log;
import java.util.ArrayList;

/**
 * Interface to native C AprilTag library.
 */

public class AprilTagNative {
    static {
        System.loadLibrary("apriltag");
        native_init();
    }

    public static native void native_init();

    public static native void yuv_to_rgb(byte[] src, int width, int height, Bitmap dst);

    public static native void apriltag_init(String tagFamily, int errorBits, double decimateFactor,
                                            double blurSigma, int nthreads);

    public static native ArrayList<AprilTagDetection> apriltag_detect_yuv(byte[] src, int width, int height);
}

