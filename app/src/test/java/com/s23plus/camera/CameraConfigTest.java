package com.s23plus.camera;

/**
 * Suite de pruebas unitarias para verificar la configuración de hardware y cálculos geométricos
 * de S23+ Camera Rescue sin requerir emulador.
 */
public final class CameraConfigTest {

    public static void main(String[] args) {
        testCameraIdsAndConstants();
        testToggleFrontCameraId();
        testCleanCropBoundsOnS23PlusResolution();
        testFullFrameCropBounds();
        testInvalidDimensionsThrowException();
        System.out.println("PASS: Todas las pruebas unitarias de CameraConfigTest finalizaron con exito (5/5).");
    }

    private static void testCameraIdsAndConstants() {
        assertEquals("1", CameraConfig.FRONT_CAMERA_WIDE_ID, "ID frontal Gran Angular debe ser 1");
        assertEquals("3", CameraConfig.FRONT_CAMERA_NORMAL_ID, "ID frontal Normal debe ser 3");
        assertEquals("56", CameraConfig.REAR_MAIN_STANDALONE_ID, "ID trasero principal independiente debe ser 56");
        assertEquals("52", CameraConfig.REAR_TELE_STANDALONE_ID, "ID trasero telefoto independiente debe ser 52");
        assertEquals("com.sec.android.app.camera.action.SCAN_QR_CODE", CameraConfig.ACTION_SCAN_QR_CODE, "Accion QR incorrecta");
        assertEquals("Pro", CameraConfig.MODE_PRO_PHOTO, "Modo Pro incorrecto");
    }

    private static void testToggleFrontCameraId() {
        assertEquals("3", CameraConfig.toggleFrontCameraId("1"), "Toggle de 1 debe retornar 3");
        assertEquals("1", CameraConfig.toggleFrontCameraId("3"), "Toggle de 3 debe retornar 1");
        assertEquals("1", CameraConfig.toggleFrontCameraId(null), "Toggle de null debe retornar 1 por defecto");
    }

    private static void testCleanCropBoundsOnS23PlusResolution() {
        int[] bounds = CameraConfig.computeCropBounds(1080, 2340, true);
        assertEquals(24, bounds[0], "Left margin en 1080p debe ser 24px");
        assertEquals(280, bounds[1], "Top bound en 2340p debe ser 280px");
        assertEquals(1032, bounds[2], "Width limpio en 1080p debe ser 1032px");
        assertEquals(1480, bounds[3], "Height limpio en 2340p debe ser 1480px");
    }

    private static void testFullFrameCropBounds() {
        int[] bounds = CameraConfig.computeCropBounds(1080, 2340, false);
        assertEquals(0, bounds[0], "Left en modo completo debe ser 0");
        assertTrue(bounds[1] > 0 && bounds[1] < 200, "Top en modo completo debe excluir barra de estado");
        assertEquals(1080, bounds[2], "Width en modo completo debe ser 1080");
        assertTrue(bounds[3] > 2000 && (bounds[1] + bounds[3]) <= 2340, "Height en modo completo debe estar dentro de la pantalla");
    }

    private static void testInvalidDimensionsThrowException() {
        boolean threw = false;
        try {
            CameraConfig.computeCropBounds(0, 2340, true);
        } catch (IllegalArgumentException expected) {
            threw = true;
        }
        assertTrue(threw, "Dimension 0 debe lanzar IllegalArgumentException");
    }

    private static void assertEquals(Object expected, Object actual, String message) {
        if (expected == null ? actual != null : !expected.equals(actual)) {
            throw new AssertionError(message + " | Esperado: " + expected + ", Actual: " + actual);
        }
    }

    private static void assertTrue(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
