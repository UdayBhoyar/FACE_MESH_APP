package com.example.face_mesh_app

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.media.MediaScannerConnection
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import androidx.core.app.NotificationCompat
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class VoiceCommandService : Service() {
    
    companion object {
        const val ACTION_STOP_VOICE = "com.example.face_mesh_app.ACTION_STOP_VOICE"
        const val ACTION_START_LISTENING = "com.example.face_mesh_app.ACTION_START_LISTENING"
        private const val TAG = "VoiceCommandService"
        private const val NOTIFICATION_ID = 2
        private const val CHANNEL_ID = "voice_command_channel"
    }

    private var speechRecognizer: SpeechRecognizer? = null
    private var isListening = false
    private val handler = Handler(Looper.getMainLooper())
    private val restartDelay = 1000L // 1 second delay before restarting

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "VoiceCommandService created")
        startAsForeground()
        initializeSpeechRecognizer()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP_VOICE -> {
                stopSelf()
                return START_NOT_STICKY
            }
            ACTION_START_LISTENING -> {
                startListening()
            }
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun startAsForeground() {
        createNotificationChannel()

        val stopIntent = Intent(this, VoiceCommandService::class.java).apply {
            action = ACTION_STOP_VOICE
        }
        val stopPending = PendingIntent.getService(
            this,
            0,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or
                    (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
        )

        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val openAppPending = PendingIntent.getActivity(
            this,
            1,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or
                    (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("🎤 Voice Commands Active")
            .setContentText("Say 'Open [app]', 'Home', 'Screenshot' • Tap for help")
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setOngoing(true)
            .setContentIntent(openAppPending)
            .addAction(android.R.drawable.ic_delete, "Stop", stopPending)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setStyle(NotificationCompat.BigTextStyle()
                .bigText("🎤 Listening for voice commands\n\n" +
                        "Try: 'Open WhatsApp', 'Camera', 'Home', 'Take Screenshot'\n\n" +
                        "Tap notification to see all commands"))
            .build()

        startForeground(NOTIFICATION_ID, notification)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Voice Commands",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Voice command recognition service"
            }
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun initializeSpeechRecognizer() {
        if (SpeechRecognizer.isRecognitionAvailable(this)) {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this).apply {
                setRecognitionListener(VoiceRecognitionListener())
            }
            startListening()
        } else {
            Log.e(TAG, "Speech recognition not available")
            stopSelf()
        }
    }

    private fun startListening() {
        if (isListening) return
        
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
        }

        try {
            speechRecognizer?.startListening(intent)
            isListening = true
            Log.d(TAG, "Started listening for voice commands")
        } catch (e: Exception) {
            Log.e(TAG, "Error starting speech recognition: ${e.message}")
            scheduleRestart()
        }
    }

    private fun stopListening() {
        try {
            speechRecognizer?.stopListening()
            isListening = false
            Log.d(TAG, "Stopped listening")
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping speech recognition: ${e.message}")
        }
    }

    private fun scheduleRestart() {
        handler.postDelayed({
            if (!isListening) {
                startListening()
            }
        }, restartDelay)
    }

    private inner class VoiceRecognitionListener : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) {
            Log.d(TAG, "Ready for speech")
        }

        override fun onBeginningOfSpeech() {
            Log.d(TAG, "Speech started")
        }

        override fun onRmsChanged(rmsdB: Float) {
            // Voice volume changed
        }

        override fun onBufferReceived(buffer: ByteArray?) {
            // Audio buffer received
        }

        override fun onEndOfSpeech() {
            Log.d(TAG, "Speech ended")
            isListening = false
        }

        override fun onError(error: Int) {
            val errorMessage = when (error) {
                SpeechRecognizer.ERROR_AUDIO -> "Audio recording error"
                SpeechRecognizer.ERROR_CLIENT -> "Client side error"
                SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Insufficient permissions"
                SpeechRecognizer.ERROR_NETWORK -> "Network error"
                SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network timeout"
                SpeechRecognizer.ERROR_NO_MATCH -> "No match found"
                SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Recognition service busy"
                SpeechRecognizer.ERROR_SERVER -> "Server error"
                SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech input"
                else -> "Unknown error: $error"
            }
            Log.e(TAG, "Speech recognition error: $errorMessage")
            isListening = false
            
            // Don't restart on no match or timeout errors, just restart listening
            if (error != SpeechRecognizer.ERROR_NO_MATCH && 
                error != SpeechRecognizer.ERROR_SPEECH_TIMEOUT) {
                scheduleRestart()
            } else {
                handler.postDelayed({ startListening() }, 500)
            }
        }

        override fun onResults(results: Bundle?) {
            val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            matches?.let { 
                if (it.isNotEmpty()) {
                    val command = it[0].lowercase(Locale.getDefault())
                    Log.d(TAG, "Recognized command: $command")
                    processVoiceCommand(command)
                }
            }
            
            // Restart listening after processing command
            handler.postDelayed({ startListening() }, 500)
        }

        override fun onPartialResults(partialResults: Bundle?) {
            // Partial results available
        }

        override fun onEvent(eventType: Int, params: Bundle?) {
            // Reserved for future events
        }
    }

    private fun processVoiceCommand(command: String) {
        Log.d(TAG, "=== Processing command: '$command' ===")
        
        // Show brief notification that command was recognized
        showCommandRecognizedNotification(command)
        
        // Also show toast for immediate feedback
        showToast("Command: $command")
        
        when {
            // Open specific apps - WhatsApp variations
            command.contains("whatsapp") || command.contains("whats app") -> {
                Log.d(TAG, "WhatsApp command detected")
                openApp("com.whatsapp")
            }
            // Camera
            command.contains("camera") -> {
                Log.d(TAG, "Camera command detected")
                openCamera()
            }
            // YouTube
            command.contains("youtube") || command.contains("you tube") -> {
                openApp("com.google.android.youtube")
            }
            // Gmail
            command.contains("gmail") || command.contains("email") || command.contains("mail") -> {
                openApp("com.google.android.gm")
            }
            // Chrome/Browser
            command.contains("chrome") || command.contains("browser") -> {
                openApp("com.android.chrome")
            }
            // Maps
            command.contains("map") || command.contains("maps") || command.contains("navigation") -> {
                openApp("com.google.android.apps.maps")
            }
            // Settings
            command.contains("setting") -> {
                openApp("com.android.settings")
            }
            // Phone/Dialer
            command.contains("phone") || command.contains("dialer") || command.contains("call") -> {
                openApp("com.android.dialer")
            }
            // Messages/SMS
            command.contains("message") || command.contains("sms") || command.contains("text") -> {
                openApp("com.android.messaging")
            }
            // Calculator
            command.contains("calculator") || command.contains("calculate") -> {
                openApp("com.android.calculator2")
            }
            // Calendar
            command.contains("calendar") -> {
                openApp("com.android.calendar")
            }
            // Clock/Alarm
            command.contains("clock") || command.contains("alarm") || command.contains("timer") -> {
                openApp("com.android.deskclock")
            }
            // Contacts
            command.contains("contact") -> {
                openApp("com.android.contacts")
            }
            // Generic open command
            command.contains("open") || command.contains("launch") || command.contains("start") -> {
                // Extract app name from command
                var appName = command
                    .replace("open", "")
                    .replace("launch", "")
                    .replace("start", "")
                    .replace("the", "")
                    .replace("app", "")
                    .trim()
                openAppByName(appName)
            }
            
            // System navigation
            command.contains("home") || command.contains("go home") || 
            command.contains("home screen") || command.contains("main screen") -> {
                goToHomeScreen()
            }
            command.contains("back") || command.contains("go back") || 
            command.contains("previous") -> {
                goBack()
            }
            command.contains("recent") || command.contains("recent apps") || 
            command.contains("app switcher") || command.contains("multitask") -> {
                showRecentApps()
            }
            command.contains("notification") -> {
                openNotifications()
            }
            
            // Screenshot
            command.contains("screenshot") || command.contains("screen shot") || 
            command.contains("take screenshot") || command.contains("capture screen") ||
            command.contains("screen capture") -> {
                takeScreenshot()
            }
            
            // Help/Info
            command.contains("help") || command.contains("what can you do") ||
            command.contains("command") -> {
                Log.d(TAG, "User requested help - show notification with available commands")
            }
            
            else -> {
                Log.d(TAG, "Command not recognized: $command")
            }
        }
    }

    private fun openApp(packageName: String) {
        try {
            Log.d(TAG, "Attempting to open app: $packageName")
            
            // Check if app is installed
            val installedApps = packageManager.getInstalledApplications(0)
            val isInstalled = installedApps.any { it.packageName == packageName }
            
            if (!isInstalled) {
                Log.e(TAG, "App not installed: $packageName")
                showToast("App not installed: $packageName")
                return
            }
            
            val intent = packageManager.getLaunchIntentForPackage(packageName)
            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                startActivity(intent)
                Log.d(TAG, "Successfully opened app: $packageName")
                showToast("Opening ${getAppName(packageName)}")
            } else {
                Log.e(TAG, "Could not get launch intent for: $packageName")
                showToast("Cannot open app: $packageName")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error opening app $packageName: ${e.message}", e)
            showToast("Error: ${e.message}")
        }
    }
    
    private fun getAppName(packageName: String): String {
        return try {
            val appInfo = packageManager.getApplicationInfo(packageName, 0)
            packageManager.getApplicationLabel(appInfo).toString()
        } catch (e: Exception) {
            packageName
        }
    }
    
    private fun showToast(message: String) {
        handler.post {
            android.widget.Toast.makeText(applicationContext, message, android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    private fun openCamera() {
        try {
            Log.d(TAG, "Attempting to open camera")
            val intent = Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            
            // Check if there's an app that can handle this intent
            if (intent.resolveActivity(packageManager) != null) {
                startActivity(intent)
                Log.d(TAG, "Successfully opened camera")
                showToast("Opening Camera")
            } else {
                Log.e(TAG, "No camera app found")
                showToast("No camera app available")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error opening camera: ${e.message}", e)
            showToast("Camera error: ${e.message}")
        }
    }

    private fun openAppByName(appName: String) {
        val packageName = VoiceCommandHelper.supportedApps[appName.lowercase()]
        if (packageName != null) {
            openApp(packageName)
        } else {
            // Try to search for app by name in installed apps
            val installedApps = packageManager.getInstalledApplications(PackageManager.GET_META_DATA)
            val matchedApp = installedApps.find { 
                packageManager.getApplicationLabel(it).toString().lowercase().contains(appName.lowercase())
            }
            
            if (matchedApp != null) {
                openApp(matchedApp.packageName)
            } else {
                Log.d(TAG, "App not found: $appName")
            }
        }
    }

    private fun goToHomeScreen() {
        try {
            Log.d(TAG, "Attempting to go to home screen")
            val intent = Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_HOME)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            
            if (intent.resolveActivity(packageManager) != null) {
                startActivity(intent)
                Log.d(TAG, "Successfully navigated to home screen")
                showToast("Going Home")
            } else {
                Log.e(TAG, "No home launcher found")
                showToast("Home navigation failed")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error going to home: ${e.message}", e)
            showToast("Home error: ${e.message}")
        }
    }

    private fun takeScreenshot() {
        // Use accessibility service to take screenshot
        GazeAccessibilityService.takeScreenshot()
        Log.d(TAG, "Screenshot command sent")
    }

    private fun goBack() {
        GazeAccessibilityService.performBack()
        Log.d(TAG, "Back navigation triggered")
    }

    private fun showRecentApps() {
        GazeAccessibilityService.performRecents()
        Log.d(TAG, "Recent apps triggered")
    }

    private fun openNotifications() {
        GazeAccessibilityService.performNotifications()
        Log.d(TAG, "Notifications triggered")
    }

    private fun showCommandRecognizedNotification(command: String) {
        // Briefly update notification to show recognized command
        handler.postDelayed({
            val notification = NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("✓ Command Recognized")
                .setContentText("Executing: '$command'")
                .setSmallIcon(android.R.drawable.ic_btn_speak_now)
                .setOngoing(true)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build()
            
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.notify(NOTIFICATION_ID, notification)
            
            // Restore normal notification after 2 seconds
            handler.postDelayed({
                startAsForeground()
            }, 2000)
        }, 100)
    }

    override fun onDestroy() {
        super.onDestroy()
        stopListening()
        speechRecognizer?.destroy()
        handler.removeCallbacksAndMessages(null)
        Log.d(TAG, "VoiceCommandService destroyed")
    }
}
