"""
Convert Keras model to TensorFlow Lite format for Android integration
Place your best_model.keras file in the same directory as this script
"""
import tensorflow as tf
import numpy as np
import os

def convert_keras_to_tflite(keras_model_path, tflite_output_path):
    """
    Convert Keras model to TFLite format with optimization
    """
    try:
        # Load the Keras model
        print(f"Loading Keras model from: {keras_model_path}")
        model = tf.keras.models.load_model(keras_model_path)
        
        # Print model summary
        print("\nModel Summary:")
        model.summary()
        
        # Get input shape
        input_shape = model.input_shape
        print(f"\nInput shape: {input_shape}")
        
        # Convert to TFLite
        print("\nConverting to TFLite...")
        converter = tf.lite.TFLiteConverter.from_keras_model(model)
        
        # Apply optimizations
        converter.optimizations = [tf.lite.Optimize.DEFAULT]
        
        # Set target spec for better compatibility
        converter.target_spec.supported_ops = [
            tf.lite.OpsSet.TFLITE_BUILTINS,
            tf.lite.OpsSet.SELECT_TF_OPS
        ]
        
        # Convert the model
        tflite_model = converter.convert()
        
        # Save the model
        print(f"Saving TFLite model to: {tflite_output_path}")
        with open(tflite_output_path, 'wb') as f:
            f.write(tflite_model)
        
        # Get file sizes
        keras_size = os.path.getsize(keras_model_path) / (1024 * 1024)
        tflite_size = os.path.getsize(tflite_output_path) / (1024 * 1024)
        
        print(f"\n✅ Conversion successful!")
        print(f"Keras model size: {keras_size:.2f} MB")
        print(f"TFLite model size: {tflite_size:.2f} MB")
        print(f"Size reduction: {((keras_size - tflite_size) / keras_size * 100):.1f}%")
        
        # Test the TFLite model
        print("\nTesting TFLite model...")
        test_tflite_model(tflite_output_path, input_shape)
        
        return True
        
    except Exception as e:
        print(f"❌ Error during conversion: {e}")
        import traceback
        traceback.print_exc()
        return False

def test_tflite_model(tflite_path, input_shape):
    """
    Test the TFLite model with random input
    """
    try:
        # Load TFLite model
        interpreter = tf.lite.Interpreter(model_path=tflite_path)
        interpreter.allocate_tensors()
        
        # Get input and output details
        input_details = interpreter.get_input_details()
        output_details = interpreter.get_output_details()
        
        print(f"Input details: {input_details[0]}")
        print(f"Output details: {output_details[0]}")
        
        # Create random test input
        if len(input_shape) == 4:
            test_input = np.random.rand(1, input_shape[1], input_shape[2], input_shape[3]).astype(np.float32)
        else:
            test_input = np.random.rand(*input_shape).astype(np.float32)
        
        # Run inference
        interpreter.set_tensor(input_details[0]['index'], test_input)
        interpreter.invoke()
        output = interpreter.get_tensor(output_details[0]['index'])
        
        print(f"Test output shape: {output.shape}")
        print(f"Test output (sample): {output[0]}")
        print("✅ TFLite model test successful!")
        
    except Exception as e:
        print(f"⚠️ Warning: Could not test TFLite model: {e}")

def main():
    # Define paths
    script_dir = os.path.dirname(os.path.abspath(__file__))
    keras_model_path = os.path.join(script_dir, "best_model.keras")
    tflite_output_path = os.path.join(script_dir, "app", "src", "main", "assets", "head_pose_model.tflite")
    
    # Check if Keras model exists
    if not os.path.exists(keras_model_path):
        print(f"❌ Error: Keras model not found at: {keras_model_path}")
        print("\n📋 Instructions:")
        print("1. Place your 'best_model.keras' file in the project root directory")
        print(f"   Expected location: {keras_model_path}")
        print("2. Run this script again: python convert_model.py")
        return
    
    # Create assets directory if it doesn't exist
    assets_dir = os.path.dirname(tflite_output_path)
    os.makedirs(assets_dir, exist_ok=True)
    print(f"Assets directory: {assets_dir}")
    
    # Convert the model
    success = convert_keras_to_tflite(keras_model_path, tflite_output_path)
    
    if success:
        print("\n" + "="*60)
        print("✅ MODEL CONVERSION COMPLETE!")
        print("="*60)
        print(f"\n📁 TFLite model saved to:")
        print(f"   {tflite_output_path}")
        print("\n📋 Next steps:")
        print("1. The model has been automatically placed in app/src/main/assets/")
        print("2. Build the Android app: .\\gradlew.bat assembleDebug")
        print("3. Install and test on device")
        print("\n💡 The app will now use a hybrid approach:")
        print("   - TFLite model for accurate head pose classification")
        print("   - MediaPipe landmarks for real-time tracking")
        print("="*60)
    else:
        print("\n❌ Conversion failed. Please check the error messages above.")

if __name__ == "__main__":
    main()
