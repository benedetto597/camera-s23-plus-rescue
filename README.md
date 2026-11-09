# S23+ Camera Rescue (`com.s23plus.camera`)

Aplicación de cámara minimalista, limpia, segura y **100% offline** diseñada específicamente para rescatar la funcionalidad fotográfica de un **Samsung Galaxy S23+ (`SM-S916U1`)** cuando el módulo físico Ultra-Wide 0.6x (`IMX564`, `sensorId 2`, dispositivo `ID 2` / `ID 58`) presenta fallo de hardware (EEPROM/I2C), dejando intactos el **lente trasero Principal 1x de 50 MP (`S5KGN3`, `ID 56`)**, el **Telefoto 3x de 10 MP (`S5K3K1`, `ID 52`)** y las **dos cámaras frontales (`S5K3LU`, `ID 1` e `ID 3`)**.

---

## 🔬 ¿Por qué fallan todas las apps de cámara convencionales y cómo lo resuelve esta app?

En el Samsung Galaxy S23+, el proveedor HAL (`vendor.samsung.hardware.camera.provider-service_64`) expone la cámara trasera pública como el dispositivo lógico multi-cámara **`ID 0`** (`[2, 5, 6]`) y el modo Foto/Video nativo usa **`device 20`** (`BACK_SEAMLESS_ZOOM` = `[58, 56, 52]`). Al abrir cualquiera de ellos, la función `isAvailableModule()` del driver `chxextensionmodule.cpp` verifica la EEPROM de los **3 sensores simultáneamente**, abortando toda la sesión por la falla eléctrica del sensor Ultra-Wide (`sensorId 2`).

Sin embargo, el análisis de ingeniería inversa sobre `ShootingModeMap` y `ShootingModeProviderImpl` en el dispositivo demostró que:
1. **Modo Pro (`ShootingMode 3`) y Video Pro (`ShootingMode 36`)** mapean el lente `W` (1x Principal) directamente a **`CameraId.BACK` (`device 56`)** y el lente `T` (3x Telefoto) directamente a **`CameraId.BACK_TELE` (`device 52`)**, inicializando cada sensor de forma individual (`numPhysicalCameras: 1`) sin tocar el sensor Ultra-Wide dañado.
2. **Modo Escáner de QR (`ShootingMode 40`, `SCAN_QR_CODE`)** abre directamente **`device 56`** (`S5KGN3` 1x Principal) con autoenfoque continuo y flash.
3. **Cámaras Frontales (`ID 1` 12 MP Gran Angular e `ID 3` 8.6 MP Normal)** son dispositivos públicos independientes que funcionan al 100% mediante la API estándar `android.hardware.camera2`.

Documentación técnica completa:
- [Diagnóstico de Hardware y Subsistema HAL (`docs/hardware-diagnosis.md`)](docs/hardware-diagnosis.md)
- [Arquitectura de Software y Seguridad (`docs/architecture.md`)](docs/architecture.md)
- [Especificación de Diseño (`docs/superpowers/specs/2026-11-08-s23-plus-camera-design.md`)](docs/superpowers/specs/2026-11-08-s23-plus-camera-design.md)
- [Plan de Implementación (`docs/superpowers/plans/2026-11-08-s23-plus-camera-plan.md`)](docs/superpowers/plans/2026-11-08-s23-plus-camera-plan.md)

---

## ✨ Características Principales (`v1.0.1`)

1. **Identidad Visual e Interfaz Moderna (`S23+ Camera Rescue`):**
   - Icono adaptativo vectorial (`@mipmap/ic_launcher`) con diseño de lente óptico e insignia de rescate.
   - Botones de acción con nombres cortos y directos (**`Tomar Selfie`**, **`Cámara Trasera Pro`**, **`Trasera Rápida (QR)`**, **`Video Trasero Pro`**) acompañados de botones desplegables (**`▾` / `▴`**) que muestran u ocultan la ficha técnica de cada sensor y modo HAL.
2. **Visor Frontal Nativo `Camera2` (`ID 1` y `ID 3`) Integrado:**
   - Vista previa fluida en tiempo real (`TextureView`).
   - Conmutación instantánea entre **`ID 1` (12 MP Gran Angular, `4000x3000`)** e **`ID 3` (8.6 MP Normal, `3392x2544`)**.
   - Disparo directo y guardado transaccional JPEG en `Pictures/S23Camera` (visible de inmediato en Samsung Gallery y Google Fotos).
3. **Lanzador Directo Trasero 1x / 3x — Modo Pro ISP (`device 56` y `device 52`):**
   - Abre la cámara de Samsung limpiando cualquier estado colgado (`FLAG_ACTIVITY_NEW_TASK | FLAG_ACTIVITY_CLEAR_TASK`) directamente sobre el sensor `S5KGN3` (1x `W`, `device 56`) y `S5K3K1` (3x `T`, `device 52`) con todo el procesamiento ISP de hardware (Auto ISO, Auto Obturador, Auto-Focus, Flash y disparo nativo en `DCIM/Camera`).
   - Incluye también acceso directo a **Video Trasero Pro (`device 56` / `52`)**.
4. **Lanzador Trasero 1x vía Escáner QR + HUD Obturador Flotante (`device 56`):**
   - Abre el visor `SCAN_QR_CODE` (`device 56`) y superpone una barra flotante minimalista (`RearCaptureService`) con botón **Capturar**, selector de recorte (**Visor** vs **Completo**) y botón para volver a la cámara frontal.

---

## 💡 Tip Adicional: Dejar la Cámara Nativa de Samsung Fija en el Lente Funcional

Además de usar `S23+ Camera Rescue`, puedes dejar la aplicación nativa de Samsung configurada para que al abrir su icono normal (o con doble toque al botón de encendido) **no vuelva a cerrarse**:
1. Abre **Modo Pro** desde el botón azul de `S23+ Camera Rescue`.
2. Toca el icono de **Ajustes (⚙️)** arriba a la izquierda en la cámara.
3. Entra en **Ajustes para conservar (*Settings to keep*)** y activa **Modo de cámara (*Camera mode*)**.
4. Mientras dejes la cámara en **Pro** (con ISO/Speed en `AUTO` y lente `W` 1x o `T` 3x), la cámara nativa recordará iniciar siempre en `device 56` en vez del modo Foto (`device 20`), evitando por completo el sensor Ultra-Wide dañado. Si alguna vez cambias por error a Foto o `UW`, pulsa el botón de rescate en `S23+ Camera Rescue` para destrabarla al instante.

---

## 🛠️ Compilación y Pruebas en Local (Costo $0)

### Ejecutar pruebas unitarias
```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\test-local.ps1
```

### Compilar y firmar el APK en local
```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\build-local.ps1
```
El APK firmado se genera en `dist/s23-plus-camera-rescue-v1.0.1.apk`.

### Instalar en el S23+ vía ADB
```powershell
adb install -r .\dist\s23-plus-camera-rescue-v1.0.1.apk
```

---

## 🚀 Publicación de Versiones con GitHub Actions ($0 Costo)

El workflow [`.github/workflows/release-apk.yml`](.github/workflows/release-apk.yml) está configurado para ejecutarse **únicamente cuando se envía un tag de versión (`v*`)**:
```bash
git tag v1.0.1
git push origin v1.0.1
```
Esto mantiene el consumo de minutos de CI en `0` durante el desarrollo normal.
