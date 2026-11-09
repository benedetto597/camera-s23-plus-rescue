# Arquitectura de Software — S23+ Camera Rescue (`com.s23plus.camera`)

## 1. Diagrama de Flujo y Ruteo de Sensores

```mermaid
flowchart TD
    User["Usuario en S23+ (SM-S916U1)"] --> MainActivity["MainActivity (com.s23plus.camera)"]

    MainActivity -->|Visor en Vivo + Disparo Directo| FrontCtrl["FrontCameraController (Camera2 API)"]
    FrontCtrl -->|openCamera ID 1| FrontWide["Sensor Frontal S5K3LU 12MP (4000x3000)"]
    FrontCtrl -->|openCamera ID 3| FrontNorm["Sensor Frontal S5K3LU 8.6MP (3392x2544)"]
    FrontWide --> MediaStore["MediaStoreSaver (Pictures/S23Camera)"]
    FrontNorm --> MediaStore

    MainActivity -->|Boton 1: Modo Pro / Video Pro| ProIntent["Intent ACTION_MAIN + previous_mode=Pro / Pro video"]
    ProIntent -->|ShootingMode 3 o 36| NativePro["Samsung Camera: CameraId.BACK (ID 56) & BACK_TELE (ID 52)"]
    NativePro -->|Obturador Hardware ISP| NativeDCIM["Galeria Nativa (DCIM/Camera)"]

    MainActivity -->|Boton 2: Modo QR + HUD Flotante| QrIntent["Intent SCAN_QR_CODE + RearCaptureService HUD"]
    QrIntent -->|ShootingMode 40| NativeQr["Samsung Camera: CameraId.BACK (ID 56 - S5KGN3 1x)"]
    NativeQr -->|takeScreenshot + computeCropBounds| MediaStore
```

---

## 2. Responsabilidades por Módulo

| Archivo | Responsabilidad |
| :--- | :--- |
| [`CameraConfig.java`](../app/src/main/java/com/s23plus/camera/CameraConfig.java) | Constantes inmutables de hardware (`ID 1`, `ID 3`, `ID 56`, `ID 52`), acciones/extras de Intent verificados y cálculo geométrico puro (`computeCropBounds`) cubierto por pruebas unitarias. |
| [`FrontCameraController.java`](../app/src/main/java/com/s23plus/camera/FrontCameraController.java) | Gestión del ciclo de vida de `Camera2` (`CameraDevice`, `CameraCaptureSession`, `ImageReader`, `HandlerThread`) para las cámaras frontales `ID 1` y `ID 3`. |
| [`RearCaptureService.java`](../app/src/main/java/com/s23plus/camera/RearCaptureService.java) | HUD flotante (`TYPE_ACCESSIBILITY_OVERLAY`) y captura instantánea del visor trasero `ID 56` mediante `takeScreenshot(Display.DEFAULT_DISPLAY, ...)`. |
| [`MediaStoreSaver.java`](../app/src/main/java/com/s23plus/camera/MediaStoreSaver.java) | Persistencia transaccional de imágenes JPEG en `Pictures/S23Camera` utilizando `IS_PENDING`. |
| [`MainActivity.java`](../app/src/main/java/com/s23plus/camera/MainActivity.java) | Interfaz de usuario principal, coordinación de permisos y lanzamiento limpio de los modos traseros (`Pro`, `Pro video` y `SCAN_QR_CODE`). |
