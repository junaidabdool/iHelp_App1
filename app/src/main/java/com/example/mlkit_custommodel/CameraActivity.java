package com.example.mlkit_custommodel;

import android.annotation.SuppressLint;
import android.graphics.Rect;
import android.media.Image;
import android.os.Bundle;
import android.util.Log;
import android.util.Size;
import android.view.OrientationEventListener;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LifecycleOwner;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.mlkit.common.model.LocalModel;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.objects.DetectedObject;
import com.google.mlkit.vision.objects.ObjectDetection;
import com.google.mlkit.vision.objects.ObjectDetector;
import com.google.mlkit.vision.objects.custom.CustomObjectDetectorOptions;



import java.util.concurrent.ExecutionException;

public class CameraActivity extends AppCompatActivity {
    GraphicOverlay graphicOverlay;
    private PreviewView previewView;
    private ListenableFuture<ProcessCameraProvider> cameraProviderFuture;
    private TextView textView;
    private static final String TAG ="MainActivity: ";


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);
        previewView = findViewById(R.id.previewView);
        cameraProviderFuture = ProcessCameraProvider.getInstance(this);
        textView = findViewById(R.id.orientation);
        cameraProviderFuture.addListener(new Runnable() {
            @Override
            public void run() {
                try {
                    ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                    bindImageAnalysis(cameraProvider);


                } catch (ExecutionException | InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }, ContextCompat.getMainExecutor(this));

    }

    private void bindImageAnalysis(@NonNull ProcessCameraProvider cameraProvider) {
        LocalModel localModel = new LocalModel.Builder().setAssetFilePath("ML_kit_model.tflite").build();
        // or .setAbsoluteFilePath(absolute file path to model file)
        // or .setUri(URI to model file)58

        // Live detection and tracking
        CustomObjectDetectorOptions customObjectDetectorOptions =
                new CustomObjectDetectorOptions.Builder(localModel)
                        .setDetectorMode(CustomObjectDetectorOptions.STREAM_MODE)
                        .enableClassification()
                        .setClassificationConfidenceThreshold(0.5f)
                        .setMaxPerObjectLabelCount(3)
                        .build();
        Log.e("TTS", "Error ");

        ObjectDetector objectDetector = ObjectDetection.getClient(customObjectDetectorOptions);
        ImageAnalysis imageAnalysis =
                new ImageAnalysis.Builder().setTargetResolution(new Size(1280, 720))
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST).build();
        imageAnalysis.setAnalyzer(ContextCompat.getMainExecutor(this), new ImageAnalysis.Analyzer() {
            @Override
            public void analyze(@NonNull ImageProxy imageProxy) {
                @SuppressLint("UnsafeOptInUsageError") Image mediaImage = imageProxy.getImage();

                if (mediaImage != null){
                    InputImage image = InputImage.fromMediaImage(mediaImage, imageProxy.getImageInfo().getRotationDegrees());
                    objectDetector
                            .process(image)
                            .addOnFailureListener(new OnFailureListener() {
                                @Override
                                public void onFailure(@NonNull Exception e) {
                                    String error = "Failed to process. Error: " + e.getMessage();

                                    Log.i(TAG, error);
                                    imageProxy.close();
                                }
                            })
                            .addOnSuccessListener(results -> {
                                for (DetectedObject detectedObject : results) {
                                    Rect boundingBox = detectedObject.getBoundingBox();
                                    Integer trackingId = detectedObject.getTrackingId();
                                    for (DetectedObject.Label label : detectedObject.getLabels()) {
                                        String text = label.getText();
                                        int index = label.getIndex();
                                        float confidence = label.getConfidence();
                                    }
                                }
//                                for (DetectedObject object : results) {
//                                    graphicOverlay.add(new ObjectGraphic(graphicOverlay, object));
//                                }


                            });
                }

            }
        });


        OrientationEventListener orientationEventListener = new OrientationEventListener(this) {
            @Override
            public void onOrientationChanged(int orientation) {
                textView.setText(Integer.toString(orientation));
            }
        };
        orientationEventListener.disable();
        Preview preview = new Preview.Builder().build();
        CameraSelector cameraSelector = new CameraSelector.Builder().requireLensFacing(CameraSelector.LENS_FACING_BACK).build();

        preview.setSurfaceProvider(previewView.createSurfaceProvider());
        cameraProvider.bindToLifecycle((LifecycleOwner) this, cameraSelector, preview, imageAnalysis);
    }


}






