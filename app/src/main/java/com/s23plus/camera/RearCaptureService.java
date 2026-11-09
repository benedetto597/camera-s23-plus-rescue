package com.s23plus.camera;

import android.accessibilityservice.AccessibilityService;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.ColorSpace;
import android.graphics.PixelFormat;
import android.graphics.drawable.GradientDrawable;
import android.hardware.HardwareBuffer;
import android.os.Handler;
import android.os.Looper;
import android.util.TypedValue;
import android.view.Display;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityEvent;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Servicio ligero de obturador flotante sobre el visor trasero (device 56) en modo Escaner QR.
 *
 * Cumple con el principio de minimo privilegio:
 * - No lee texto de la pantalla (canRetrieveWindowContent="false", accessibilityEventTypes="").
 * - Solo utiliza takeScreenshot(Display.DEFAULT_DISPLAY, ...) cuando el usuario pulsa "TOMAR FOTO".
 */
public final class RearCaptureService extends AccessibilityService {

    private static volatile RearCaptureService instance;

    private WindowManager windowManager;
    private LinearLayout hudContainer;
    private TextView hudStatusText;
    private Button cropModeButton;
    private Handler mainHandler;
    private ExecutorService ioExecutor;

    private boolean cleanCropMode = true;
    private boolean isHudAttached = false;

    public static RearCaptureService getInstance() {
        return instance;
    }

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        instance = this;
        mainHandler = new Handler(Looper.getMainLooper());
        ioExecutor = Executors.newSingleThreadExecutor();
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        buildHudView();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && "SHOW_HUD".equals(intent.getAction())) {
            showHud();
        } else if (intent != null && "CAPTURE_NOW".equals(intent.getAction())) {
            captureRearFrame();
        }
        return START_NOT_STICKY;
    }

    public void showHud() {
        if (mainHandler == null) {
            return;
        }
        mainHandler.post(() -> {
            if (windowManager == null) {
                return;
            }
            if (hudContainer == null) {
                buildHudView();
            }
            if (!isHudAttached) {
                WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                        WindowManager.LayoutParams.WRAP_CONTENT,
                        WindowManager.LayoutParams.WRAP_CONTENT,
                        WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                                | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                        PixelFormat.TRANSLUCENT
                );
                params.gravity = Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL;
                params.y = dpToPx(88);
                try {
                    windowManager.addView(hudContainer, params);
                    isHudAttached = true;
                } catch (RuntimeException e) {
                    Toast.makeText(this, "Error mostrando HUD: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            } else {
                hudContainer.setVisibility(View.VISIBLE);
            }
        });
    }

    public void hideHud() {
        if (mainHandler == null) {
            return;
        }
        mainHandler.post(() -> {
            if (isHudAttached && hudContainer != null && windowManager != null) {
                try {
                    windowManager.removeView(hudContainer);
                } catch (RuntimeException ignored) {
                }
                isHudAttached = false;
            }
        });
    }

    private void buildHudView() {
        hudContainer = new LinearLayout(this);
        hudContainer.setOrientation(LinearLayout.VERTICAL);
        hudContainer.setGravity(Gravity.CENTER_HORIZONTAL);
        int padH = dpToPx(14);
        int padV = dpToPx(10);
        hudContainer.setPadding(padH, padV, padH, padV);

        GradientDrawable bg = new GradientDrawable();
        bg.setColor(Color.parseColor("#D910141C"));
        bg.setCornerRadius(dpToPx(20));
        bg.setStroke(dpToPx(1), Color.parseColor("#38BDF8"));
        hudContainer.setBackground(bg);

        hudStatusText = new TextView(this);
        hudStatusText.setText("S23+ Camera Rescue · Lente 1x (ID 56)");
        hudStatusText.setTextColor(Color.parseColor("#E2E8F0"));
        hudStatusText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
        hudStatusText.setGravity(Gravity.CENTER);
        hudContainer.addView(hudStatusText);

        LinearLayout buttonRow = new LinearLayout(this);
        buttonRow.setOrientation(LinearLayout.HORIZONTAL);
        buttonRow.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        rowParams.topMargin = dpToPx(6);
        buttonRow.setLayoutParams(rowParams);

        Button shutterButton = createStyledButton("Capturar", "#0284C7", "#FFFFFF", true);
        shutterButton.setOnClickListener(v -> captureRearFrame());
        buttonRow.addView(shutterButton);

        cropModeButton = createStyledButton("Visor", "#1E293B", "#38BDF8", false);
        cropModeButton.setOnClickListener(v -> {
            cleanCropMode = !cleanCropMode;
            cropModeButton.setText(cleanCropMode ? "Visor" : "Completo");
        });
        buttonRow.addView(cropModeButton);

        Button switchFrontButton = createStyledButton("Frontal", "#1E293B", "#A7F3D0", false);
        switchFrontButton.setOnClickListener(v -> {
            hideHud();
            Intent frontIntent = new Intent(this, MainActivity.class);
            frontIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(frontIntent);
        });
        buttonRow.addView(switchFrontButton);

        Button closeButton = createStyledButton("✕", "#334155", "#FCA5A5", false);
        closeButton.setOnClickListener(v -> hideHud());
        buttonRow.addView(closeButton);

        hudContainer.addView(buttonRow);
    }

    private Button createStyledButton(String text, String bgColorHex, String textColorHex, boolean primary) {
        Button btn = new Button(this);
        btn.setAllCaps(false);
        btn.setText(text);
        btn.setTextColor(Color.parseColor(textColorHex));
        btn.setTextSize(TypedValue.COMPLEX_UNIT_SP, primary ? 14 : 12);

        GradientDrawable btnBg = new GradientDrawable();
        btnBg.setColor(Color.parseColor(bgColorHex));
        btnBg.setCornerRadius(dpToPx(14));
        btn.setBackground(btnBg);

        int hPad = dpToPx(primary ? 16 : 10);
        int vPad = dpToPx(8);
        btn.setPadding(hPad, vPad, hPad, vPad);

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        lp.leftMargin = dpToPx(4);
        lp.rightMargin = dpToPx(4);
        btn.setLayoutParams(lp);
        return btn;
    }

    public void captureRearFrame() {
        if (mainHandler == null || ioExecutor == null) {
            return;
        }
        mainHandler.post(() -> {
            if (hudContainer != null && isHudAttached) {
                hudContainer.setVisibility(View.INVISIBLE);
            }
            // Esperar 85ms para que SurfaceFlinger componga el cuadro sin el HUD flotante
            mainHandler.postDelayed(() -> {
                takeScreenshot(Display.DEFAULT_DISPLAY, ioExecutor, new TakeScreenshotCallback() {
                    @Override
                    public void onSuccess(ScreenshotResult screenshotResult) {
                        Bitmap cropped = null;
                        Bitmap soft = null;
                        try (HardwareBuffer hb = screenshotResult.getHardwareBuffer()) {
                            ColorSpace cs = screenshotResult.getColorSpace();
                            Bitmap hwBitmap = Bitmap.wrapHardwareBuffer(hb, cs);
                            if (hwBitmap == null) {
                                throw new IllegalStateException("Bitmap.wrapHardwareBuffer retorno null");
                            }
                            soft = hwBitmap.copy(Bitmap.Config.ARGB_8888, false);
                            hwBitmap.recycle();

                            int[] bounds = CameraConfig.computeCropBounds(
                                    soft.getWidth(), soft.getHeight(), cleanCropMode);
                            cropped = Bitmap.createBitmap(soft, bounds[0], bounds[1], bounds[2], bounds[3]);

                            MediaStoreSaver.SaveResult saved = MediaStoreSaver.saveBitmap(
                                    RearCaptureService.this, cropped, "S23_REAR_ID56");

                            mainHandler.post(() -> {
                                if (hudContainer != null && isHudAttached) {
                                    hudContainer.setVisibility(View.VISIBLE);
                                    hudStatusText.setText("Guardada: " + saved.fileName);
                                }
                                Toast.makeText(RearCaptureService.this,
                                        "Foto guardada en Galeria: " + saved.fileName,
                                        Toast.LENGTH_SHORT).show();
                            });
                        } catch (Exception e) {
                            mainHandler.post(() -> {
                                if (hudContainer != null && isHudAttached) {
                                    hudContainer.setVisibility(View.VISIBLE);
                                    hudStatusText.setText("Error: " + e.getMessage());
                                }
                                Toast.makeText(RearCaptureService.this,
                                        "Error al guardar foto: " + e.getMessage(),
                                        Toast.LENGTH_LONG).show();
                            });
                        } finally {
                            if (cropped != null && !cropped.isRecycled()) {
                                cropped.recycle();
                            }
                            if (soft != null && !soft.isRecycled()) {
                                soft.recycle();
                            }
                        }
                    }

                    @Override
                    public void onFailure(int errorCode) {
                        mainHandler.post(() -> {
                            if (hudContainer != null && isHudAttached) {
                                hudContainer.setVisibility(View.VISIBLE);
                                hudStatusText.setText("Reintentar disparo (codigo " + errorCode + ")");
                            }
                            Toast.makeText(RearCaptureService.this,
                                    "Espera 1 segundo entre capturas (cod " + errorCode + ")",
                                    Toast.LENGTH_SHORT).show();
                        });
                    }
                });
            }, 85);
        });
    }

    private int dpToPx(int dp) {
        return Math.round(TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, dp, getResources().getDisplayMetrics()));
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        // No se procesan eventos de accesibilidad por privacidad y rendimiento
    }

    @Override
    public void onInterrupt() {
        hideHud();
    }

    @Override
    public void onDestroy() {
        hideHud();
        if (ioExecutor != null) {
            ioExecutor.shutdownNow();
        }
        if (instance == this) {
            instance = null;
        }
        super.onDestroy();
    }
}
