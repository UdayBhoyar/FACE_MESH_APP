# Face Mesh App

An Android application that enables hands-free device control using eye gaze tracking, head movements, and voice commands. Built with MediaPipe, CameraX, and TensorFlow Lite for real-time face landmark detection and gesture recognition.

## 🚀 Features

### Eye Gaze Control
- **Real-time eye tracking**: Move cursor by looking around the screen
- **Tap gestures**: Open your mouth to click at the cursor position
- **Adjustable sensitivity**: Fine-tune eye tracking responsiveness
- **Calibration system**: Optional 4x4 grid calibration for improved accuracy

### Head Gesture Control
- **Swipe gestures**: Move your head while mouth is open to perform swipes
  - Head left → Swipe right
  - Head right → Swipe left  
  - Head up → Swipe down
  - Head down → Swipe up
- **Hybrid head pose detection**: Combines MediaPipe landmarks with optional TensorFlow Lite model for enhanced accuracy

### Voice Commands
- **Background voice recognition**: Continuous listening when enabled
- **Supported commands**:
  - "Open WhatsApp" / "Open Camera" / "Open [app name]"
  - "Go home" / "Home screen"
  - "Take screenshot" / "Screenshot"
  - "Go back" / "Back"
  - "Show recent apps" / "Recent apps"
  - "Open notifications" / "Notifications"

### Accessibility Features
- **System-wide control**: Works across all apps via Android Accessibility Service
- **Overlay cursor**: Visual feedback for gaze position
- **Foreground service**: Voice commands work in background
- **Permission management**: Guided setup for required permissions

## 📱 Requirements

- **Android SDK**: 24+ (Android 7.0)
- **Target SDK**: 34 (Android 14)
- **Permissions Required**:
  - Camera (for face tracking)
  - Microphone (for voice commands)
  - Accessibility Service (for system-wide gestures)
  - Draw over other apps (for overlay cursor)
  - Foreground service (for background voice recognition)

## 🛠️ Installation

### Prerequisites
1. Android Studio Arctic Fox or later
2. Kotlin 1.9.0+
3. Gradle 8.7+

### Build Instructions

1. **Clone the repository**
   ```bash
   git clone https://github.com/UdayBhoyar/FACE_MESH_APP.git
   cd FACE_MESH_APP
   ```

2. **Open in Android Studio**
   - File → Open → Select the project folder
   - Wait for Gradle sync to complete

3. **Build the project**
   ```bash
   ./gradlew assembleDebug
   ```

4. **Install on device**
   ```bash
   ./gradlew installDebug
   ```

### Optional: Head Pose Model Setup

For enhanced head pose detection, you can add a TensorFlow Lite model:

1. **Convert your Keras model** (if you have one):
   ```bash
   python convert_model.py
   ```

2. **Place the model file**:
   - Copy `head_pose_model.tflite` to `app/src/main/assets/`
   - The app will automatically detect and load the model

## 🎯 Usage

### First Time Setup

1. **Launch the app** and grant camera permission
2. **Enable Accessibility Service**:
   - Go to Settings → Accessibility → Face Mesh App → Turn on
   - This enables system-wide tap and swipe gestures
3. **Enable overlay permission** (optional):
   - Allow "Draw over other apps" to show cursor overlay
4. **Enable voice commands** (optional):
   - Grant microphone permission
   - Toggle voice commands in the app

### Basic Controls

#### Eye Gaze Cursor
- Look around to move the cursor
- Use +/− buttons to adjust sensitivity (3.0-5.0 range)
- The cursor appears as a colored circle on screen

#### Gestures
| Action | How to Perform |
|--------|----------------|
| **Click/Tap** | Open your mouth (closed → open transition) |
| **Swipe Right** | Open mouth + move head left |
| **Swipe Left** | Open mouth + move head right |
| **Swipe Down** | Open mouth + move head up |
| **Swipe Up** | Open mouth + move head down |

#### Voice Commands
- Toggle voice commands on in the app
- Say commands clearly:
  - "Open WhatsApp" → Launches WhatsApp
  - "Take screenshot" → Captures screen
  - "Go home" → Returns to home screen
  - "Go back" → Presses back button

### Advanced Features

#### Calibration
1. Press "Calibrate" button
2. Look at each red dot and press "Capture"
3. Complete all 16 calibration points
4. Press "Done" to save calibration

#### Troubleshooting Gestures
- **Gestures not working**: Check if Accessibility Service is enabled
- **Voice commands not responding**: Verify microphone permission and background app restrictions
- **Cursor too sensitive**: Decrease sensitivity or try calibration
- **Head swipes not triggering**: Ensure mouth is open and make larger head movements

## 🏗️ Architecture

### Key Components

- **MainActivity**: Main UI, camera setup, gesture coordination
- **FaceLandmarkerHelper**: MediaPipe face detection wrapper  
- **HeadPoseDetector**: Hybrid head pose estimation (landmarks + TFLite)
- **GazeAccessibilityService**: System-wide gesture execution
- **VoiceCommandService**: Background voice recognition
- **OverlayView**: Real-time landmark and cursor rendering
- **EyeGazeCalculator**: Eye gaze estimation from landmarks

### Dependencies

```kotlin
// Camera & UI
implementation "androidx.camera:camera-camera2:1.3.1"
implementation "androidx.camera:camera-lifecycle:1.3.1"
implementation "androidx.camera:camera-view:1.3.1"

// MediaPipe
implementation "com.google.mediapipe:tasks-vision:0.10.9"

// TensorFlow Lite (optional)
implementation "org.tensorflow:tensorflow-lite:2.14.0"
implementation "org.tensorflow:tensorflow-lite-api:2.14.0"
implementation "org.tensorflow:tensorflow-lite-support:0.4.4"
```

## 🔧 Configuration

### Sensitivity Tuning
- **Eye Sensitivity**: 3.0-5.0 (higher = more sensitive)
- **Head Movement Threshold**: 0.05 (normalized coordinates)
- **Swipe Cooldown**: 500ms between gestures
- **Mouth Open Threshold**: 0.02 (lip distance)

### Model Configuration
The app supports hybrid head pose detection:
- **Landmark-only mode**: Always available, uses MediaPipe face mesh
- **Hybrid mode**: Adds TFLite model for improved accuracy (optional)
- Model input: 224x224 RGB images
- Model output: 5 classes (center, left, right, up, down)

## 🐛 Troubleshooting

### Common Issues

1. **"Accessibility Service not enabled"**
   - Go to Android Settings → Accessibility → Face Mesh App → Enable

2. **"Voice commands not working"**
   - Check microphone permission
   - Disable battery optimization for the app
   - Ensure voice commands toggle is on

3. **"Gestures not performing system actions"**
   - Verify Accessibility Service is enabled and running
   - Check that the service has necessary permissions

4. **"App crashes on startup"**
   - Ensure camera permission is granted
   - Check Android version compatibility (API 24+)

5. **"Head model not loading"**
   - The app works fine without the TFLite model (landmark fallback)
   - To add model: place `head_pose_model.tflite` in `app/src/main/assets/`

### Debug Information
- Enable verbose logging in `GazeAccessibilityService` and `VoiceCommandService`
- Check logcat for gesture execution results
- Head pose detection shows confidence scores and source (model/landmark/hybrid)

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

## 📞 Support

For issues and questions:
- Create an [issue](https://github.com/UdayBhoyar/FACE_MESH_APP/issues) on GitHub
- Check existing documentation in the `/docs` folder

## 🙏 Acknowledgments

- **MediaPipe**: Google's framework for building perception pipelines
- **TensorFlow Lite**: On-device machine learning inference
- **CameraX**: Android's camera development library
- **Android Accessibility Service**: For system-wide gesture support

---

**Note**: This app requires specific permissions and accessibility settings to function properly. Please follow the setup instructions carefully for the best experience.