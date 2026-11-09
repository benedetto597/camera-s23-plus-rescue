package com.s23plus.camera;

import android.Manifest;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.provider.Settings;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

/**
 * Actividad principal de S23+ Camera Rescue (v1.0.1).
 *
 * Interfaz moderna oscura con:
 * 1. Visor Camera2 en vivo para las camaras frontales ("1" 12 MP y "3" 8.6 MP) con controles cortos
 *    y ficha tecnica desplegable (▾/▴).
 * 2. Tarjetas de accion para Camara Trasera con nombres cortos y boton de flecha (▾/▴) que despliega
 *    la descripcion tecnica de cada modo de rescate (Modo Pro ISP, Escaner QR + HUD, y Video Pro).
 */
public final class MainActivity extends Activity implements FrontCameraController.StatusListener {

    private static final int REQUEST_CAMERA_PERMISSION = 1001;

    private TextureView frontPreviewView;
    private TextView statusBanner;
    private TextView lensOverlayBadge;
    private Button switchLensButton;
    private FrontCameraController frontCameraController;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setShowWhenLocked(true);
        setTurnScreenOn(true);
        frontCameraController = new FrontCameraController(this, this);
        buildModernUserInterface();
    }

    private void buildModernUserInterface() {
        ScrollView rootScroll = new ScrollView(this);
        rootScroll.setFillViewport(true);
        rootScroll.setBackgroundColor(Color.parseColor("#070B12"));

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        int padH = dpToPx(16);
        root.setPadding(padH, dpToPx(14), padH, dpToPx(20));

        // 1. Encabezado Moderno con Insignia de Version
        LinearLayout headerRow = new LinearLayout(this);
        headerRow.setOrientation(LinearLayout.HORIZONTAL);
        headerRow.setGravity(Gravity.CENTER_VERTICAL);

        LinearLayout titleColumn = new LinearLayout(this);
        titleColumn.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams titleColLp = new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);
        titleColumn.setLayoutParams(titleColLp);

        TextView titleView = new TextView(this);
        titleView.setText(CameraConfig.APP_TITLE);
        titleView.setTextColor(Color.parseColor("#F8FAFC"));
        titleView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 21);
        titleView.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
        titleColumn.addView(titleView);

        TextView subtitleView = new TextView(this);
        subtitleView.setText("Bypass de Hardware · Sensores 1x, 3x y Frontales");
        subtitleView.setTextColor(Color.parseColor("#64748B"));
        subtitleView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        titleColumn.addView(subtitleView);
        headerRow.addView(titleColumn);

        TextView versionBadge = new TextView(this);
        versionBadge.setText("v" + CameraConfig.APP_VERSION);
        versionBadge.setTextColor(Color.parseColor("#38BDF8"));
        versionBadge.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
        versionBadge.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
        versionBadge.setPadding(dpToPx(10), dpToPx(4), dpToPx(10), dpToPx(4));
        versionBadge.setBackground(createRoundedDrawable("#0F172A", "#1E3A5F", 12, 1));
        headerRow.addView(versionBadge);
        root.addView(headerRow);

        // Banner de estado en vivo
        statusBanner = new TextView(this);
        statusBanner.setText("● Inicializando visor frontal...");
        statusBanner.setTextColor(Color.parseColor("#34D399"));
        statusBanner.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        statusBanner.setPadding(dpToPx(12), dpToPx(7), dpToPx(12), dpToPx(7));
        statusBanner.setBackground(createRoundedDrawable("#0D1522", "#1E293B", 12, 1));
        LinearLayout.LayoutParams statusLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        statusLp.topMargin = dpToPx(10);
        statusLp.bottomMargin = dpToPx(10);
        statusBanner.setLayoutParams(statusLp);
        root.addView(statusBanner);

        // 2. Tarjeta del Visor Frontal Camera2
        LinearLayout frontCard = new LinearLayout(this);
        frontCard.setOrientation(LinearLayout.VERTICAL);
        int cardPad = dpToPx(10);
        frontCard.setPadding(cardPad, cardPad, cardPad, cardPad);
        frontCard.setBackground(createRoundedDrawable("#111827", "#1F2937", 20, 1));

        FrameLayout previewFrame = new FrameLayout(this);
        LinearLayout.LayoutParams previewParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dpToPx(290));
        previewFrame.setLayoutParams(previewParams);
        previewFrame.setBackground(createRoundedDrawable("#090D16", "#334155", 16, 1));

        frontPreviewView = new TextureView(this);
        FrameLayout.LayoutParams textureLp = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        int innerMargin = dpToPx(3);
        textureLp.setMargins(innerMargin, innerMargin, innerMargin, innerMargin);
        frontPreviewView.setLayoutParams(textureLp);
        previewFrame.addView(frontPreviewView);

        lensOverlayBadge = new TextView(this);
        lensOverlayBadge.setText("Frontal ID 1 · 12 MP");
        lensOverlayBadge.setTextColor(Color.parseColor("#E2E8F0"));
        lensOverlayBadge.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
        lensOverlayBadge.setPadding(dpToPx(10), dpToPx(4), dpToPx(10), dpToPx(4));
        lensOverlayBadge.setBackground(createRoundedDrawable("#BF0F172A", "#38BDF8", 10, 1));
        FrameLayout.LayoutParams badgeLp = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        badgeLp.gravity = Gravity.TOP | Gravity.START;
        badgeLp.setMargins(dpToPx(12), dpToPx(12), 0, 0);
        lensOverlayBadge.setLayoutParams(badgeLp);
        previewFrame.addView(lensOverlayBadge);

        frontCard.addView(previewFrame);

        // Controles cortos del visor frontal + boton desplegable (▾)
        LinearLayout frontControlsRow = new LinearLayout(this);
        frontControlsRow.setOrientation(LinearLayout.HORIZONTAL);
        frontControlsRow.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams controlsLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        controlsLp.topMargin = dpToPx(10);
        frontControlsRow.setLayoutParams(controlsLp);

        Button takeSelfieBtn = createShortButton(
                CameraConfig.BTN_FRONT_SHUTTER_SHORT,
                "#059669",
                "#FFFFFF",
                true
        );
        LinearLayout.LayoutParams selfieLp = new LinearLayout.LayoutParams(
                0, dpToPx(46), 1.15f);
        selfieLp.rightMargin = dpToPx(6);
        takeSelfieBtn.setLayoutParams(selfieLp);
        takeSelfieBtn.setOnClickListener(v -> frontCameraController.capturePhoto(getDisplayRotationCompat()));
        frontControlsRow.addView(takeSelfieBtn);

        switchLensButton = createShortButton(
                "Lente 8.6 MP",
                "#1E293B",
                "#7DD3FC",
                false
        );
        LinearLayout.LayoutParams switchLp = new LinearLayout.LayoutParams(
                0, dpToPx(46), 0.85f);
        switchLp.rightMargin = dpToPx(6);
        switchLensButton.setLayoutParams(switchLp);
        switchLensButton.setOnClickListener(v -> {
            frontCameraController.switchFrontCamera(frontPreviewView, getDisplayRotationCompat());
            String activeId = frontCameraController.getCurrentCameraId();
            boolean isWide = CameraConfig.FRONT_CAMERA_WIDE_ID.equals(activeId);
            switchLensButton.setText(isWide ? "Lente 8.6 MP" : "Lente 12 MP");
            lensOverlayBadge.setText(isWide ? "Frontal ID 1 · 12 MP" : "Frontal ID 3 · 8.6 MP");
        });
        frontControlsRow.addView(switchLensButton);

        TextView frontTechView = createTechDescriptionView(CameraConfig.TECH_DESC_FRONT);
        Button frontArrowBtn = createArrowToggleButton(frontTechView);
        frontControlsRow.addView(frontArrowBtn);

        frontCard.addView(frontControlsRow);
        frontCard.addView(frontTechView);
        root.addView(frontCard);

        // 3. Encabezado de Seccion Trasera
        TextView rearSectionHeader = new TextView(this);
        rearSectionHeader.setText("MODOS DE RESCATE — CÁMARA TRASERA");
        rearSectionHeader.setTextColor(Color.parseColor("#94A3B8"));
        rearSectionHeader.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
        rearSectionHeader.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
        LinearLayout.LayoutParams sectionLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        sectionLp.topMargin = dpToPx(16);
        sectionLp.bottomMargin = dpToPx(8);
        rearSectionHeader.setLayoutParams(sectionLp);
        root.addView(rearSectionHeader);

        // Tarjeta 1: Camara Trasera Pro (ISP 50MP/12MP - ID 56 y 52)
        root.addView(createExpandableRescueCard(
                CameraConfig.BTN_REAR_PRO_SHORT,
                "Recomendado · Lentes 1x (50 MP) y 3x Tele · ISP Nativo",
                "#0284C7",
                "#38BDF8",
                CameraConfig.TECH_DESC_REAR_PRO,
                () -> launchSamsungRearMode(CameraConfig.MODE_PRO_PHOTO, CameraConfig.ACTIVITY_NAME_PRO_PHOTO)
        ));

        // Tarjeta 2: Trasera Rapida (QR + Boton Obturador Flotante - ID 56)
        root.addView(createExpandableRescueCard(
                CameraConfig.BTN_REAR_QR_SHORT,
                "Obturador Flotante Rescue · Lente 1x Principal",
                "#4F46E5",
                "#818CF8",
                CameraConfig.TECH_DESC_REAR_QR,
                this::launchRearQrWithFloatingHud
        ));

        // Tarjeta 3: Video Trasero Pro (ID 56 y 52)
        root.addView(createExpandableRescueCard(
                CameraConfig.BTN_REAR_VIDEO_SHORT,
                "Grabación 1x y 3x con Estabilización y Audio",
                "#1E293B",
                "#94A3B8",
                CameraConfig.TECH_DESC_REAR_VIDEO,
                () -> launchSamsungRearMode(CameraConfig.MODE_PRO_VIDEO, CameraConfig.ACTIVITY_NAME_PRO_VIDEO)
        ));

        rootScroll.addView(root);
        setContentView(rootScroll);
    }

    /**
     * Construye una tarjeta moderna con boton de titulo corto, subtitulo conciso y un boton de flecha (▾/▴)
     * que despliega u oculta la explicacion tecnica del modo de rescate.
     */
    private LinearLayout createExpandableRescueCard(
            String shortButtonTitle,
            String subtitleText,
            String primaryButtonHex,
            String accentBorderHex,
            String technicalText,
            Runnable onLaunchAction
    ) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        int pad = dpToPx(10);
        card.setPadding(pad, pad, pad, pad);
        card.setBackground(createRoundedDrawable("#111827", "#1F2937", 18, 1));

        LinearLayout.LayoutParams cardLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        cardLp.bottomMargin = dpToPx(10);
        card.setLayoutParams(cardLp);

        LinearLayout topRow = new LinearLayout(this);
        topRow.setOrientation(LinearLayout.HORIZONTAL);
        topRow.setGravity(Gravity.CENTER_VERTICAL);

        Button launchBtn = createShortButton(shortButtonTitle, primaryButtonHex, "#FFFFFF", true);
        LinearLayout.LayoutParams launchLp = new LinearLayout.LayoutParams(
                0, dpToPx(46), 1.0f);
        launchLp.rightMargin = dpToPx(8);
        launchBtn.setLayoutParams(launchLp);
        launchBtn.setOnClickListener(v -> onLaunchAction.run());
        topRow.addView(launchBtn);

        TextView techDetailView = createTechDescriptionView(technicalText);
        Button arrowBtn = createArrowToggleButton(techDetailView);
        topRow.addView(arrowBtn);

        card.addView(topRow);

        TextView subtitleView = new TextView(this);
        subtitleView.setText(subtitleText);
        subtitleView.setTextColor(Color.parseColor(accentBorderHex));
        subtitleView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
        subtitleView.setPadding(dpToPx(4), dpToPx(6), dpToPx(4), dpToPx(2));
        card.addView(subtitleView);

        card.addView(techDetailView);
        return card;
    }

    private Button createShortButton(String text, String bgHex, String fgHex, boolean bold) {
        Button button = new Button(this);
        button.setAllCaps(false);
        button.setText(text);
        button.setTextColor(Color.parseColor(fgHex));
        button.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        button.setTypeface(Typeface.create("sans-serif-medium", bold ? Typeface.BOLD : Typeface.NORMAL));
        button.setBackground(createRoundedDrawable(bgHex, bgHex, 13, 0));
        button.setPadding(dpToPx(12), dpToPx(6), dpToPx(12), dpToPx(6));
        return button;
    }

    private Button createArrowToggleButton(final TextView targetTechView) {
        final Button arrowBtn = new Button(this);
        arrowBtn.setAllCaps(false);
        arrowBtn.setText("▾");
        arrowBtn.setTextColor(Color.parseColor("#38BDF8"));
        arrowBtn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        arrowBtn.setTypeface(Typeface.DEFAULT_BOLD);
        arrowBtn.setBackground(createRoundedDrawable("#0F172A", "#334155", 13, 1));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dpToPx(46), dpToPx(46));
        arrowBtn.setLayoutParams(lp);
        arrowBtn.setOnClickListener(v -> {
            boolean willShow = targetTechView.getVisibility() != View.VISIBLE;
            targetTechView.setVisibility(willShow ? View.VISIBLE : View.GONE);
            arrowBtn.setText(willShow ? "▴" : "▾");
            arrowBtn.setBackground(createRoundedDrawable(
                    willShow ? "#1E293B" : "#0F172A",
                    willShow ? "#38BDF8" : "#334155",
                    13,
                    1
            ));
        });
        return arrowBtn;
    }

    private TextView createTechDescriptionView(String description) {
        TextView tv = new TextView(this);
        tv.setText(description);
        tv.setTextColor(Color.parseColor("#CBD5E1"));
        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        tv.setLineSpacing(dpToPx(2), 1.0f);
        int p = dpToPx(10);
        tv.setPadding(p, p, p, p);
        tv.setBackground(createRoundedDrawable("#090D16", "#1E3A5F", 12, 1));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.topMargin = dpToPx(8);
        tv.setLayoutParams(lp);
        tv.setVisibility(View.GONE);
        return tv;
    }

    private GradientDrawable createRoundedDrawable(String fillHex, String strokeHex, int cornerDp, int strokeDp) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(Color.parseColor(fillHex));
        drawable.setCornerRadius(dpToPx(cornerDp));
        if (strokeDp > 0) {
            drawable.setStroke(dpToPx(strokeDp), Color.parseColor(strokeHex));
        }
        return drawable;
    }

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

    private void launchRearQrWithFloatingHud() {
        RearCaptureService service = RearCaptureService.getInstance();
        if (service == null) {
            Toast.makeText(
                    this,
                    "Activa 'S23+ Camera Rescue — Obturador Trasero' en Accesibilidad para usar el boton flotante.",
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
                onError("Permiso de camara requerido para el visor frontal (ID 1 y 3).");
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
            statusBanner.setTextColor(Color.parseColor("#34D399"));
            statusBanner.setText("● " + message);
        }
    }

    @Override
    public void onPhotoSaved(MediaStoreSaver.SaveResult result) {
        String msg = "✓ Guardada en Galería: " + result.fileName;
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
            statusBanner.setText("▲ " + error);
        }
        Toast.makeText(this, error, Toast.LENGTH_LONG).show();
    }

    private int dpToPx(int dp) {
        return Math.round(TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, dp, getResources().getDisplayMetrics()));
    }
}
