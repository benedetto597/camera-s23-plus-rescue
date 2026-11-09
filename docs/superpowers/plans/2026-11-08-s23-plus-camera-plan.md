# S23+ Camera Rescue (`com.s23plus.camera`) — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Construir, verificar en dispositivo e implementar en GitHub la aplicación Android minimalista, limpia y segura `S23+ Camera Rescue` (`com.s23plus.camera`) que permite utilizar el sensor trasero principal 1x (`S5KGN3` / `device 56`), el telefoto 3x (`S5K3K1` / `device 52`) y las dos cámaras frontales (`S5K3LU` / `ID 1` e `ID 3`) en un Samsung Galaxy S23+ (`SM-S916U1`) con el módulo Ultra-Wide (`IMX564` / `sensorId 2`) averiado.

**Architecture:**
1. **`CameraConfig`**: Clase pura de configuración y cálculo geométrico sin dependencias de Android UI, con constantes verificadas en hardware (`FRONT_WIDE_ID = "1"`, `FRONT_NORMAL_ID = "3"`, `REAR_MAIN_INTERNAL_ID = "56"`, `REAR_TELE_INTERNAL_ID = "52"`), generación de parámetros de Intent para el rescate en **Modo Pro ISP (`ShootingMode 3`)**, **Video Pro (`ShootingMode 36`)** y **Escáner QR (`ShootingMode 40`)**, y cálculo determinista de recorte del visor óptico.
2. **`MediaStoreSaver`**: Guardado transaccional de imágenes JPEG en `MediaStore.Images.Media` (`Pictures/S23Camera`) con `IS_PENDING = 1 -> 0` y limpieza automática en caso de error.
3. **`FrontCameraController`**: Controlador `android.hardware.camera2` limpio y seguro para las cámaras frontales públicas `ID 1` (12 MP Gran Angular) e `ID 3` (8.6 MP Normal), con vista previa fluida en `TextureView`, autoenfoque continuo y captura JPEG.
4. **`RearCaptureService`**: `AccessibilityService` minimalista (`canTakeScreenshot="true"`, `canRetrieveWindowContent="false"`) que despliega un HUD flotante (`TYPE_ACCESSIBILITY_OVERLAY`) sobre el visor trasero `device 56` en modo Escáner QR para capturar fotografías instantáneas vía `takeScreenshot(Display.DEFAULT_DISPLAY, ...)` y guardarlas en `Pictures/S23Camera`.
5. **`MainActivity`**: Interfaz unificada con visor frontal `Camera2` en vivo, conmutador `ID 1` / `ID 3`, botón de rescate directo a **Cámara Trasera 1x/3x (Modo Pro ISP — `device 56` / `52`)**, botón de **Video Pro (`device 56` / `52`)** y botón de **Cámara Trasera 1x vía Escáner QR + HUD Flotante (`device 56`)**.
6. **Automatización Local y CI/CD de Costo $0**: Script `scripts/build-local.ps1` y `scripts/test-local.ps1` para compilación y pruebas unitarias en local sin costo, más `.github/workflows/release-apk.yml` disparado únicamente al publicar tags `v*`.

**Tech Stack:** Java 17, Android SDK 34 (Min SDK 30 / Target SDK 34), Android `Camera2` API, `MediaStore`, `AccessibilityService.takeScreenshot`, Gradle Kotlin DSL (`build.gradle.kts`), GitHub Actions.

---

### Task 1: Estructura Base del Proyecto Gradle y Configuración Git

**Files:**
- Create: `.gitignore`
- Create: `settings.gradle.kts`
- Create: `build.gradle.kts`
- Create: `gradle.properties`
- Create: `app/build.gradle.kts`

- [x] **Step 1: Crear `.gitignore` y archivos Gradle raíz**
- [x] **Step 2: Crear `app/build.gradle.kts` con configuración limpia sin dependencias externas innecesarias**

---

### Task 2: Lógica Pura de Configuración y Pruebas Unitarias (`CameraConfig` & `CameraConfigTest`)

**Files:**
- Create: `app/src/main/java/com/s23plus/camera/CameraConfig.java`
- Create: `app/src/test/java/com/s23plus/camera/CameraConfigTest.java`
- Create: `scripts/test-local.ps1`

- [x] **Step 1: Escribir pruebas unitarias en `CameraConfigTest.java` verificando IDs de cámaras, constantes de Intents y cálculo de recorte del visor óptico**
- [x] **Step 2: Implementar `CameraConfig.java` y ejecutar `scripts/test-local.ps1` con JDK 17 local**

---

### Task 3: Guardado Transaccional en Almacenamiento Local (`MediaStoreSaver`) y Controlador `Camera2` Frontal (`FrontCameraController`)

**Files:**
- Create: `app/src/main/java/com/s23plus/camera/MediaStoreSaver.java`
- Create: `app/src/main/java/com/s23plus/camera/FrontCameraController.java`

- [x] **Step 1: Implementar `MediaStoreSaver.java` con escritura atómica JPEG en `Pictures/S23Camera`**
- [x] **Step 2: Implementar `FrontCameraController.java` para `ID 1` (12 MP) e `ID 3` (8.6 MP) con manejo determinista del ciclo de vida y orientación EXIF/JPEG**

---

### Task 4: Servicio de Captura Trasera Flotante (`RearCaptureService`) y Actividad Principal (`MainActivity`)

**Files:**
- Create: `app/src/main/res/xml/accessibility_service_config.xml`
- Create: `app/src/main/AndroidManifest.xml`
- Create: `app/src/main/java/com/s23plus/camera/RearCaptureService.java`
- Create: `app/src/main/java/com/s23plus/camera/MainActivity.java`
- Create: `scripts/build-local.ps1`

- [x] **Step 1: Configurar `accessibility_service_config.xml` y `AndroidManifest.xml` siguiendo el principio de mínimo privilegio (sin permiso `INTERNET`, sin lectura de texto de pantalla)**
- [x] **Step 2: Implementar `RearCaptureService.java` y `MainActivity.java`**
- [x] **Step 3: Compilar el APK firmado `dist/s23-plus-camera-v1.0.0.apk` mediante `scripts/build-local.ps1`**

---

### Task 5: Verificación en Vivo en el Samsung Galaxy S23+ (`R5CTC0VM96B`) y Configuración Permanente de Cámara Nativa

**Files:**
- Modify: Dispositivo conectado vía ADB (`SM-S916U1`)

- [x] **Step 1: Configurar "Ajustes para conservar -> Modo de cámara" en la app nativa de Samsung Camera mientras está en Modo Pro (`device 56`) y verificar que al cerrarla y reabrirla inicia directamente en el lente 1x funcional**
- [x] **Step 2: Limpiar APK temporal de diagnóstico (`com.s23plus.probe`), instalar `dist/s23-plus-camera-v1.0.0.apk`, otorgar permisos y verificar captura frontal (`ID 1`/`3`), rescate Pro (`device 56`) y captura trasera con HUD flotante (`SCAN_QR_CODE` + `RearCaptureService`)**

---

### Task 6: CI/CD de Costo $0, Documentación de Alto Estándar y Publicación en GitHub

**Files:**
- Create: `.github/workflows/release-apk.yml`
- Create: `README.md`
- Create: `SECURITY.md`
- Create: `docs/architecture.md`

- [x] **Step 1: Crear `.github/workflows/release-apk.yml` restringido estrictamente a `on: push: tags: ['v*']`**
- [x] **Step 2: Redactar `README.md`, `SECURITY.md` y `docs/architecture.md`**
### Task 7: Lanzamiento v1.0.1 — Icono Adaptativo, Rediseño de Interfaz Moderna con Desplegables Técnicos (`▾`) y Renombrado a `camera-s23-plus-rescue`

**Files:**
- Create: `app/src/main/res/drawable/ic_launcher_background.xml`
- Create: `app/src/main/res/drawable/ic_launcher_foreground.xml`
- Create: `app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml`
- Create: `app/src/main/res/mipmap-anydpi-v26/ic_launcher_round.xml`
- Modify: `app/src/main/AndroidManifest.xml`
- Modify: `app/build.gradle.kts`
- Modify: `settings.gradle.kts`
- Modify: `app/src/main/java/com/s23plus/camera/CameraConfig.java`
- Modify: `app/src/test/java/com/s23plus/camera/CameraConfigTest.java`
- Modify: `app/src/main/java/com/s23plus/camera/MainActivity.java`
- Modify: `app/src/main/java/com/s23plus/camera/RearCaptureService.java`
- Modify: `scripts/build-local.ps1`
- Modify: `README.md`

- [x] **Step 1: Crear el icono adaptativo vectorial (`ic_launcher_background.xml`, `ic_launcher_foreground.xml`, `ic_launcher.xml`, `ic_launcher_round.xml`) y configurar `AndroidManifest.xml` con `S23+ Camera Rescue` y versión `1.0.1` (`versionCode = 2`)**
- [x] **Step 2: Rediseñar `MainActivity.java` y `RearCaptureService.java` con estética moderna oscura, botones de nombres cortos y tarjetas con botón de flecha (`▾` / `▴`) para desplegar descripciones técnicas**
- [x] **Step 3: Actualizar y ejecutar pruebas unitarias (`CameraConfigTest`) y compilar `dist/s23-plus-camera-rescue-v1.0.1.apk`**
- [x] **Step 4: Actualizar la app en el S23+ conectado vía ADB y verificar visual y funcionalmente la nueva interfaz v1.0.1 y los desplegables técnicos**
- [x] **Step 5: Renombrar el repositorio de GitHub a `camera-s23-plus-rescue`, hacer push de `main` y publicar el tag `v1.0.1`**

