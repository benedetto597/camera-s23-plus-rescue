package com.s23plus.camera;

import android.Manifest;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.provider.Settings;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.Surface;
import android.view.TextureView;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

/**
 * Actividad principal de S23+ Camera Rescue.
 *
 * Proporciona:
 * 1. Visor Camera2 en vivo y captura directa a maxima resolucion para las dos camaras frontales
 *    funcionales ("1" 12 MP Gran Angular y "3" 8.6 MP Normal), guardando en Pictures/S23Camera.
 * 2. Lanzador directo de Camara Trasera 1x (device 56, 50MP/12MP S5KGN3) y Telefoto 3x (device 52, 10MP S5K3K1)
 *    en Modo Pro Nativo (ShootingMode 3) y Video Pro (ShootingMode 36), saltandose el sensor Ultra-Wide danado.
 * 3. Lanzador de Camara Trasera 1x (device 56) via Escaner QR (SCAN_QR_CODE) con HUD de Obturador Flotante.
 */
public final class MainActivity extends Activity implements FrontCameraController.StatusListener {

    private static final int REQUEST_CAMERA_PERMISSION = 1001;

    private TextureView frontPreviewView;
    private TextView statusBanner;
    private Button switchLensButton;
    private FrontCameraController frontCameraController;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        frontCameraController = new FrontCameraController(this, this);
        buildUserInterface();
    }

    private void buildUserInterface() {
        ScrollView rootScroll = new ScrollView(this);
        rootScroll.setFillViewport(true);
        rootScroll.setBackgroundColor(Color.parseColor("#0B0F17"));

        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        int pad = dpToPx(16);
        container.setPadding(pad, dpToPx(12), pad, pad);

        // Encabezado
        TextView titleView = new TextView(this);
        titleView.setText("S23+ Camera Rescue");
        titleView.setTextColor(Color.parseColor("#F8FAFC"));
        titleView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20);
        titleView.setGravity(Gravity.CENTER_HORIZONTAL);
        container.addView(titleView);

        TextView subtitleView = new TextView(this);
        subtitleView.setText("Trasera 1x (ID 56) + Tele 3x (ID 52) + Frontales (ID 1 y 3)");
        subtitleView.setTextColor(Color.parseColor("#38BDF8"));
        subtitleView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        subtitleView.setGravity(Gravity.CENTER_HORIZONTAL);
        container.addView(subtitleView);

        statusBanner = new TextView(this);
        statusBanner.setText("Inicializando visor frontal...");
        statusBanner.setTextColor(Color.parseColor("#A7F3D0"));
        statusBanner.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        statusBanner.setGravity(Gravity.CENTER);
        int bannerPad = dpToPx(8);
        statusBanner.setPadding(bannerPad, bannerPad, bannerPad, bannerPad);
        container.addView(statusBanner);

        // Contenedor del visor Camera2 Frontal
        FrameLayout previewFrame = new FrameLayout(this);
        LinearLayout.LayoutParams previewParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dpToPx(320)
        );
        previewParams.topMargin = dpToPx(6);
        previewParams.bottomMargin = dpToPx(10);
        previewFrame.setLayoutParams(previewParams);

        GradientDrawable frameBorder = new GradientDrawable();
        frameBorder.setColor(Color.parseColor("#111827"));
        frameBorder.setCornerRadius(dpToPx(16));
        frameBorder.setStroke(dpToPx(2), Color.parseColor("#1E293B"));
        previewFrame.setBackground(frameBorder);

        frontPreviewView = new TextureView(this);
        FrameLayout.LayoutParams textureLp = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        );
        int innerMargin = dpToPx(4);
        textureLp.setMargins(innerMargin, innerMargin, innerMargin, innerMargin);
        frontPreviewView.setLayoutParams(textureLp);
        previewFrame.addView(frontPreviewView);
        container.addView(previewFrame);

        // Fila de controles de Camara Frontal (ID 1 y 3)
        LinearLayout frontControlsRow = new LinearLayout(this);
        frontControlsRow.setOrientation(LinearLayout.HORIZONTAL);
        frontControlsRow.setGravity(Gravity.CENTER);

        Button takeSelfieBtn = createActionButton(
                "TOMAR FOTO FRONTAL",
                "#059669",
                "#FFFFFF"
        );
        LinearLayout.LayoutParams selfieLp = new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.2f);
        selfieLp.rightMargin = dpToPx(6);
        takeSelfieBtn.setLayoutParams(selfieLp);
        takeSelfieBtn.setOnClickListener(v -> frontCameraController.capturePhoto(getDisplayRotationCompat()));
        frontControlsRow.addView(takeSelfieBtn);

        switchLensButton = createActionButton(
                "Cambiar a ID 3 (8.6MP)",
                "#1E293B",
                "#38BDF8"
        );
        LinearLayout.LayoutParams switchLp = new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);
        switchLp.leftMargin = dpToPx(6);
        switchLensButton.setLayoutParams(switchLp);
        switchLensButton.setOnClickListener(v -> {
            frontCameraController.switchFrontCamera(frontPreviewView, getDisplayRotationCompat());
            String activeId = frontCameraController.getCurrentCameraId();
            switchLensButton.setText(CameraConfig.FRONT_CAMERA_WIDE_ID.equals(activeId)
                    ? "Cambiar a ID 3 (8.6MP)"
                    : "Cambiar a ID 1 (12MP)");
        });
        frontControlsRow.addView(switchLensButton);
        container.addView(frontControlsRow);

        // Seccion de Camara Trasera Principal (S5KGN3 - ID 56) y Telefoto (S5K3K1 - ID 52)
        TextView rearHeader = new TextView(this);
        rearHeader.setText("CAMARA TRASERA PRINCIPAL (BYPASS SENSOR 2 DANADO)");
        rearHeader.setTextColor(Color.parseColor("#94A3B8"));
        rearHeader.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        LinearLayout.LayoutParams headerLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        headerLp.topMargin = dpToPx(16);
        headerLp.bottomMargin = dpToPx(6);
        rearHeader.setLayoutParams(headerLp);
        container.addView(rearHeader);

        // Boton 1: Modo Pro ISP Directo (ID 56 1x Wide + ID 52 3x Tele)
        Button rearProBtn = createActionButton(
                "ABRIR TRASERA 1x / 3x — MODO PRO NATIVO (ID 56 / 52)",
                "#0284C7",
                "#FFFFFF"
        );
        LinearLayout.LayoutParams fullBtnLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        fullBtnLp.bottomMargin = dpToPx(8);
        rearProBtn.setLayoutParams(fullBtnLp);
        rearProBtn.setOnClickListener(v -> launchSamsungRearMode(
                CameraConfig.MODE_PRO_PHOTO,
                CameraConfig.ACTIVITY_NAME_PRO_PHOTO
        ));
        container.addView(rearProBtn);

        // Boton 2: Escaner QR + HUD Obturador Flotante (ID 56 garantizado)
        Button rearQrHudBtn = createActionButton(
                "ABRIR TRASERA 1x — MODO QR + BOTON FLOTANTE (ID 56)",
                "#4F46E5",
                "#FFFFFF"
        );
        rearQrHudBtn.setLayoutParams(fullBtnLp);
        rearQrHudBtn.setOnClickListener(v -> launchRearQrWithFloatingHud());
        container.addView(rearQrHudBtn);

        // Boton 3: Video Pro Trasero (ID 56 1x + ID 52 3x)
        Button rearProVideoBtn = createActionButton(
                "ABRIR VIDEO TRASERO 1x / 3x — VIDEO PRO (ID 56 / 52)",
                "#1E293B",
                "#E2E8F0"
        );
        rearProVideoBtn.setLayoutParams(fullBtnLp);
        rearProVideoBtn.setOnClickListener(v -> launchSamsungRearMode(
                CameraConfig.MODE_PRO_VIDEO,
                CameraConfig.ACTIVITY_NAME_PRO_VIDEO
        ));
        container.addView(rearProVideoBtn);

        TextView footerNote = new TextView(this);
        footerNote.setText(
                "Nota: En Modo Pro/Video Pro usa los lentes W (1x Principal ID 56) y T (3x Tele ID 52). "
                        + "Evita pulsar UW (0.6x) ya que corresponde al sensor fisico con falla de EEPROM."
        );
        footerNote.setTextColor(Color.parseColor("#64748B"));
        footerNote.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
        container.addView(footerNote);

        rootScroll.addView(container);
        setContentView(rootScroll);
    }

    private Button createActionButton(String text, String bgHex, String fgHex) {
        Button button = new Button(this);
        button.setAllCaps(false);
        button.setText(text);
        button.setTextColor(Color.parseColor(fgHex));
        button.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);

        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(Color.parseColor(bgHex));
        drawable.setCornerRadius(dpToPx(12));
        button.setBackground(drawable);

        int vPad = dpToPx(12);
        int hPad = dpToPx(12);
        button.setPadding(hPad, vPad, hPad, vPad);
        return button;
    }

    /**
     * Lanza Samsung Camera directamente en Modo Pro (ShootingMode 3 -> device 56) o Video Pro (ShootingMode 36),
     * liberando antes la camara frontal y limpiando cualquier tarea previa colgada.
     */
    private void launchSamsungRearMode(String previousMode, String activityName) {
        frontCameraController.closeCameraOnly();
        try {
            Intent intent = new Intent(Intent.ACTION_MAIN);
            intent.setComponent(new ComponentName(
                    CameraConfig.SAMSUNG_CAMERA_PACKAGE,
                    CameraConfig.SAMSUNG_CAMERA_ACTIVITY
            ));
            intent.putExtra(CameraConfig.EXTRA_PREVIOUS_MODE, previousMode);
            intent.putExtra(CameraConfig.EXTRA_ACTIVITY_NAME, activityName);
            intent.putExtra(CameraConfig.EXTRA_CAMERA_FACING, CameraConfig.FACING_BACK);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        } catch (RuntimeException e) {
            onError("No se pudo abrir Samsung Camera en modo " + previousMode + ": " + e.getMessage());
        }
    }

    /**
     * Lanza el Escaner de QR nativo (ShootingMode 40 -> device 56) junto con el HUD de Obturador Flotante
     * de RearCaptureService para tomar fotos sobre el visor 1x y guardarlas en Pictures/S23Camera.
     */
    private void launchRearQrWithFloatingHud() {
        RearCaptureService service = RearCaptureService.getInstance();
        if (service == null) {
            Toast.makeText(
                    this,
                    "Activa 'S23+ Obturador Trasero (ID 56)' en Accesibilidad para mostrar el boton flotante sobre el visor QR.",
                    Toast.LENGTH_LONG
            ).show();
            try {
                Intent settingsIntent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
                startActivity(settingsIntent);
            } catch (RuntimeException ignored) {
            }
            return;
        }

        frontCameraController.closeCameraOnly();
        service.showHud();

        try {
            Intent qrIntent = new Intent(CameraConfig.ACTION_SCAN_QR_CODE);
            qrIntent.setPackage(CameraConfig.SAMSUNG_CAMERA_PACKAGE);
            qrIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(qrIntent);
        } catch (RuntimeException e) {
            onError("No se pudo iniciar SCAN_QR_CODE: " + e.getMessage());
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        RearCaptureService service = RearCaptureService.getInstance();
        if (service != null) {
            service.hideHud();
        }
        ensureCameraPermissionAndStartFrontPreview();
    }

    @Override
    protected void onPause() {
        frontCameraController.stop();
        super.onPause();
    }

    private void ensureCameraPermissionAndStartFrontPreview() {
        if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.CAMERA}, REQUEST_CAMERA_PERMISSION);
        } else {
            frontCameraController.start(frontPreviewView, getDisplayRotationCompat());
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                frontCameraController.start(frontPreviewView, getDisplayRotationCompat());
            } else {
                onError("Permiso de camara denegado. Requerido para el visor frontal (ID 1 y 3).");
            }
        }
    }

    private int getDisplayRotationCompat() {
        if (getDisplay() != null) {
            return getDisplay().getRotation();
        }
        return Surface.ROTATION_0;
    }

    @Override
    public void onStatusChanged(String message) {
        if (statusBanner != null) {
            statusBanner.setTextColor(Color.parseColor("#A7F3D0"));
            statusBanner.setText(message);
        }
    }

    @Override
    public void onPhotoSaved(MediaStoreSaver.SaveResult result) {
        String msg = "Guardada en Galeria (Pictures/S23Camera): " + result.fileName;
        if (statusBanner != null) {
            statusBanner.setTextColor(Color.parseColor("#38BDF8"));
            statusBanner.setText(msg);
        }
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onError(String error) {
        if (statusBanner != null) {
            statusBanner.setTextColor(Color.parseColor("#FCA5A5"));
            statusBanner.setText(error);
        }
        Toast.makeText(this, error, Toast.LENGTH_LONG).show();
    }

    private int dpToPx(int dp) {
        return Math.round(TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, dp, getResources().getDisplayMetrics()));
    }
}
