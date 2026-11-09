# Política de Seguridad y Privacidad (`SECURITY.md`)

## 1. Principios de Diseño Seguro

`S23+ Camera Rescue` (`com.s23plus.camera`) fue diseñada bajo el principio de **Mínimo Privilegio** y **Privacidad por Diseño**:

1. **100% Offline (Sin Permiso de Red):**
   - El archivo `AndroidManifest.xml` **NO solicita `android.permission.INTERNET`** ni ningún permiso de red o ubicación.
   - Es técnicamente imposible para la aplicación transmitir imágenes, metadatos o telemetría fuera del dispositivo.
2. **Cero Dependencias de Terceros:**
   - El código fuente utiliza exclusivamente las APIs estándar del SDK de Android (`android.hardware.camera2`, `android.provider.MediaStore`, `android.accessibilityservice.AccessibilityService`). No incluye SDKs analíticos, publicitarios ni librerías binarias externas.
3. **Servicio de Captura Flotante Restringido (`RearCaptureService`):**
   - Configurado en `res/xml/accessibility_service_config.xml` con:
     - `android:accessibilityEventTypes=""` (no escucha eventos de interfaz ni pulsaciones).
     - `android:canRetrieveWindowContent="false"` (no puede inspeccionar el árbol de vistas ni leer textos de ninguna aplicación).
     - `android:canTakeScreenshot="true"` utilizado exclusivamente de forma manual cuando el usuario pulsa el botón **TOMAR FOTO** en el HUD flotante.
4. **Almacenamiento Seguro (`Scoped Storage`):**
   - Utiliza `MediaStore.Images.Media` con escrituras atómicas (`IS_PENDING = 1 -> 0`) dentro de `Pictures/S23Camera`, sin requerir permisos amplios de lectura de archivos (`READ_EXTERNAL_STORAGE` / `MANAGE_EXTERNAL_STORAGE` no solicitados).
5. **Gestión Determinista de Recursos:**
   - La cámara frontal (`ID 1` / `ID 3`) se libera de forma inmediata en `onPause()` y antes de invocar el visor trasero de Samsung Camera, evitando bloqueos del servicio `cameraserver`.
