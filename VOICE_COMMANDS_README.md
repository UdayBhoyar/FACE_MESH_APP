# Voice Commands Feature - Face Mesh App

## Overview
The Voice Commands feature allows you to control your Android device hands-free using voice commands while the app runs in the background.

## How to Enable

1. **In the App**: 
   - Open the Face Mesh App
   - Look for the "Voice OFF" toggle button in the top-right controls
   - Tap to enable voice commands (changes to "Voice ON")
   - Grant microphone permission when prompted

2. **Background Operation**:
   - Voice commands work even when the app is in the background
   - A persistent notification shows "Voice Commands Active"
   - Use the notification to stop voice commands anytime

3. **Requirements**:
   - Microphone permission
   - Accessibility service enabled (for some actions)
   - Android 9+ for screenshot functionality

## Available Voice Commands

### 📱 Open Applications

| Command | Action |
|---------|--------|
| "Open WhatsApp" / "WhatsApp" | Opens WhatsApp |
| "Open Camera" / "Camera" | Opens Camera app |
| "Open YouTube" | Opens YouTube |
| "Open Gmail" / "Email" | Opens Gmail |
| "Open Chrome" / "Browser" | Opens Chrome browser |
| "Open Maps" / "Navigation" | Opens Google Maps |
| "Open Settings" | Opens device Settings |
| "Open Phone" / "Call" | Opens Phone/Dialer |
| "Open Messages" / "SMS" | Opens Messages app |
| "Open Calculator" | Opens Calculator |
| "Open Calendar" | Opens Calendar |
| "Open Clock" / "Alarm" | Opens Clock/Alarm |
| "Open Contacts" | Opens Contacts |
| "Open Instagram" | Opens Instagram |
| "Open Facebook" | Opens Facebook |
| "Open [any app name]" | Attempts to open any installed app |

### 🏠 Navigation Commands

| Command | Action |
|---------|--------|
| "Home" / "Home Screen" | Returns to home screen |
| "Back" / "Go Back" | Goes back (previous screen) |
| "Recent Apps" / "Multitask" | Shows recent apps |
| "Notifications" | Opens notification shade |

### 📸 Action Commands

| Command | Action |
|---------|--------|
| "Take Screenshot" | Takes a screenshot (Android 9+) |
| "Screen Capture" | Takes a screenshot (Android 9+) |

## Tips for Best Results

1. **Speak Clearly**: Use a normal speaking voice, not too loud or soft
2. **Natural Language**: Commands work with natural variations (e.g., "Open WhatsApp" or just "WhatsApp")
3. **Background Mode**: Commands continue working when app is minimized
4. **Continuous Listening**: The service automatically restarts listening after each command
5. **Help**: Long-press the "Voice Commands" toggle to see the help dialog

## Troubleshooting

### Voice Commands Not Working
- Ensure microphone permission is granted
- Check if the service is running (notification should be visible)
- Restart the voice command service by toggling it off and on

### App Not Opening
- Verify the app is installed on your device
- Some apps may have different package names
- Try using the full app name

### Screenshot Not Working
- Requires Android 9 (Pie) or higher
- Accessibility service must be enabled
- Grant necessary permissions in Settings

## Privacy & Permissions

### Required Permissions
- **RECORD_AUDIO**: To listen for voice commands
- **FOREGROUND_SERVICE_MICROPHONE**: To run in background
- **QUERY_ALL_PACKAGES**: To search for installed apps

### Privacy Notes
- Voice recognition is handled by Android's built-in speech recognizer
- No voice data is stored or transmitted by this app
- Commands are processed locally on your device

## Stopping Voice Commands

### From the App
- Toggle the "Voice ON" button to "Voice OFF"

### From Notification
- Swipe down notification shade
- Find "Voice Commands Active" notification
- Tap "Stop Voice Commands" button

## Advanced Features

### Adding Custom Apps
The app supports opening any installed application by name. If the app isn't recognized by its common name, try:
- Using the exact app name as shown in your app drawer
- Opening the app manually once, then try the voice command again

### Command Aliases
Many commands have multiple aliases for natural interaction:
- "Home" = "Home Screen" = "Go Home"
- "Back" = "Go Back" = "Previous"
- "Screenshot" = "Screen Capture" = "Take Screenshot"

## Future Enhancements
Potential features being considered:
- Custom voice command phrases
- Gesture sequences via voice
- Volume control commands
- Music playback control
- Custom app shortcuts

## Feedback
If you encounter issues or have suggestions for new commands, please provide feedback through the app settings or repository issues.

---

**Note**: This feature works best in quiet environments. Background noise may affect recognition accuracy.
