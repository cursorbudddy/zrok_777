# MultiWhatsApp - Multi-Account WhatsApp Android Client

A native Android application that enables you to manage multiple WhatsApp accounts through the Matrix protocol and mautrix-whatsapp bridge.

## 📱 Features

- ✅ **Multiple WhatsApp Accounts**: Manage unlimited WhatsApp accounts in a single app
- ✅ **Native Android Experience**: Built with Jetpack Compose for modern, smooth UI
- ✅ **Real-time Sync**: Messages sync in real-time across all your accounts
- ✅ **Full WhatsApp Features**: Send/receive text, images, videos, files, and audio
- ✅ **Unified Chat View**: See all conversations from all accounts in one place
- ✅ **End-to-end Matrix Integration**: Powered by Matrix protocol for reliability
- ✅ **Account Switching**: Easily switch between different WhatsApp accounts
- ✅ **QR Code Authentication**: Simple linking process with WhatsApp

## 🏗️ Architecture

```
┌─────────────────────────────────────────┐
│     MultiWhatsApp Android App           │
│  (Jetpack Compose + Matrix SDK)         │
└──────────────┬──────────────────────────┘
               │ Matrix Protocol
┌──────────────▼──────────────────────────┐
│    Matrix Homeserver (Synapse)          │
└──────────────┬──────────────────────────┘
               │ Application Service API
┌──────────────▼──────────────────────────┐
│   mautrix-whatsapp Bridge               │
└──────────────┬──────────────────────────┘
               │ WhatsApp Web Protocol
┌──────────────▼──────────────────────────┐
│        WhatsApp Servers                  │
└──────────────────────────────────────────┘
```

## 📋 Prerequisites

### Server Side (Required)
1. **Matrix Homeserver** (Synapse, Dendrite, or Conduit)
   - Must be accessible via HTTPS
   - Application service support enabled

2. **mautrix-whatsapp Bridge**
   - Installed and configured
   - Connected to your Matrix homeserver
   - Bridge bot user created (e.g., `@whatsappbot:yourdomain.com`)

3. **PostgreSQL Database**
   - For mautrix-whatsapp bridge data storage

### Client Side
1. **Android Device**
   - Android 7.0 (API 24) or higher
   - Internet connection

2. **WhatsApp Mobile App**
   - To scan QR codes for linking accounts

## 🚀 Setup Instructions

### 1. Server Setup

#### Install Matrix Homeserver (Synapse)

```bash
# Install Synapse
pip install matrix-synapse

# Generate config
python -m synapse.app.homeserver \
  --server-name yourdomain.com \
  --config-path homeserver.yaml \
  --generate-config \
  --report-stats=no

# Start Synapse
synapse_homeserver -c homeserver.yaml
```

#### Install mautrix-whatsapp Bridge

```bash
# Clone repository
git clone https://github.com/mautrix/whatsapp.git
cd whatsapp

# Build
go build

# Generate config
./mautrix-whatsapp -g

# Edit config.yaml
nano config.yaml
```

**Important config.yaml settings:**
```yaml
homeserver:
    address: https://matrix.yourdomain.com
    domain: yourdomain.com

appservice:
    address: http://localhost:29318
    hostname: 0.0.0.0
    port: 29318
    id: whatsapp
    bot:
        username: whatsappbot
        displayname: WhatsApp Bridge Bot

bridge:
    username_template: "whatsapp_{{.}}"
    displayname_template: "{{.DisplayName}} (WhatsApp)"

database:
    type: postgres
    uri: postgres://user:password@localhost/mautrix_whatsapp?sslmode=disable
```

#### Register Bridge with Homeserver

```bash
# Generate registration file
./mautrix-whatsapp -g -r

# Add to Synapse homeserver.yaml
app_service_config_files:
  - /path/to/whatsapp-registration.yaml

# Restart Synapse
```

#### Start the Bridge

```bash
./mautrix-whatsapp
```

### 2. App Configuration

Before building the app, update the bridge bot ID in `WhatsAppBridgeService.kt`:

```kotlin
// Line 18 in WhatsAppBridgeService.kt
private val bridgeBotId: String = "@whatsappbot:yourdomain.com"
```

Replace `yourdomain.com` with your actual Matrix homeserver domain.

### 3. Build the App

```bash
cd MultiWhatsApp

# Build debug APK
./gradlew assembleDebug

# Or build release APK
./gradlew assembleRelease

# APK will be at: app/build/outputs/apk/debug/app-debug.apk
```

### 4. Install on Android

```bash
adb install app/build/outputs/apk/debug/app-debug.apk
```

Or open the project in Android Studio and run it directly.

## 📖 User Guide

### Adding Your First WhatsApp Account

1. **Open the App**
   - Launch MultiWhatsApp on your Android device

2. **Login to Matrix**
   - Enter your Matrix homeserver URL (e.g., `https://matrix.yourdomain.com`)
   - Enter your Matrix username
   - Enter your Matrix password
   - Tap "Login"

3. **Link WhatsApp**
   - After login, the app will automatically request a QR code from the bridge
   - Wait for the QR code to appear
   - Open WhatsApp on your phone
   - Go to **Settings > Linked Devices > Link a Device**
   - Scan the QR code shown in the app
   - Your WhatsApp account is now linked!

4. **Start Chatting**
   - Your WhatsApp chats will appear in the chat list
   - Tap any chat to open and start messaging

### Adding Additional WhatsApp Accounts

1. **Create Additional Matrix Accounts**
   - Each WhatsApp account requires a separate Matrix account
   - Register new accounts on your Matrix homeserver

2. **Add Account in App**
   - Tap the **+** button in the chat list
   - Login with the new Matrix account credentials
   - Link the new WhatsApp account via QR code

3. **Switch Between Accounts**
   - All chats from all accounts appear in the unified chat list
   - Each chat shows which account it belongs to

## 🔧 Technical Details

### Project Structure

```
MultiWhatsApp/
├── app/
│   ├── src/main/
│   │   ├── java/com/multiwhatsapp/
│   │   │   ├── data/
│   │   │   │   ├── matrix/          # Matrix SDK integration
│   │   │   │   │   ├── MatrixClientManager.kt
│   │   │   │   │   ├── MatrixAuthService.kt
│   │   │   │   │   └── WhatsAppBridgeService.kt
│   │   │   │   ├── models/          # Data models
│   │   │   │   │   ├── MatrixAccount.kt
│   │   │   │   │   ├── ChatRoom.kt
│   │   │   │   │   └── Message.kt
│   │   │   │   └── repository/      # Data repositories
│   │   │   │       ├── AccountRepository.kt
│   │   │   │       └── ChatRepository.kt
│   │   │   ├── ui/
│   │   │   │   ├── login/           # Login screen
│   │   │   │   ├── chat/            # Chat screens
│   │   │   │   └── theme/           # App theme
│   │   │   ├── MainActivity.kt
│   │   │   └── MultiWhatsAppApplication.kt
│   │   ├── res/                     # Resources
│   │   └── AndroidManifest.xml
│   └── build.gradle.kts
├── build.gradle.kts
├── settings.gradle.kts
└── README.md
```

### Key Technologies

- **Kotlin**: Modern, safe programming language
- **Jetpack Compose**: Declarative UI framework
- **Matrix Android SDK**: Official Matrix SDK for Android
- **Coroutines**: Asynchronous programming
- **Flow**: Reactive streams
- **DataStore**: Preferences storage
- **Coil**: Image loading
- **ZXing**: QR code handling

### How It Works

1. **Account Creation**: Each WhatsApp account links to a unique Matrix account
2. **Bridge Communication**: The app communicates with mautrix-whatsapp via Matrix protocol
3. **QR Code Flow**:
   - App sends "login" command to bridge bot
   - Bridge generates QR code and sends as image
   - User scans with WhatsApp mobile app
   - Bridge establishes WhatsApp Web connection
4. **Message Flow**:
   - WhatsApp → Bridge → Matrix → App
   - App → Matrix → Bridge → WhatsApp
5. **Portals**: Each WhatsApp chat becomes a Matrix "portal" room
6. **Ghosts**: Each WhatsApp contact becomes a Matrix "ghost" user

## ⚠️ Important Notes

### Security Considerations

- **NOT Official**: This is NOT an official WhatsApp client
- **Terms of Service**: May violate WhatsApp's ToS
- **Account Bans**: Risk of WhatsApp account suspension
- **Encryption**: Breaks WhatsApp's end-to-end encryption
- **Privacy**: Bridge server can see all messages
- **Self-Host**: Use your own infrastructure for privacy

### Limitations

- No phone calls or video calls
- No WhatsApp Stories (may work with some bridges)
- Media handling requires proper implementation
- Push notifications need FCM setup
- Battery usage may be higher due to constant sync

### Recommendations

- **Use for Testing**: Recommended for development/testing only
- **Burner Accounts**: Use with disposable WhatsApp accounts
- **Self-Host**: Host your own Matrix and bridge servers
- **Secure Connection**: Always use HTTPS for homeserver
- **Backup**: Regular backups of bridge database

## 🐛 Troubleshooting

### QR Code Not Appearing

1. Check bridge is running: `./mautrix-whatsapp`
2. Check bridge logs for errors
3. Verify bridge bot user exists in Matrix
4. Ensure app can reach homeserver

### Messages Not Syncing

1. Check Matrix sync is running
2. Verify bridge connection to WhatsApp
3. Restart bridge: `./mautrix-whatsapp`
4. Check network connectivity

### Login Failed

1. Verify homeserver URL is correct
2. Check username/password
3. Ensure homeserver is accessible
4. Check SSL certificate is valid

### Chats Not Loading

1. Verify bridge is running
2. Check WhatsApp is still linked
3. Re-scan QR code if needed
4. Check bridge database connection

## 📝 Configuration

### Custom Bridge Bot ID

Edit `WhatsAppBridgeService.kt`:
```kotlin
private val bridgeBotId: String = "@yourbotname:yourdomain.com"
```

### Custom Homeserver

Users enter this when logging in, but you can set a default in `LoginScreen.kt`:
```kotlin
var homeserverUrl by remember { mutableStateOf("https://matrix.yourdomain.com") }
```

## 🔮 Future Enhancements

- [ ] Push notifications via Firebase Cloud Messaging
- [ ] Media upload/download with progress
- [ ] Voice message recording
- [ ] Contact management
- [ ] Settings screen
- [ ] Account management UI
- [ ] Dark mode
- [ ] Message search
- [ ] Backup/restore chats
- [ ] Multi-language support

## 📄 License

This project is for educational purposes. Use at your own risk.

**Note**: WhatsApp is a trademark of Meta Platforms, Inc. This project is not affiliated with, endorsed by, or connected to Meta or WhatsApp.

## 🤝 Contributing

Contributions are welcome! Please ensure:
- Code follows Kotlin coding conventions
- UI follows Material Design 3
- All features are tested
- Documentation is updated

## 📧 Support

For issues related to:
- **mautrix-whatsapp**: Visit https://github.com/mautrix/whatsapp
- **Matrix**: Visit https://matrix.org
- **This App**: Open an issue in this repository

## 🙏 Credits

- [mautrix-whatsapp](https://github.com/mautrix/whatsapp) - WhatsApp bridge
- [Matrix](https://matrix.org) - Open protocol
- [whatsmeow](https://github.com/tulir/whatsmeow) - WhatsApp Web library
- Android Jetpack team for excellent libraries

---

**⚠️ Disclaimer**: This application is for educational and research purposes only. Using unofficial WhatsApp clients may violate WhatsApp's Terms of Service and could result in account suspension. Use at your own risk.
