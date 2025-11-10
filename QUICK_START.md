# 🚀 QUICK START - Hybrid Head Pose Detection

## Convert Your Model (30 seconds)

1. **Place your Keras model** in project root:
   ```
   Face_Mesh_App/best_model.keras
   ```

2. **Run conversion**:
   ```powershell
   cd C:\Users\Asus\OneDrive\Desktop\Code\Face_Mesh_App
   python convert_model.py
   ```

3. **Verify success**:
   ```
   ✅ Conversion successful!
   TFLite model saved to: app/src/main/assets/head_pose_model.tflite
   ```

## Build & Install (1 minute)

```powershell
# Build
.\gradlew.bat assembleDebug

# Install
adb install -r app\build\outputs\apk\debug\app-debug.apk
```

## Test (30 seconds)

1. Open app
2. Move head left/right/up/down
3. Watch for Toast: "Head: left (87%) [hybrid]"
4. Check logs: `adb logcat | Select-String "HeadPose"`

## What You'll See

**With Model**:
- Toast: "Head: left (87%) [hybrid]"
- High confidence predictions
- Smooth transitions

**Without Model** (fallback):
- Toast: "Head: left [landmark]"
- Still works, slightly less accurate

## Troubleshooting

**No model loaded?**
```powershell
# Check if file exists
ls app\src\main\assets\head_pose_model.tflite

# Re-run conversion
python convert_model.py
```

**Python error?**
```powershell
pip install tensorflow numpy
```

## That's It!

✅ Hybrid system implemented  
✅ Model + landmark fusion  
✅ Automatic fallback  
✅ Build successful  

**Full documentation**: See `HYBRID_HEAD_POSE_README.md`
