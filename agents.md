# Agents Specification: Air-Gapped Internal Architecture

## 1. Input Isolation Agent
* **Role:** Manages user interactions via a proprietary input layer.
* **Implementation:** Renders a custom virtual keyboard and touch-surface directly to a private `SurfaceView`.
* **Isolation:** Bypasses the Android Input Method Editor (IME) and Accessibility frameworks to prevent the host OS or background processes from logging keystrokes or touch coordinates.

## 2. Media Silo Agent
* **Role:** Handles camera and microphone hardware streams.
* **Implementation:** Utilizes NDK-level access to process raw media buffers entirely in volatile, app-private memory.
* **Isolation:** Blocks standard system broadcast intents and prevents file indexing by the OS MediaStore. All media is encrypted before being cached to internal storage.

## 3. Cryptographic Warden
* **Role:** Enforces data-at-rest security and key lifecycle.
* **Implementation:** Manages hardware-backed AES-256-GCM encryption keys via Android StrongBox or the Trusted Execution Environment (TEE).
* **Isolation:** Ensures that data is never stored in plain text and that encryption keys are hardware-bound, preventing extraction even if the host system is compromised or rooted.

## 4. Local Execution Agent
* **Role:** Runs all application logic and features without external dependencies.
* **Implementation:** Contains all required binaries, models, and libraries within the APK.
* **Isolation:** Explicitly lacks the `INTERNET` permission. Implements `FLAG_SECURE` to prevent the host system from capturing screen data or displaying content in the task switcher.

## Isolation Matrix

| Host Vector | Mitigation Strategy |
| :--- | :--- |
| **System Keyboard** | Proprietary internal keyboard layer (No IME). |
| **Screen Capture** | `WindowManager.LayoutParams.FLAG_SECURE` enforcement. |
| **Cloud/Network** | Complete removal of network-related permissions. |
| **OS Media Indexing** | NDK-level raw buffer processing; private data silos. |
| **System Backups** | `android:allowBackup="false"` + hardware-bound keys. |