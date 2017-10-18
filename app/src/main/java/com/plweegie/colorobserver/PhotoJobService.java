/*
Copyright (C) 2017 Jan K. Szymanski

This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.plweegie.colorobserver;

import android.app.job.JobParameters;
import android.app.job.JobService;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.Image;
import android.media.ImageReader;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Base64;
import android.util.Log;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;

import java.nio.ByteBuffer;

import boofcv.alg.color.ColorHsv;
import boofcv.android.ConvertBitmap;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.Planar;

import static android.content.ContentValues.TAG;

/**
 * Created by jan on 08/10/17.
 */

public class PhotoJobService extends JobService {

    private FirebaseDatabase mDatabase;
    private PicoCamera mCamera;
    private ImageReader.OnImageAvailableListener mOnImageAvailableListener;

    /**
     * A {@link Handler} for running Camera tasks in the background.
     */
    private Handler mCameraHandler;

    /**
     * An additional thread for running Camera tasks that shouldn't block the UI.
     */
    private HandlerThread mCameraThread;

    private JobParameters mParams;

    public PhotoJobService() {

    }

    @Override
    public boolean onStartJob(JobParameters jobParameters) {

        Log.d(TAG, "Starting job...");
        mParams = jobParameters;

        // We need permission to access the camera
        if (checkSelfPermission(android.Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            // A problem occurred auto-granting the permission
            Log.d(TAG, "No permission");
            return false;
        }

        mDatabase = FirebaseDatabase.getInstance();

        // Creates new handlers and associated threads for camera and networking operations.
        mCameraThread = new HandlerThread("CameraBackground");
        mCameraThread.start();
        mCameraHandler = new Handler(mCameraThread.getLooper());

        /**
         * Listener for new camera images.
         */
        mOnImageAvailableListener =
                new ImageReader.OnImageAvailableListener() {
                    @Override
                    public void onImageAvailable(ImageReader reader) {
                        Image image = reader.acquireLatestImage();
                        // get image bytes
                        ByteBuffer imageBuf = image.getPlanes()[0].getBuffer();
                        final byte[] imageBytes = new byte[imageBuf.remaining()];
                        imageBuf.get(imageBytes);
                        image.close();
                        Log.d(TAG, "Image available");

                        onPictureTaken(imageBytes);
                    }
                };

        mCamera = PicoCamera.getInstance();
        mCamera.initializeCamera(this, mCameraHandler, mOnImageAvailableListener);

        return true;
    }

    @Override
    public boolean onStopJob(JobParameters jobParameters) {
        Log.d(TAG, "Job finished");
        cleanUpAndReleaseCamera();
        return false;
    }

    /**
     * Handle image processing in BoofCV and Firebase.
     */
    private void onPictureTaken(final byte[] imageBytes) {
        if (imageBytes != null) {
            final DatabaseReference log = mDatabase.getReference("logs").push();

            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inSampleSize = 2;
            Bitmap imageBitmap = BitmapFactory.decodeByteArray(imageBytes, 0,
                    imageBytes.length, options);

            String imageStr = Base64.encodeToString(imageBytes, Base64.NO_WRAP | Base64.URL_SAFE);

            Planar<GrayF32> boofImage = new Planar<GrayF32>(GrayF32.class,
                    320, 240, 3);
            Planar<GrayF32> hsvImage = new Planar<GrayF32>(GrayF32.class,
                    320, 240, 3);

            ConvertBitmap.bitmapToPlanar(imageBitmap, boofImage, GrayF32.class, null);
            ColorHsv.rgbToHsv_F32(boofImage, hsvImage);
            float intensityValue = hsvImage.getBand(2).get(160, 120);
            float hueValue = hsvImage.getBand(0).get(160, 120);
            Log.d(TAG, "Hue: " + String.valueOf(hueValue));
            float saturationValue = hsvImage.getBand(1).get(160, 120);

            //if image is uniformly gray, hue will be NaN and Firebase will complain
            if (Float.isNaN(hueValue)) {
                hueValue = -1.0f;
            }

            // upload image to Firebase
            log.child("timestamp").setValue(ServerValue.TIMESTAMP);
            log.child("image").setValue(imageStr);
            log.child("intensity").setValue(intensityValue);
            log.child("hue").setValue(hueValue);
            log.child("saturation").setValue(saturationValue);
            Log.d(TAG, "Sent to Firebase");
        }
        jobFinished(mParams, false);
        cleanUpAndReleaseCamera();
    }

    private void cleanUpAndReleaseCamera() {
        mCamera.shutDown();
        mCameraThread.quitSafely();
    }
}
