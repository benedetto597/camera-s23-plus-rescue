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
