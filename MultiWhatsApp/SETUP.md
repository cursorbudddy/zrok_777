# Quick Setup Guide

## 1. Configure the App

Update bridge bot ID in `app/src/main/java/com/multiwhatsapp/data/matrix/WhatsAppBridgeService.kt`:

```kotlin
private val bridgeBotId: String = "@whatsappbot:YOUR_DOMAIN.com"
```

Replace `YOUR_DOMAIN.com` with your actual Matrix homeserver domain.

## 2. Build the App

### Option A: Using Android Studio (Recommended)
1. Open Android Studio
2. Select "Open an Existing Project"
3. Navigate to the `MultiWhatsApp` folder
4. Wait for Gradle sync to complete
5. Click "Run" (Green play button) or Build > Build Bundle(s) / APK(s) > Build APK(s)

### Option B: Using Command Line
```bash
cd MultiWhatsApp

# Make gradlew executable (Linux/Mac)
chmod +x gradlew

# Build debug APK
./gradlew assembleDebug

# Build release APK (for production)
./gradlew assembleRelease
```

APK will be located at:
- Debug: `app/build/outputs/apk/debug/app-debug.apk`
- Release: `app/build/outputs/apk/release/app-release.apk`

## 3. Install on Device

### Via USB
```bash
adb install app/build/outputs/apk/debug/app-debug.apk
```

### Via Android Studio
Click the "Run" button with your device connected

### Via File Transfer
Transfer the APK to your phone and install manually (enable "Install from Unknown Sources")

## 4. First Run

1. **Enter Matrix Credentials**
   - Homeserver: `https://matrix.yourdomain.com`
   - Username: Your Matrix username
   - Password: Your Matrix password

2. **Link WhatsApp**
   - Wait for QR code to appear
   - Open WhatsApp > Settings > Linked Devices
   - Scan the QR code

3. **Start Messaging**
   - Your chats will appear automatically
   - Send and receive messages as normal

## Troubleshooting

### Gradle Sync Failed
```bash
# Update Gradle wrapper
./gradlew wrapper --gradle-version=8.2
```

### Build Failed - SDK Not Found
Install Android SDK via Android Studio or set ANDROID_HOME environment variable

### App Crashes on Startup
Check logcat for errors:
```bash
adb logcat | grep MultiWhatsApp
```

### Matrix Connection Issues
- Verify homeserver URL is correct and accessible
- Check SSL certificate is valid
- Ensure port 8448 (or your Matrix port) is open

### Bridge Not Responding
- Verify mautrix-whatsapp bridge is running
- Check bridge logs for errors
- Ensure bridge bot user exists in homeserver
- Verify app service registration is correct

## Testing Without Server

If you don't have a Matrix server yet:

1. Use a public Matrix homeserver (e.g., matrix.org)
2. Create a free account
3. Install mautrix-whatsapp on your local machine or VPS
4. Configure bridge to connect to the public homeserver

**Note**: This is only for testing. For production, always use your own homeserver.

## Next Steps

- Customize app colors in `app/src/main/res/values/colors.xml`
- Add your own app icon
- Configure push notifications
- Build release APK with signing key
- Publish to Google Play Store (if desired)

## Support

For detailed information, see the main [README.md](README.md)
