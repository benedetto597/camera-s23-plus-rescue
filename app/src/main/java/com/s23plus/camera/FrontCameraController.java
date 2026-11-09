package com.s23plus.camera;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.util.Size;
import android.view.Surface;
import android.view.TextureView;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Collections;

/**
 * Controlador Camera2 dedicado para los sensores frontales funcionales del Samsung Galaxy S23+:
 * - ID "1": S5K3LU 12 MP Gran Angular (4000x3000)
 * - ID "3": S5K3LU 8.6 MP Normal (3392x2544)
 */
public final class FrontCameraController {

    public interface StatusListener {
        void onStatusChanged(String message);
        void onPhotoSaved(MediaStoreSaver.SaveResult result);
        void onError(String error);
    }

    private final Context context;
    private final CameraManager cameraManager;
    private final Handler mainHandler;
    private final StatusListener listener;

    private HandlerThread backgroundThread;
    private Handler backgroundHandler;

    private CameraDevice cameraDevice;
    private CameraCaptureSession captureSession;
    private ImageReader imageReader;
    private Surface previewSurface;

    private String currentCameraId = CameraConfig.FRONT_CAMERA_WIDE_ID;
    private int sensorOrientation = 270;
    private boolean isOpening = false;

    public FrontCameraController(Context context, StatusListener listener) {
        this.context = context.getApplicationContext();
        this.cameraManager = (CameraManager) this.context.getSystemService(Context.CAMERA_SERVICE);
        this.mainHandler = new Handler(Looper.getMainLooper());
        this.listener = listener;
    }

    public String getCurrentCameraId() {
        return currentCameraId;
    }

    public void setCurrentCameraId(String cameraId) {
        if (CameraConfig.FRONT_CAMERA_WIDE_ID.equals(cameraId) || CameraConfig.FRONT_CAMERA_NORMAL_ID.equals(cameraId)) {
            this.currentCameraId = cameraId;
        }
    }

    public synchronized void start(TextureView textureView, int displayRotation) {
        startBackgroundThread();
        if (textureView.isAvailable()) {
            openCameraInternal(textureView.getSurfaceTexture(), displayRotation);
        } else {
            textureView.setSurfaceTextureListener(new TextureView.SurfaceTextureListener() {
                @Override
                public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
                    openCameraInternal(surface, displayRotation);
                }

                @Override
                public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
                }

                @Override
                public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
                    closeCameraOnly();
                    return true;
                }

                @Override
                public void onSurfaceTextureUpdated(SurfaceTexture surface) {
                }
            });
        }
    }

    public synchronized void switchFrontCamera(TextureView textureView, int displayRotation) {
        currentCameraId = CameraConfig.toggleFrontCameraId(currentCameraId);
        closeCameraOnly();
        if (textureView.isAvailable()) {
            openCameraInternal(textureView.getSurfaceTexture(), displayRotation);
        }
    }

    @SuppressLint("MissingPermission")
    private synchronized void openCameraInternal(SurfaceTexture surfaceTexture, int displayRotation) {
        if (isOpening || cameraDevice != null || surfaceTexture == null) {
            return;
        }
        isOpening = true;
        try {
            CameraCharacteristics characteristics = cameraManager.getCameraCharacteristics(currentCameraId);
            Integer orientation = characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION);
            if (orientation != null) {
                sensorOrientation = orientation;
            }

            StreamConfigurationMap map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
            Size previewSize = new Size(1440, 1080);
            Size jpegSize = CameraConfig.FRONT_CAMERA_WIDE_ID.equals(currentCameraId)
                    ? new Size(4000, 3000)
                    : new Size(3392, 2544);

            if (map != null) {
                Size[] jpegSizes = map.getOutputSizes(ImageFormat.JPEG);
                if (jpegSizes != null && jpegSizes.length > 0) {
                    jpegSize = chooseLargestSize(jpegSizes);
                }
            }

            surfaceTexture.setDefaultBufferSize(previewSize.getWidth(), previewSize.getHeight());
            previewSurface = new Surface(surfaceTexture);

            imageReader = ImageReader.newInstance(jpegSize.getWidth(), jpegSize.getHeight(), ImageFormat.JPEG, 2);
            final Size finalJpegSize = jpegSize;

            cameraManager.openCamera(currentCameraId, new CameraDevice.StateCallback() {
                @Override
                public void onOpened(CameraDevice camera) {
                    synchronized (FrontCameraController.this) {
                        isOpening = false;
                        cameraDevice = camera;
                        createPreviewSession(finalJpegSize);
                    }
                }

                @Override
                public void onDisconnected(CameraDevice camera) {
                    synchronized (FrontCameraController.this) {
                        isOpening = false;
                        camera.close();
                        cameraDevice = null;
                    }
                }

                @Override
                public void onError(CameraDevice camera, int error) {
                    synchronized (FrontCameraController.this) {
                        isOpening = false;
                        camera.close();
                        cameraDevice = null;
                    }
                    notifyError("Error al abrir camara frontal ID " + currentCameraId + " (codigo " + error + ")");
                }
            }, backgroundHandler);

        } catch (CameraAccessException | RuntimeException e) {
            isOpening = false;
            notifyError("No se pudo inicializar camara frontal ID " + currentCameraId + ": " + e.getMessage());
        }
    }

    private void createPreviewSession(Size jpegSize) {
        if (cameraDevice == null || previewSurface == null || imageReader == null) {
            return;
        }
        try {
            final CaptureRequest.Builder previewBuilder =
                    cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            previewBuilder.addTarget(previewSurface);
            previewBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
            previewBuilder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON);

            cameraDevice.createCaptureSession(
                    Arrays.asList(previewSurface, imageReader.getSurface()),
                    new CameraCaptureSession.StateCallback() {
                        @Override
                        public void onConfigured(CameraCaptureSession session) {
                            synchronized (FrontCameraController.this) {
                                if (cameraDevice == null) {
                                    return;
                                }
                                captureSession = session;
                                try {
                                    session.setRepeatingRequest(previewBuilder.build(), null, backgroundHandler);
                                    String label = CameraConfig.FRONT_CAMERA_WIDE_ID.equals(currentCameraId)
                                            ? "Frontal ID 1 (12 MP Gran Angular)"
                                            : "Frontal ID 3 (8.6 MP Normal)";
                                    notifyStatus("Activa: " + label + " [" + jpegSize.getWidth() + "x" + jpegSize.getHeight() + "]");
                                } catch (CameraAccessException e) {
                                    notifyError("Error al iniciar vista previa: " + e.getMessage());
                                }
                            }
                        }

                        @Override
                        public void onConfigureFailed(CameraCaptureSession session) {
                            notifyError("Fallo al configurar sesion Camera2 en ID " + currentCameraId);
                        }
                    },
                    backgroundHandler
            );
        } catch (CameraAccessException e) {
            notifyError("Error creando sesion de vista previa: " + e.getMessage());
        }
    }

    public synchronized void capturePhoto(int displayRotation) {
        if (cameraDevice == null || captureSession == null || imageReader == null) {
            notifyError("El visor frontal aun no esta listo.");
            return;
        }

        try {
            imageReader.setOnImageAvailableListener(reader -> {
                try (Image image = reader.acquireLatestImage()) {
                    if (image == null) {
                        return;
                    }
                    ByteBuffer buffer = image.getPlanes()[0].getBuffer();
                    byte[] bytes = new byte[buffer.remaining()];
                    buffer.get(bytes);

                    String prefix = "S23_FRONT_ID" + currentCameraId;
                    MediaStoreSaver.SaveResult result = MediaStoreSaver.saveJpegBytes(context, bytes, prefix);
                    mainHandler.post(() -> listener.onPhotoSaved(result));
                } catch (Exception e) {
                    notifyError("Error guardando selfie: " + e.getMessage());
                }
            }, backgroundHandler);

            CaptureRequest.Builder captureBuilder =
                    cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
            captureBuilder.addTarget(imageReader.getSurface());
            captureBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
            captureBuilder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON);
            captureBuilder.set(CaptureRequest.JPEG_QUALITY, (byte) 95);
            captureBuilder.set(CaptureRequest.JPEG_ORIENTATION, computeJpegOrientation(displayRotation));

            captureSession.capture(captureBuilder.build(), new CameraCaptureSession.CaptureCallback() {
                @Override
                public void onCaptureCompleted(CameraCaptureSession session, CaptureRequest request, TotalCaptureResult result) {
                    notifyStatus("Selfie capturada en ID " + currentCameraId + ", guardando...");
                }
            }, backgroundHandler);

        } catch (CameraAccessException e) {
            notifyError("Error al disparar foto frontal: " + e.getMessage());
        }
    }

    private int computeJpegOrientation(int displayRotation) {
        int degrees = 0;
        if (displayRotation == Surface.ROTATION_90) {
            degrees = 90;
        } else if (displayRotation == Surface.ROTATION_180) {
            degrees = 180;
        } else if (displayRotation == Surface.ROTATION_270) {
            degrees = 270;
        }
        return (sensorOrientation + degrees) % 360;
    }

    private static Size chooseLargestSize(Size[] sizes) {
        return Collections.max(Arrays.asList(sizes),
                (a, b) -> Long.compare((long) a.getWidth() * a.getHeight(), (long) b.getWidth() * b.getHeight()));
    }

    public synchronized void closeCameraOnly() {
        isOpening = false;
        if (captureSession != null) {
            try {
                captureSession.close();
            } catch (Exception ignored) {
            }
            captureSession = null;
        }
        if (cameraDevice != null) {
            try {
                cameraDevice.close();
            } catch (Exception ignored) {
            }
            cameraDevice = null;
        }
        if (imageReader != null) {
            try {
                imageReader.close();
            } catch (Exception ignored) {
            }
            imageReader = null;
        }
        if (previewSurface != null) {
            try {
                previewSurface.release();
            } catch (Exception ignored) {
            }
            previewSurface = null;
        }
    }

    public synchronized void stop() {
        closeCameraOnly();
        stopBackgroundThread();
    }

    private void startBackgroundThread() {
        if (backgroundThread == null) {
            backgroundThread = new HandlerThread("S23FrontCameraThread");
            backgroundThread.start();
            backgroundHandler = new Handler(backgroundThread.getLooper());
        }
    }

    private void stopBackgroundThread() {
        if (backgroundThread != null) {
            backgroundThread.quitSafely();
            try {
                backgroundThread.join(500);
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            }
            backgroundThread = null;
            backgroundHandler = null;
        }
    }

    private void notifyStatus(String message) {
        mainHandler.post(() -> listener.onStatusChanged(message));
    }

    private void notifyError(String error) {
        mainHandler.post(() -> listener.onError(error));
    }
}
