# Voice Commands Feature - Testing Checklist

## Pre-Testing Setup
- [ ] Build and install the updated app
- [ ] Ensure device has microphone
- [ ] Ensure Android version is 7.0 or higher
- [ ] Have test apps installed (WhatsApp, YouTube, etc.)
- [ ] Quiet testing environment recommended

---

## Phase 1: Basic Functionality

### Initial Setup
- [ ] App launches successfully
- [ ] All existing features still work
- [ ] New "Voice OFF" toggle button is visible
- [ ] Toggle button positioned correctly (top-right)
- [ ] No UI layout issues or overlaps

### Permission Handling
- [ ] Tap "Voice OFF" button
- [ ] Microphone permission dialog appears
- [ ] Grant permission successfully
- [ ] Service starts after permission granted
- [ ] Toast message "Voice Commands Enabled" appears
- [ ] Toggle changes to "Voice ON"

### Notification
- [ ] Pull down notification shade
- [ ] "Voice Commands Active" notification visible
- [ ] Notification shows microphone icon
- [ ] "Stop" button visible in notification
- [ ] Tapping notification opens app
- [ ] Notification is persistent (ongoing)

---

## Phase 2: Voice Command Testing

### App Opening Commands

#### Pre-configured Apps
- [ ] "Open WhatsApp" → Opens WhatsApp
- [ ] "WhatsApp" (short form) → Opens WhatsApp
- [ ] "Open Camera" → Opens Camera app
- [ ] "Camera" → Opens Camera app
- [ ] "Open YouTube" → Opens YouTube
- [ ] "Open Gmail" → Opens Gmail
- [ ] "Email" → Opens Gmail
- [ ] "Open Chrome" → Opens Chrome
- [ ] "Browser" → Opens Chrome
- [ ] "Open Maps" → Opens Google Maps
- [ ] "Navigation" → Opens Google Maps
- [ ] "Open Settings" → Opens Settings app

#### Popular Apps (if installed)
- [ ] "Open Instagram" → Opens Instagram
- [ ] "Open Facebook" → Opens Facebook
- [ ] "Open Messenger" → Opens Messenger
- [ ] "Open Twitter" → Opens Twitter
- [ ] "Open Telegram" → Opens Telegram
- [ ] "Open Spotify" → Opens Spotify
- [ ] "Open Netflix" → Opens Netflix

#### System Apps
- [ ] "Open Phone" → Opens Phone/Dialer
- [ ] "Call" → Opens Phone/Dialer
- [ ] "Open Messages" → Opens SMS app
- [ ] "SMS" → Opens SMS app
- [ ] "Open Calculator" → Opens Calculator
- [ ] "Open Calendar" → Opens Calendar
- [ ] "Open Clock" → Opens Clock app
- [ ] "Alarm" → Opens Clock app
- [ ] "Open Contacts" → Opens Contacts

#### Generic Commands
- [ ] "Open [installed app name]" → Opens that app
- [ ] "Launch [app name]" → Opens that app
- [ ] "Start [app name]" → Opens that app

### Navigation Commands

#### Home Navigation
- [ ] "Home" → Returns to home screen
- [ ] "Home Screen" → Returns to home screen
- [ ] "Go Home" → Returns to home screen
- [ ] "Main Screen" → Returns to home screen

#### Back Navigation
- [ ] "Back" → Goes to previous screen
- [ ] "Go Back" → Goes to previous screen
- [ ] "Previous" → Goes to previous screen

#### Recent Apps
- [ ] "Recent Apps" → Shows recent apps
- [ ] "Recents" → Shows recent apps
- [ ] "Multitask" → Shows recent apps
- [ ] "App Switcher" → Shows recent apps

#### Notifications
- [ ] "Notifications" → Opens notification shade

### Action Commands

#### Screenshot (Android 9+ only)
- [ ] "Take Screenshot" → Takes screenshot
- [ ] "Screenshot" → Takes screenshot
- [ ] "Screen Capture" → Takes screenshot
- [ ] "Capture Screen" → Takes screenshot
- [ ] Screenshot saved to gallery (verify)

---

## Phase 3: Background Operation

### Background Listening
- [ ] Minimize the app (press Home)
- [ ] Notification still visible
- [ ] Say "Open WhatsApp" → App opens
- [ ] Say "Home" → Returns to home screen
- [ ] Open another app (e.g., Chrome)
- [ ] Say "Open Camera" → Camera opens
- [ ] Voice commands work while using other apps

### Service Persistence
- [ ] Lock screen
- [ ] Unlock screen
- [ ] Notification still visible
- [ ] Voice commands still work
- [ ] Close app via recent apps
- [ ] Voice service still running (notification visible)
- [ ] Voice commands still work

---

## Phase 4: Help & Documentation

### In-App Help
- [ ] Long-press "Voice Commands" toggle
- [ ] Help dialog appears
- [ ] All commands listed in dialog
- [ ] Dialog dismisses with "Got it" button
- [ ] Dialog readable and formatted correctly

### First-Use Experience
- [ ] Uninstall and reinstall app (or clear app data)
- [ ] Enable voice commands for first time
- [ ] Help dialog automatically appears
- [ ] Dialog shown only once
- [ ] Subsequent toggles don't show dialog (unless long-press)

---

## Phase 5: Stopping Voice Commands

### Via Toggle
- [ ] Open the app
- [ ] Toggle "Voice ON" to "Voice OFF"
- [ ] Service stops
- [ ] Notification disappears
- [ ] Toast "Voice Commands Disabled" appears
- [ ] Voice commands no longer work

### Via Notification
- [ ] Enable voice commands
- [ ] Pull down notification shade
- [ ] Tap "Stop" button in notification
- [ ] Service stops
- [ ] Notification disappears
- [ ] App still shows "Voice ON" (re-sync expected)

---

## Phase 6: Edge Cases & Error Handling

### Permission Denied
- [ ] Deny microphone permission
- [ ] Toggle stays at "Voice OFF"
- [ ] Toast message about permission requirement
- [ ] No crash or error

### No Speech Recognition
- [ ] Test on emulator (may not have speech recognition)
- [ ] Service handles gracefully
- [ ] No crash
- [ ] Appropriate error logging

### Unrecognized Commands
- [ ] Say gibberish or nonsense
- [ ] Service continues listening
- [ ] No crash
- [ ] No false command execution

### Command Variations
- [ ] Say commands with different volumes
- [ ] Say commands quickly
- [ ] Say commands slowly
- [ ] Say commands with pauses
- [ ] Test with background noise (if possible)

### App Not Installed
- [ ] Say "Open [non-existent app]"
- [ ] No crash
- [ ] Service continues
- [ ] Next command still works

### Rapid Commands
- [ ] Say multiple commands quickly
- [ ] "Open WhatsApp" → "Back" → "Home" → "Camera"
- [ ] Commands execute in order
- [ ] No crashes or hangs

---

## Phase 7: Integration with Existing Features

### With Eye Tracking
- [ ] Enable voice commands
- [ ] Eye tracking cursor still works
- [ ] Both features work simultaneously
- [ ] No performance issues

### With Overlay
- [ ] Enable overlay service
- [ ] Enable voice commands
- [ ] Both services run together
- [ ] Voice commands work with overlay active
- [ ] No camera conflicts

### With Accessibility Service
- [ ] Ensure accessibility service enabled
- [ ] Voice commands work properly
- [ ] Screenshot command works (Android 9+)
- [ ] Tap simulation works
- [ ] No conflicts

### With Calibration
- [ ] Start calibration
- [ ] Voice commands still work
- [ ] Can say "Home" to exit calibration (if needed)
- [ ] No interference

---

## Phase 8: State Persistence

### App Restart
- [ ] Enable voice commands
- [ ] Close app completely
- [ ] Reopen app
- [ ] Toggle state preserved (visual only, service stopped)
- [ ] User can re-enable easily

### After Reboot
- [ ] Enable voice commands
- [ ] Reboot device
- [ ] Open app
- [ ] Voice commands not auto-started (expected)
- [ ] User can enable manually

---

## Phase 9: Performance & Battery

### CPU Usage
- [ ] Enable voice commands
- [ ] Monitor CPU usage (via developer options)
- [ ] Usage is reasonable (<10% average)
- [ ] No excessive heating

### Battery Drain
- [ ] Enable voice commands
- [ ] Let run for 1 hour
- [ ] Check battery usage in settings
- [ ] Drain is acceptable (<5% per hour)

### Memory Usage
- [ ] Enable voice commands
- [ ] Monitor memory usage
- [ ] No memory leaks over time
- [ ] Service uses reasonable memory (<50MB)

---

## Phase 10: Accessibility & Usability

### Command Recognition
- [ ] Commands recognized accurately (>80%)
- [ ] Common apps work reliably
- [ ] Navigation commands reliable
- [ ] Minimal false positives

### Response Time
- [ ] Commands execute within 1-2 seconds
- [ ] No significant delay
- [ ] Notification updates promptly
- [ ] Apps launch quickly

### User Feedback
- [ ] Notification updates when command recognized
- [ ] Clear indication of service status
- [ ] Easy to understand what's happening
- [ ] Help is accessible

---

## Phase 11: Multi-Language (Future)
- [ ] Test with device in different language
- [ ] English commands still work
- [ ] No crashes with non-English system language

---

## Known Limitations to Document

### Expected Behaviors
- [ ] Screenshot requires Android 9+
- [ ] Some apps may not open if not installed
- [ ] Background noise affects recognition
- [ ] Works best in quiet environments
- [ ] Service doesn't auto-start after reboot
- [ ] Continuous listening uses microphone (expected)

### Not Bugs
- [ ] Slight delay in command recognition (speech processing time)
- [ ] Occasional misrecognition (speech recognition limitation)
- [ ] Service stops when app is force-closed (expected)
- [ ] Commands don't work with screen off (expected)

---

## Bug Reporting Template

If issues found:
```
Bug Title: [Brief description]

Steps to Reproduce:
1. [Step 1]
2. [Step 2]
3. [Step 3]

Expected Behavior:
[What should happen]

Actual Behavior:
[What actually happens]

Device Info:
- Device Model: [e.g., Pixel 6]
- Android Version: [e.g., Android 13]
- App Version: [e.g., 1.1.0]

Logs:
[Relevant logcat output if available]

Screenshots:
[If applicable]
```

---

## Testing Sign-Off

| Test Phase | Status | Tester | Date | Notes |
|------------|--------|--------|------|-------|
| Phase 1: Basic Functionality | ⬜ Pass / ⬜ Fail | | | |
| Phase 2: Voice Commands | ⬜ Pass / ⬜ Fail | | | |
| Phase 3: Background Operation | ⬜ Pass / ⬜ Fail | | | |
| Phase 4: Help & Documentation | ⬜ Pass / ⬜ Fail | | | |
| Phase 5: Stopping Commands | ⬜ Pass / ⬜ Fail | | | |
| Phase 6: Edge Cases | ⬜ Pass / ⬜ Fail | | | |
| Phase 7: Integration | ⬜ Pass / ⬜ Fail | | | |
| Phase 8: State Persistence | ⬜ Pass / ⬜ Fail | | | |
| Phase 9: Performance | ⬜ Pass / ⬜ Fail | | | |
| Phase 10: Usability | ⬜ Pass / ⬜ Fail | | | |

**Overall Status**: ⬜ Ready for Release / ⬜ Needs Work

**Tester Signature**: ___________________ **Date**: ___________

---

## Post-Testing Tasks

- [ ] Update changelog with any fixes
- [ ] Update documentation with known issues
- [ ] Create user tutorial video (optional)
- [ ] Prepare release notes
- [ ] Test on multiple devices (if possible)
- [ ] Get user feedback from beta testers

---

**Testing Version**: 1.1.0
**Last Updated**: October 15, 2025
