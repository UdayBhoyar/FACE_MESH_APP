# TensorFlow Lite Model Placeholder

## Instructions

Place your converted `head_pose_model.tflite` file in this directory.

### Quick Steps:

1. **Place your Keras model** in the project root:
   ```
   C:\Users\Asus\OneDrive\Desktop\Code\Face_Mesh_App\best_model.keras
   ```

2. **Run the conversion script**:
   ```powershell
   cd C:\Users\Asus\OneDrive\Desktop\Code\Face_Mesh_App
   python convert_model.py
   ```

3. **The script will automatically**:
   - Convert `best_model.keras` to TFLite format
   - Place the converted model in this directory as `head_pose_model.tflite`
   - Verify the conversion worked correctly

4. **Build and install the app**:
   ```powershell
   .\gradlew.bat clean assembleDebug
   adb install -r app\build\outputs\apk\debug\app-debug.apk
   ```

### Model Requirements:
- Input: 224x224x3 RGB image (normalized 0-1)
- Output: 5 class probabilities [center, left, right, up, down]

### Without the Model:
The app will work with landmark-based detection only (no model inference).
The hybrid system gracefully falls back to pure landmark tracking.
