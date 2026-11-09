# Especificación de Diseño: S23+ Camera Rescue (`com.s23plus.camera`)

**Fecha:** 8 de noviembre de 2026  
**Dispositivo Objetivo:** Samsung Galaxy S23+ (`SM-S916U1` — Android 16 / One UI 8)  
**Estado:** Aprobado para Implementación (v1.0 MVP + Arquitectura Extensible v1.1+)

---

## 1. Contexto y Problema Técnico

El dispositivo Samsung Galaxy S23+ (`SM-S916U1`) presenta una avería física de bus I2C / EEPROM en el módulo de cámara trasera Ultra-Wide 0.6x (`IMX564`, `sensorId 2`, dispositivo público `ID 2` / interno `ID 58`).

Aunque el sensor trasero Principal 1x de 50 MP (`S5KGN3`, `sensorId 0`, físico `ID 5` / interno independiente `ID 56`) y los sensores frontales (`S5K3LU`, públicos `ID 1` y `ID 3`) funcionan al 100%, todas las aplicaciones de cámara estándar (incluyendo el modo Foto de Samsung Camera, OpenCamera, GCam y apps de redes sociales) fallan al intentar abrir la cámara trasera porque abren el dispositivo lógico `ID 0` (`[2, 5, 6]`, `chiCameraId: 16`, `numPhysicalCameras: 3`), cuyo driver HAL (`chxextensionmodule.cpp`) aborta `ConfigureStreams()` al verificar la EEPROM de los 3 sensores agrupados.

El diagnóstico en dispositivo demostró que:
1. El modo **Escáner de QR** nativo de Samsung (`com.sec.android.app.camera.action.SCAN_QR_CODE`) asigna internamente `initialShootingMode = 40` (`CameraId.BACK`), abriendo directamente el dispositivo independiente **`device 56`** (`chiCameraId: 6`, `numPhysicalCameras: 1`, `sensorId: 0`).
2. Los modos **Pro** (`initialShootingMode = 3`, `previous_mode="Pro"`, `activity_name="com.sec.android.app.camera.shootingmode.pro"`) y **Video Pro** (`initialShootingMode = 36`, `previous_mode="Pro video"`, `activity_name="com.sec.android.app.camera.shootingmode.provideo"`) en `ShootingModeMap.initializeBackCameraIdListMapForProAndProVideo()` también evitan `BACK_SEAMLESS_ZOOM` (`device 20`) y abren directamente **`device 56`** (1x Principal `S5KGN3`) y **`device 52`** (3x Telefoto `S5K3K1`) con todo el pipeline ISP de hardware.
3. Las cámaras frontales (`ID 1` Gran Angular 12 MP y `ID 3` Recorte Normal 8.6 MP) son dispositivos independientes (`numPhysicalCameras: 1`) accesibles directamente mediante la API pública `android.hardware.camera2`.

---

## 2. Objetivos y Alcance por Versiones

### Versión 1.0 (MVP — Enfoque Principal)
- **Apertura de Cámara Trasera Principal (`S5KGN3` / `device 56`) con Doble Mecanismo de Rescate:**
  1. **Modo Escáner QR + HUD Obturador Flotante (`SCAN_QR_CODE` + `RearCaptureService`):** Invoca el Intent público `com.sec.android.app.camera.action.SCAN_QR_CODE` (que garantiza abrir `device 56` imitando exactamente al escáner QR nativo) y superpone un **HUD Flotante Minimalista** (`TYPE_ACCESSIBILITY_OVERLAY` + `AccessibilityService.takeScreenshot(Display.DEFAULT_DISPLAY, ...)`) con botón obturador para capturar y recortar el visor óptico limpio (`[46, 360] – [1035, 1679]` o pantalla completa) y guardarlo como JPEG en `Pictures/S23Camera`.
  2. **Modo Directo ISP Pro (`previous_mode="Pro"` / `"Pro video"`):** Invoca `com.sec.android.app.camera/.Camera` con `FLAG_ACTIVITY_NEW_TASK | FLAG_ACTIVITY_CLEAR_TASK` y los extras de inicialización de Modo Pro (`ShootingMode 3`) o Video Pro (`ShootingMode 36`), habilitando captura nativa de hardware en `DCIM/Camera` tanto con el lente 1x (`device 56`) como con el Telefoto 3x (`device 52`).
- **Guardado Seguro en Almacenamiento Local (`MediaStore`):**
  - Almacenamiento transaccional en `Pictures/S23Camera` (galería pública de Android) usando `ContentResolver` + `MediaStore.Images.Media` (`IS_PENDING = 1` -> escritura JPEG 95% -> `IS_PENDING = 0`), visible inmediatamente en Samsung Gallery y Google Fotos.
- **Soporte Nativo `Camera2` para Cámaras Frontales (`ID 1` y `ID 3`):**
  - Visor `TextureView` en vivo integrado en `MainActivity` para tomar selfies con los sensores `ID 1` (`4000x3000`, 12 MP) e `ID 3` (`3392x2544`, 8.6 MP) y conmutar rápidamente entre cámara trasera (`device 56`) y frontal (`ID 1` / `ID 3`).

### Versiones Futuras Planificadas (v1.1+)
- Controles avanzados de exposición manual en cámara frontal, temporizador de disparo, cuadrícula de composición y accesos directos de widgets/Quick Settings Tile.

---

## 3. Arquitectura de Componentes

```
com.s23plus.camera
├── CameraConfig.java              # Constantes verificadas de hardware (IDs 1, 3, 52, 56), Intents y cálculo puro de recorte del visor
├── MainActivity.java              # Pantalla principal: visor Camera2 frontal (ID 1/3) + lanzadores Traseros (QR + HUD y Modo Pro ISP)
├── RearCaptureService.java        # AccessibilityService + Overlay HUD flotante (Obturador, Modo Encuadre, Giro a Frontal, Cerrar)
├── FrontCameraController.java     # Controlador Camera2 limpio y seguro para sensores públicos funcionales (ID 1 e ID 3)
└── MediaStoreSaver.java           # Guardado atómico de fotografías JPEG en MediaStore (Pictures/S23Camera)
```

---

## 4. Seguridad, Privacidad y Estándares de Código

1. **Principio de Mínimo Privilegio:**
   - Solo solicita `android.permission.CAMERA` (para sensores frontales `ID 1`/`3`) y el servicio de accesibilidad `RearCaptureService` con `android:canTakeScreenshot="true"` y `TYPE_ACCESSIBILITY_OVERLAY` (sin leer contenido de ventanas de texto ni eventos de teclado: `android:accessibilityEventTypes=""`, `android:canRetrieveWindowContent="false"`).
   - Cero permisos de red (`INTERNET` no declarado): la aplicación es **100% offline** por diseño.
2. **Gestión Segura de Recursos:**
   - Cierre determinista de `CameraDevice`, `CameraCaptureSession`, `ImageReader` y `HandlerThread` en `onPause()` / `onDestroy()`.
   - Escritura en `MediaStore` con bloques `try-with-resources` y reversión limpia (`resolver.delete(uri, null, null)`) en caso de excepción de E/S.
3. **CI/CD de Costo $0:**
   - Compilación y pruebas diarias 100% en local mediante el script reproducible `scripts/build-local.ps1`.
   - Workflow de GitHub Actions (`.github/workflows/release-apk.yml`) configurado exclusivamente con `on: push: tags: ['v*']` para generar el APK firmado y publicarlo en GitHub Releases solo al lanzar versiones oficiales.
