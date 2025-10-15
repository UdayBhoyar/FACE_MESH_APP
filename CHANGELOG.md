# Changelog - Face Mesh App

## [Version 1.1.0] - Voice Commands Feature

### 🎉 New Features

#### Voice Commands System
- **Background Voice Recognition**: Continuous voice command listening even when app is minimized
- **20+ Pre-configured Apps**: Quick access to popular apps like WhatsApp, YouTube, Gmail, Camera, etc.
- **Dynamic App Opening**: Open any installed app by speaking its name
- **System Navigation**: Voice control for Home, Back, Recent Apps, and Notifications
- **Screenshot Capture**: Take screenshots using voice commands (Android 9+)
- **Smart Command Processing**: Natural language support with multiple command aliases
- **Persistent Notification**: Visual feedback and easy access to stop voice commands
- **In-App Help**: Long-press Voice Commands button for comprehensive guide
- **First-Use Tutorial**: Automatic help dialog on first activation

### 🎨 UI Changes
- Added "Voice Commands" toggle button in top-right controls
- Toggle states: "Voice OFF" / "Voice ON"
- Minimal UI impact - maintains existing layout
- Help accessible via long-press on toggle

### 🔧 Technical Improvements
- New foreground service for reliable background operation
- Enhanced accessibility service with screenshot capability
- Improved permission handling for microphone access
- State persistence for voice command preferences
- Smart app package resolution
- Continuous listening with automatic restart
- Error handling and recovery mechanisms

### 📱 Supported Voice Commands

#### App Commands (20+ apps)
- WhatsApp, YouTube, Gmail, Camera, Chrome, Maps
- Instagram, Facebook, Messenger, Twitter, Telegram
- Spotify, Netflix, Phone, Messages, Calculator
- Calendar, Clock, Contacts, Settings, Play Store
- Any installed app by name

#### Navigation Commands
- "Home" - Return to home screen
- "Back" - Go to previous screen
- "Recent Apps" - Show multitasking view
- "Notifications" - Open notification shade

#### Action Commands
- "Take Screenshot" - Capture screen (Android 9+)
- "Screen Capture" - Alternative screenshot command

### 🔐 New Permissions
- `RECORD_AUDIO` - Required for voice command listening
- `FOREGROUND_SERVICE_MICROPHONE` - Background voice service
- `QUERY_ALL_PACKAGES` - Find and launch installed apps

### 📚 Documentation Added
- **VOICE_COMMANDS_README.md** - Comprehensive user guide
- **VOICE_COMMANDS_QUICK_GUIDE.txt** - Quick reference card
- **IMPLEMENTATION_SUMMARY.md** - Technical documentation
- In-app help dialog with all commands

### 🐛 Bug Fixes
- None (new feature)

### ⚡ Performance
- Efficient background service design
- Minimal battery impact
- Optimized speech recognition restart logic
- Smart notification updates

### 🔒 Privacy
- All voice processing done locally on device
- No voice data transmitted or stored
- Uses Android's built-in speech recognizer
- Transparent permission usage

### 🎯 Compatibility
- Minimum Android version: 7.0 (API 24) - unchanged
- Screenshot feature: Android 9.0+ (API 28)
- Works with all existing features
- No conflicts with eye tracking or overlay

### 📝 Notes
- Voice commands work best in quiet environments
- Accessibility service must be enabled for screenshots
- Some system actions require appropriate permissions
- App names should match installed app labels

---

## [Version 1.0.0] - Initial Release

### Features
- Real-time eye gaze tracking using MediaPipe Face Mesh
- Mouth-open gesture for clicking/tapping
- System-wide overlay for hands-free navigation
- 16-point calibration system
- Adjustable sensitivity controls (3.0 - 5.0)
- Head movement detection for swipe gestures
- Smooth cursor movement with alpha smoothing
- Background operation via foreground service
- Accessibility service integration
- CameraX integration for reliable camera access

### Core Components
- Face landmark detection (478 points)
- Iris tracking for precise gaze calculation
- Real-time cursor rendering
- Gesture recognition (mouth open/close)
- System-level tap simulation
- Overlay management

### Permissions
- Camera access
- System alert window (overlay)
- Accessibility service binding
- Foreground service

---

## Future Roadmap

### Planned Features
- [ ] Custom voice command phrases
- [ ] Voice feedback/confirmation
- [ ] Multi-language voice support
- [ ] Music playback voice controls
- [ ] Volume adjustment via voice
- [ ] Enhanced gesture library
- [ ] Voice command macros
- [ ] Offline voice recognition
- [ ] Command history and analytics
- [ ] Cloud sync for settings (optional)

### Under Consideration
- [ ] Voice training for personalization
- [ ] Custom wake word
- [ ] Hands-free mode toggle via voice
- [ ] Integration with smart home devices
- [ ] Wear OS companion app
- [ ] Widget for quick toggles
- [ ] Tasker/automation integration

---

## Version History

| Version | Release Date | Key Feature |
|---------|-------------|-------------|
| 1.1.0 | TBD | Voice Commands |
| 1.0.0 | Previous | Eye Tracking & Gestures |

---

## Credits

### Technologies Used
- MediaPipe Face Mesh (Google)
- Android SpeechRecognizer API
- CameraX (Google)
- Android Accessibility Services
- Kotlin Coroutines

### Open Source Libraries
- MediaPipe Tasks Vision: 0.10.9
- CameraX: 1.3.1
- AndroidX Core KTX: 1.12.0
- Material Components: 1.11.0

---

## Support & Feedback

For issues, suggestions, or feature requests:
- GitHub Issues (if applicable)
- In-app feedback (if implemented)
- Documentation: See README files

---

**Last Updated**: October 15, 2025
**Maintainer**: Face Mesh App Team
**License**: [Your License]
