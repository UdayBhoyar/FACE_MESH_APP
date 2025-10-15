package com.example.face_mesh_app

import android.app.AlertDialog
import android.content.Context

object VoiceCommandHelper {
    
    fun showVoiceCommandsHelp(context: Context) {
        val helpText = """
            |Voice Commands Available:
            |
            |📱 OPEN APPS:
            |  • "Open WhatsApp"
            |  • "Open Camera"
            |  • "Open YouTube"
            |  • "Open Gmail"
            |  • "Open Chrome"
            |  • "Open Maps"
            |  • "Open Instagram"
            |  • "Open Facebook"
            |  • "Open Settings"
            |  • "Open [any app name]"
            |
            |🏠 NAVIGATION:
            |  • "Home" or "Home Screen"
            |  • "Go Back" or "Back"
            |  • "Recent Apps" or "Recents"
            |  • "Notifications"
            |
            |📸 ACTIONS:
            |  • "Take Screenshot"
            |  • "Capture Screen"
            |
            |💡 TIPS:
            |  • Speak clearly and naturally
            |  • Commands work in background
            |  • Use notification to stop voice commands
            |  • Requires Android 9+ for screenshots
        """.trimMargin()
        
        AlertDialog.Builder(context)
            .setTitle("Voice Commands Guide")
            .setMessage(helpText)
            .setPositiveButton("Got it") { dialog, _ -> dialog.dismiss() }
            .setIcon(android.R.drawable.ic_btn_speak_now)
            .show()
    }
    
    val supportedApps = mapOf(
        "whatsapp" to "com.whatsapp",
        "youtube" to "com.google.android.youtube",
        "gmail" to "com.google.android.gm",
        "chrome" to "com.android.chrome",
        "maps" to "com.google.android.apps.maps",
        "play store" to "com.android.vending",
        "instagram" to "com.instagram.android",
        "facebook" to "com.facebook.katana",
        "messenger" to "com.facebook.orca",
        "twitter" to "com.twitter.android",
        "telegram" to "org.telegram.messenger",
        "spotify" to "com.spotify.music",
        "netflix" to "com.netflix.mediaclient",
        "amazon" to "com.amazon.mShop.android.shopping",
        "settings" to "com.android.settings",
        "calculator" to "com.android.calculator2",
        "calendar" to "com.android.calendar",
        "clock" to "com.android.deskclock",
        "contacts" to "com.android.contacts",
        "phone" to "com.android.dialer",
        "messages" to "com.android.messaging"
    )
}
