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
import android.net.Uri;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.annotation.NonNull;
import android.util.Base64;
import android.util.Log;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.plweegie.colorobserver.models.ColorMeasurement;
import com.plweegie.colorobserver.rest.DjangoColorsAPI;
import com.plweegie.colorobserver.rest.RestClient;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.Random;

import boofcv.alg.color.ColorHsv;
import boofcv.alg.feature.detect.edge.CannyEdge;
import boofcv.alg.feature.detect.edge.EdgeContour;
import boofcv.android.ConvertBitmap;
import boofcv.android.VisualizeImageData;
import boofcv.factory.feature.detect.edge.FactoryEdgeDetectors;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.GrayS16;
import boofcv.struct.image.GrayU8;
import boofcv.struct.image.Planar;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import static android.content.ContentValues.TAG;

public class PhotoJobService extends JobService {

    private FirebaseDatabase mDatabase;
    private DatabaseReference mDbReference;
    private FirebaseStorage mStorage;
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
        mStorage = FirebaseStorage.getInstance();
        mDbReference = mDatabase.getReference("logs").push();

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

            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inSampleSize = 2;
            Bitmap imageBitmap = BitmapFactory.decodeByteArray(imageBytes, 0,
                    imageBytes.length, options);

            String imageStr = Base64.encodeToString(imageBytes,
                    Base64.NO_WRAP | Base64.URL_SAFE);

            sendToDb(imageBitmap, imageStr);
            sendToStorage(imageBitmap);
        }
        jobFinished(mParams, false);
        cleanUpAndReleaseCamera();
    }

    private void sendToDb(Bitmap bitmap, String imgAsString) {

        RestClient client = new RestClient();
        DjangoColorsAPI service = client.getApiService();

        Planar<GrayF32> boofImage = new Planar<GrayF32>(GrayF32.class,
                320, 240, 3);
        Planar<GrayF32> hsvImage = new Planar<GrayF32>(GrayF32.class,
                320, 240, 3);

        ConvertBitmap.bitmapToPlanar(bitmap, boofImage, GrayF32.class, null);
        ColorHsv.rgbToHsv_F32(boofImage, hsvImage);
        float intensityValue = hsvImage.getBand(2).get(160, 120);
        float hueValue = hsvImage.getBand(0).get(160, 120);
        Log.d(TAG, "Hue: " + String.valueOf(hueValue));
        float saturationValue = hsvImage.getBand(1).get(160, 120);

        //if image is uniformly gray, hue will be NaN and Firebase will complain
        if (Float.isNaN(hueValue)) {
            hueValue = -1.0f;
        }

        //create colormeasurement object
        ColorMeasurement color = new ColorMeasurement();
        color.setHue(hueValue);
        color.setIntensity(intensityValue);
        color.setSaturation(saturationValue);
        color.setBitmapAsString(imgAsString);

        // send image data to Django server
        Call<ResponseBody> call = service.createColorEntry(color);
        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response.isSuccessful()) {
                    Log.d(TAG, "New data inserted " + response.toString());
                } else {
                    Log.d(TAG, "Bad request " + response.toString());
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                Log.e(TAG, "Retrofit error " + t.getMessage());
            }
        });

//        // upload image to Firebase
//        mDbReference.child("timestamp").setValue(ServerValue.TIMESTAMP);
//        mDbReference.child("image").setValue(imgAsString);
//        mDbReference.child("intensity").setValue(intensityValue);
//        mDbReference.child("hue").setValue(hueValue);
//        mDbReference.child("saturation").setValue(saturationValue);
//        Log.d(TAG, "Sent to Firebase");
    }

    private void sendToStorage(Bitmap bitmap) {
        final StorageReference storageRef = mStorage.getReference(mDbReference.getKey());

        Bitmap bitmapToSend = detectEdges(bitmap);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bitmapToSend.compress(Bitmap.CompressFormat.JPEG, 100, baos);
        byte[] dataToStore = baos.toByteArray();
        ByteArrayInputStream bais = new ByteArrayInputStream(dataToStore);

        //upload image to Firebase Storage
        UploadTask upload = storageRef.putStream(bais);
        upload.addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Log.e(TAG, "Upload failed for key " + mDbReference.getKey());
            }
        }).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                Uri downloadUrl = taskSnapshot.getDownloadUrl();

                if (downloadUrl != null) {
                    Log.d(TAG, "Sent to storage");
                    Log.d(TAG, downloadUrl.toString());
                }
            }
        });
    }

    private Bitmap detectEdges(Bitmap bitmap) {

        Random rand = new Random(234);

        GrayU8 gray = new GrayU8(320, 240);
        ConvertBitmap.bitmapToGray(bitmap, gray, GrayU8.class, null);

        CannyEdge<GrayU8, GrayS16> canny = FactoryEdgeDetectors.canny(2, true,
                true, GrayU8.class, GrayS16.class);
        canny.process(gray, 0.03f, 0.09f, null);
        List<EdgeContour> cannyContours = canny.getContours();

        int[] rgb = new int[cannyContours.size()];

        for(int i = 0; i < cannyContours.size(); i++) {
            rgb[i] = rand.nextInt();
        }

        Bitmap outputBitmap = Bitmap.createBitmap(320, 240, Bitmap.Config.ARGB_8888);

        VisualizeImageData.drawEdgeContours(cannyContours, rgb, outputBitmap, null);
        return outputBitmap;
    }

    private void cleanUpAndReleaseCamera() {
        mCamera.shutDown();
        mCameraThread.quitSafely();
    }
}
