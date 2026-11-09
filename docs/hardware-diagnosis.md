# Diagnóstico de Hardware y Subsistema de Cámara — Samsung Galaxy S23+ (`SM-S916U1`)

**Fecha de diagnóstico:** 8 de noviembre de 2026  
**Dispositivo:** Samsung Galaxy S23+ (`SM-S916U1`)  
**Versión de SO / Firmware:** Android 16 (API 36) / One UI 8 (`BP4A.251205.006.S916U1UES8FZI1`)  
**Herramientas utilizadas:** `adb dumpsys media.camera`, `adb logcat` (`CameraService`, `CHI`, `CamX`), y suite de sondeo en dispositivo (`com.s23plus.probe` v0.5–v0.8).

---

## 1. Mapa Completo de Sensores y Dispositivos HAL (`ICameraProvider/internal/0-1`)

El proveedor HAL de cámara de Samsung (`vendor.samsung.hardware.camera.provider-service_64`) registra **12 dispositivos de cámara** a nivel interno, de los cuales Android `CameraManager` expone públicamente `0, 1, 2, 3` (y metadatos físicos para `5, 6`):

| ID Cámara | Tipo | Dirección | Sensor Físico | Focal / Resolución Activa | Estado de Hardware | Acceso API Estándar (`Camera2`) |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| **`0`** | Lógica Multi-Cámara (`[2, 5, 6]`) | Trasera (`BACK`) | Agrupa `IMX564` + `S5KGN3` + `S5K3K1` | `5.4 mm` / `4080x3060` | **Bloqueada por fallo en sub-módulo `2`** | `openCamera("0")` abre, pero `ConfigureStreams()` falla con `-38 (ENOSYS)` |
| **`1`** | Física / Pública | Frontal (`FRONT`) | `S5K3LU` (`sensorId 3`, modo completo) | `3.3 mm` / `4000x3000` (12 MP) | **100% Funcional** | **Acceso público total** (Preview + JPEG 30 fps verificados) |
| **`2`** | Física / Pública | Trasera (`BACK`) | `IMX564` (`sensorId 2`, Ultra-Wide 0.6x) | `2.2 mm` / `4000x3000` (12 MP) | **DAÑADO (Fallo I2C / EEPROM)** | Falla al inicializar sensor (`CamxResultEFailed`) |
| **`3`** | Física / Pública | Frontal (`FRONT`) | `S5K3LU` (`sensorId 3`, recorte 1x) | `3.3 mm` / `3392x2544` (8.6 MP) | **100% Funcional** | **Acceso público total** (Preview + JPEG 30 fps verificados) |
| **`4`** | Segura (`SECURE_IMAGE_DATA`) | Frontal (`FRONT`) | `S5K3LU` | `3.3 mm` / `4000x3000` | Funcional | Restringida a sistema (reconocimiento facial) |
| **`5`** | Sub-cámara física de `0` | Trasera (`BACK`) | `S5KGN3` (`sensorId 0`, Principal 1x 50MP) | `5.4 mm` / `4080x3060` | **100% Funcional** | No permite `openCamera("5")` directo; dentro de `0` es bloqueada por el chequeo global de `sensorId 2` |
| **`6`** | Sub-cámara física de `0` | Trasera (`BACK`) | `S5K3K1` (`sensorId 1`, Telefoto 3x 10MP) | `7.0 mm` / `3648x2736` | Funcional | No permite `openCamera("6")` directo |
| **`20`** | Lógica Interna (`[58, 56, 52]`) | Trasera (`BACK`) | `IMX564` + `S5KGN3` + `S5K3K1` | `5.4 mm` / `4080x3060` | Bloqueada por `58` (`sensorId 2`) | Oculta (`cameraId >= 20`, solo sistema) |
| **`21`** | Lógica Interna (`[52, 56]`) | Trasera (`BACK`) | `S5K3K1` + `S5KGN3` (sin Ultra-Wide) | `7.0 mm` / `3648x2736` | Funcional | Oculta (`cameraId >= 20`, solo sistema) |
| **`23`** | Lógica Interna (`[56, 58]`) | Trasera (`BACK`) | `S5KGN3` + `IMX564` | `5.4 mm` / `4080x3060` | Bloqueada por `58` (`sensorId 2`) | Oculta (`cameraId >= 20`, solo sistema) |
| **`52`** | Física Interna Independiente | Trasera (`BACK`) | `S5K3K1` (`sensorId 1`, Telefoto 3x) | `7.0 mm` / `3648x2736` | Funcional | Oculta (`cameraId >= 20`, solo sistema) |
| **`56`** | **Física Interna Independiente** | Trasera (`BACK`) | **`S5KGN3` (`sensorId 0`, Principal 1x)** | `5.4 mm` / `4080x3060` | **100% Funcional** | **Usada por el Escáner QR de Samsung (`com.sec.android.app.camera`)** |
| **`58`** | Física Interna Independiente | Trasera (`BACK`) | `IMX564` (`sensorId 2`, Ultra-Wide 0.6x) | `2.2 mm` / `4000x3000` | **DAÑADO** | Oculta (`cameraId >= 20`, solo sistema) |
| **`90`** | Segura (`SECURE_IMAGE_DATA`) | Frontal (`FRONT`) | `S5K3LU` | `3.3 mm` / `4000x3000` | Funcional | Oculta (`cameraId >= 20`, solo sistema) |

---

## 2. Causa Raíz Exacta a Nivel de Driver (`CHI` / `CamX`)

### 2.1. Evidencia del fallo físico en el sensor Ultra-Wide (`sensorId 2` / `IMX564`)
Al intentar abrir `ID 2` o configurar streams en `ID 0`, el módulo de extensión de Qualcomm/Samsung (`chxextensionmodule.cpp` y `camxsensornode.cpp`) registra:

```text
E CHI  : [SS_ERR ]: [CHI]: chxextensionmodule.cpp: isAvailableModule: 16079: Unavailable Cal Data, check i2c or module eeprom
E CHI  : [SS_ERR ]: [CHI]: chxextensionmodule.cpp: isAvailableModule: 16201: Invalid sizes of moduleinfo, make camera fail (13 >= 8)
E CHI  : [SS_ERR ]: [CHI]: chxextensionmodule.cpp: isAvailableModule: 16254: invalid module : 0x05
E CHI  : [SS_ERR ]: [CHI]: chxextensionmodule.cpp: isAvailableModule: 16256: numPhysicalCameras: 3, chiCameraId: 16, sensorId 2
E CHI  : [SS_ERR ]: [CHI]: chxextensionmodule.cpp: InitializeOverrideSession: 6032: Unavailable camera module (4)
E CamX : [ERROR][HAL] camxhaldevice.cpp:2354 ConfigureStreams() CHI Module failed to configure streams
```

### 2.2. ¿Por qué fallan todas las apps de terceros al abrir la cámara trasera?
1. Las aplicaciones de terceros (OpenCamera, WhatsApp, Instagram, GCam, etc.) solo ven `ID 0` como cámara trasera principal.
2. `ID 0` mapea a `chiCameraId: 16`, que tiene `numPhysicalCameras: 3` (`sensorId 0`, `sensorId 1`, `sensorId 2`).
3. Aunque una app use la API de cámaras físicas de Android 9+ (`OutputConfiguration.setPhysicalCameraId("5")`) o `CameraExtensionSession`, la función `InitializeOverrideSession` de Samsung verifica `isAvailableModule()` para **los 3 sensores** del grupo lógico antes de inicializar el pipeline. Como `sensorId 2` tiene su EEPROM/I2C inaccesible, `InitializeOverrideSession` aborta con `Unavailable camera module (4)`.

### 2.3. ¿Por qué el Escáner de QR nativo de Samsung, el Modo Pro y `*#0*#` SÍ logran abrir la cámara trasera?
Al decompilar la tabla de ruteo de sensores de Samsung Camera (`com.sec.android.app.camera.util.ShootingModeMap` y `ShootingModeProviderImpl`) y verificar en vivo con `dumpsys media.camera` y `logcat` (`ShootingModeProvider`, `CameraSettings`), se descubrió el mecanismo exacto:

```text
I ShootingModeProvider: initializeCameraId : initialShootingMode=40, facing=0, lens type =0
I CameraSettings: setCameraId : BACK
I CameraService: Camera 56: Access Check: Package: com.sec.android.app.camera Strategy: 0
I CameraService: CONNECT device 56 client for package com.sec.android.app.camera (PID ...)
```

1. **Modos Estándar (`Photo` = `0`, `Video` = `1`, `Portrait` = `28`, `Night` = `30`):**
   - En `ShootingModeMap.initializeBackCameraIdListMap()`, los modos Foto y Video estándar mapean por defecto a **`CameraId.BACK_SEAMLESS_ZOOM` (`device 20` = `[58, 56, 52]`)**, el cual intenta energizar simultáneamente los 3 sensores traseros y aborta inmediatamente por el fallo de EEPROM en `device 58` (`IMX564`, `sensorId 2`).
2. **Modo Escáner de QR (`Qr` = `40`, `com.sec.android.app.camera.action.SCAN_QR_CODE`):**
   - Al invocar el Intent público exportado `com.sec.android.app.camera.action.SCAN_QR_CODE` (`com.sec.android.app.camera/.QrScannerActivity`), la aplicación nativa redirige internamente a `com.sec.android.app.camera/.Camera` con la acción `com.sec.android.app.camera.action.QR_SCANNER_MODE` y los extras `"camerafacing"="back"` e `"is_qr_mode"=true`.
   - Esto asigna `initialShootingMode = 40`, seleccionando **`CameraId.BACK` (`device 56`)**, que abre únicamente el sensor principal 1x (`S5KGN3`, `chiCameraId: 6`, `numPhysicalCameras: 1`, `sensorId: 0`).
3. **Modo Pro (`Pro` = `3`) y Video Pro (`Pro video` = `36`):**
   - En `ShootingModeMap.initializeBackCameraIdListMapForProAndProVideo()`, Samsung mapea cada lente de forma individual en lugar de usar `BACK_SEAMLESS_ZOOM`:
     - Lente `0` (`W` / 1x Principal) $\rightarrow$ **`CameraId.BACK` (`device 56`, sensor `S5KGN3` 50 MP / 12 MP)**.
     - Lente `2` (`T` / 3x Telefoto) $\rightarrow$ **`CameraId.BACK_TELE` (`device 52`, sensor `S5K3K1` 10 MP)**.
     - Lente `1` (`UW` / 0.6x Ultra-Wide) $\rightarrow$ `CameraId.BACK_WIDE` (`device 58`, dañado).
   - Al lanzar `com.sec.android.app.camera/.Camera` con `Intent.ACTION_MAIN` y los extras `"previous_mode"="Pro"`, `"activity_name"="com.sec.android.app.camera.shootingmode.pro"` y `"camerafacing"="back"`, Samsung Camera abre directamente **`device 56`** con todo el pipeline ISP de hardware habilitado (Auto ISO, Auto Obturador, Auto-Focus, Flash, cambio a Telefoto 3x `device 52` y a Frontal `ID 1`/`3`, y guardado nativo de JPEGs de alta resolución en `DCIM/Camera`).

---

## 3. Restricciones de Seguridad de Android y Estrategia de Arquitectura Dual

1. **Bloqueo de `cameraId >= 20` para apps de terceros:**
   - En el servicio nativo `cameraserver` de Samsung (`CameraService::validateClientPermissionsLocked`), cualquier solicitud `openCamera()` a un `cameraId >= 20` (como `56` o `52`) valida que el UID llamante sea `AID_SYSTEM` (`1000`) o esté en la lista blanca firmada por plataforma (`com.sec.android.app.camera`). A las aplicaciones de terceros se les rechaza con `Status(-8, EX_SERVICE_SPECIFIC):8: validateClientPermissionsLocked:1624: Caller "com.s23plus..." (PID ..., UID ...) cannot open camera "56"`.
2. **Solución de Arquitectura Híbrida Dual 100% Funcional (Sin Root, APIs Oficiales):**
   - **Cámara Trasera Principal 1x (`S5KGN3` — `device 56`) y Telefoto 3x (`S5K3K1` — `device 52`):**
     - **Modo A — Lanzador Directo ISP Pro (`previous_mode="Pro"` / `"Pro video"`):** Inicia `com.sec.android.app.camera/.Camera` limpiando tareas previas (`FLAG_ACTIVITY_NEW_TASK | FLAG_ACTIVITY_CLEAR_TASK`) directamente en Modo Pro (`ShootingMode 3`) o Video Pro (`ShootingMode 36`) sobre `device 56` (`1x W`) y `device 52` (`3x T`), permitiendo disparo ISP completo de hardware en `/sdcard/DCIM/Camera/`.
     - **Modo B — Bypass Escáner QR + HUD Obturador Flotante (`SCAN_QR_CODE` + `RearCaptureService`):** Abre el pipeline infalible `device 56` mediante `com.sec.android.app.camera.action.SCAN_QR_CODE` y superpone un HUD minimalista (`TYPE_ACCESSIBILITY_OVERLAY` + `AccessibilityService.takeScreenshot(Display.DEFAULT_DISPLAY, ...)`) que captura el frame limpio del visor óptico y lo guarda instantáneamente en `Pictures/S23Camera`.
   - **Cámaras Frontales (`S5K3LU` — `ID 1` 12 MP Gran Angular y `ID 3` 8.6 MP Normal):**
     - Integradas directamente dentro de `MainActivity` mediante el pipeline nativo **Android `Camera2` API** (`CameraDevice`, `TextureView`, `ImageReader` JPEG 95%, autoenfoque continuo y guardado atómico en `Pictures/S23Camera`).

