package com.s23plus.camera;

/**
 * Configuración inmutable y funciones puras de cálculo para S23+ Camera Rescue.
 *
 * Basado en el diagnóstico de hardware del Samsung Galaxy S23+ (SM-S916U1):
 * - Sensor trasero Ultra-Wide 0.6x (IMX564, sensorId 2, ID público "2" / interno "58"): DAÑADO (EEPROM/I2C).
 * - Sensor trasero Principal 1x 50MP (S5KGN3, sensorId 0, físico "5" / interno independiente "56"): 100% FUNCIONAL.
 * - Sensor trasero Telefoto 3x 10MP (S5K3K1, sensorId 1, físico "6" / interno independiente "52"): 100% FUNCIONAL.
 * - Sensor frontal (S5K3LU, sensorId 3, IDs públicos "1" 12MP Gran Angular y "3" 8.6MP Normal): 100% FUNCIONALES.
 */
public final class CameraConfig {

    /** Identidad y versión oficial de la aplicación. */
    public static final String APP_TITLE = "S23+ Camera Rescue";
    public static final String APP_VERSION = "1.0.1";

    /** Nombres cortos para los botones principales de la interfaz moderna (v1.0.1). */
    public static final String BTN_FRONT_SHUTTER_SHORT = "Tomar Selfie";
    public static final String BTN_REAR_PRO_SHORT = "Cámara Trasera Pro";
    public static final String BTN_REAR_QR_SHORT = "Trasera Rápida (QR)";
    public static final String BTN_REAR_VIDEO_SHORT = "Video Trasero Pro";

    /** Descripciones técnicas desplegables (acordeón ▾/▴) para cada módulo de cámara. */
    public static final String TECH_DESC_FRONT =
            "Pipeline Camera2 directo sobre el sensor frontal S5K3LU (sin pasar por la app nativa).\n"
            + "• ID 1: 12 MP Gran Angular (4000×3000, f/2.2, 3.3 mm).\n"
            + "• ID 3: 8.6 MP Recorte Normal (3392×2544).\n"
            + "• Guardado transaccional JPEG 95% en Pictures/S23Camera.";

    public static final String TECH_DESC_REAR_PRO =
            "Inicia Samsung Camera en ShootingMode 3 (Pro) con FLAG_ACTIVITY_CLEAR_TASK.\n"
            + "• Abre directamente CameraId.BACK (ID 56, S5KGN3 50MP/12MP 1x) y CameraId.BACK_TELE (ID 52, S5K3K1 10MP 3x).\n"
            + "• Evita CameraId.BACK_SEAMLESS_ZOOM (ID 20), saltándose el chequeo de EEPROM del sensor Ultra-Wide dañado (ID 58 / sensorId 2).\n"
            + "• Conserva autoenfoque, flash, controles ISO/Speed en AUTO y disparo ISP en DCIM/Camera.";

    public static final String TECH_DESC_REAR_QR =
            "Invoca el Intent exportado com.sec.android.app.camera.action.SCAN_QR_CODE (ShootingMode 40).\n"
            + "• Fuerza la apertura exclusiva del pipeline independiente ID 56 (S5KGN3 1x, numPhysicalCameras: 1).\n"
            + "• Superpone el HUD flotante de RearCaptureService para capturar el visor óptico limpio y guardarlo en Pictures/S23Camera.";

    public static final String TECH_DESC_REAR_VIDEO =
            "Inicia Samsung Camera en ShootingMode 36 (Pro video) con FLAG_ACTIVITY_CLEAR_TASK.\n"
            + "• Habilita grabación de video con estabilización y audio sobre los sensores independientes ID 56 (1x W) e ID 52 (3x T).\n"
            + "• Evita por completo el grupo lógico multi-cámara ID 20 y el sensor Ultra-Wide averiado.";

    /** ID público Camera2 para la cámara frontal Gran Angular (12 MP, 4000x3000). */
    public static final String FRONT_CAMERA_WIDE_ID = "1";

    /** ID público Camera2 para la cámara frontal Normal (8.6 MP, 3392x2544). */
    public static final String FRONT_CAMERA_NORMAL_ID = "3";

    /** ID interno independiente del HAL de Samsung para el sensor trasero principal 1x (S5KGN3). */
    public static final String REAR_MAIN_STANDALONE_ID = "56";

    /** ID interno independiente del HAL de Samsung para el sensor trasero telefoto 3x (S5K3K1). */
    public static final String REAR_TELE_STANDALONE_ID = "52";

    /** Paquete y actividad principal de la cámara nativa de Samsung. */
    public static final String SAMSUNG_CAMERA_PACKAGE = "com.sec.android.app.camera";
    public static final String SAMSUNG_CAMERA_ACTIVITY = "com.sec.android.app.camera.Camera";

    /**
     * Acción pública exportada que abre el Escáner de QR nativo de Samsung (ShootingMode 40 -> CameraId.BACK = device 56).
     */
    public static final String ACTION_SCAN_QR_CODE = "com.sec.android.app.camera.action.SCAN_QR_CODE";

    /**
     * Extras de inicialización para lanzar Samsung Camera directamente en Modo Pro (ShootingMode 3 -> device 56 / 52)
     * o Video Pro (ShootingMode 36 -> device 56 / 52), evitando CameraId.BACK_SEAMLESS_ZOOM (device 20).
     */
    public static final String EXTRA_PREVIOUS_MODE = "previous_mode";
    public static final String EXTRA_ACTIVITY_NAME = "activity_name";
    public static final String EXTRA_CAMERA_FACING = "camerafacing";

    public static final String MODE_PRO_PHOTO = "Pro";
    public static final String ACTIVITY_NAME_PRO_PHOTO = "com.sec.android.app.camera.shootingmode.pro";

    public static final String MODE_PRO_VIDEO = "Pro video";
    public static final String ACTIVITY_NAME_PRO_VIDEO = "com.sec.android.app.camera.shootingmode.provideo";

    public static final String FACING_BACK = "back";

    /** Directorio relativo en MediaStore donde se almacenan las fotografías tomadas por la app. */
    public static final String MEDIA_RELATIVE_PATH = "Pictures/S23Camera";

    /**
     * Proporciones verificadas en el visor óptico de Samsung S23+ (1080x2340) durante el modo Escáner QR,
     * correspondientes al área limpia libre de textos e iconos superiores/inferiores.
     */
    public static final double QR_VIEWFINDER_TOP_RATIO = 280.0 / 2340.0;
    public static final double QR_VIEWFINDER_BOTTOM_RATIO = 1760.0 / 2340.0;
    public static final double QR_VIEWFINDER_SIDE_MARGIN_RATIO = 24.0 / 1080.0;

    private CameraConfig() {
        // Clase de utilidad no instanciable
    }

    /**
     * Alterna entre los dos IDs funcionales de cámara frontal ("1" Gran Angular y "3" Normal).
     *
     * @param currentId ID actual de cámara frontal
     * @return el otro ID funcional de cámara frontal
     */
    public static String toggleFrontCameraId(String currentId) {
        if (FRONT_CAMERA_WIDE_ID.equals(currentId)) {
            return FRONT_CAMERA_NORMAL_ID;
        }
        return FRONT_CAMERA_WIDE_ID;
    }

    /**
     * Calcula la región de recorte [left, top, width, height] para extraer el encuadre limpio
     * de una captura de pantalla del visor trasero (device 56).
     *
     * @param screenWidth     ancho del bitmap en píxeles (debe ser > 0)
     * @param screenHeight    alto del bitmap en píxeles (debe ser > 0)
     * @param cleanCropActive true para recortar únicamente el área limpia del visor; false para pantalla completa pero sin barras de estado/navegación
     * @return arreglo de 4 enteros: {left, top, width, height}
     */
    public static int[] computeCropBounds(int screenWidth, int screenHeight, boolean cleanCropActive) {
        if (screenWidth <= 0 || screenHeight <= 0) {
            throw new IllegalArgumentException("Las dimensiones de pantalla deben ser positivas.");
        }

        if (!cleanCropActive) {
            int topBar = Math.max(0, (int) Math.round(screenHeight * 0.045));
            int bottomBar = Math.max(0, (int) Math.round(screenHeight * 0.94));
            int height = Math.max(1, bottomBar - topBar);
            return new int[]{0, topBar, screenWidth, height};
        }

        int left = Math.max(0, (int) Math.round(screenWidth * QR_VIEWFINDER_SIDE_MARGIN_RATIO));
        int right = Math.min(screenWidth, screenWidth - left);
        int top = Math.max(0, (int) Math.round(screenHeight * QR_VIEWFINDER_TOP_RATIO));
        int bottom = Math.min(screenHeight, (int) Math.round(screenHeight * QR_VIEWFINDER_BOTTOM_RATIO));

        int width = Math.max(1, right - left);
        int height = Math.max(1, bottom - top);
        return new int[]{left, top, width, height};
    }
}
