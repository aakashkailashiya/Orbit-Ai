# 🚀 OrbitAI - Your Personal AI Assistant

<div align="center">
<img width="1024" height="1024" alt="orbitai" src="https://github.com/user-attachments/assets/9b2728d8-2730-4fef-8dc6-a02bfc416d26" />

</div><br>
## 📱 Overview

  
OrbitAI is a powerful, privacy-focused AI chat assistant for Android that runs large language models directly on your device. No internet connection required after model download! 🌐

### ✨ Key Features

- 🤖 **Multiple AI Models**: Choose from various state-of-the-art models including Gemma and Phi
- 💬 **Natural Conversations**: Engage in fluid, human-like conversations with AI
- 📚 **Chat History**: Save and organize your conversations for future reference
- 🧠 **System Prompts**: Customize AI behavior with different personality presets
- 🔊 **Text-to-Speech**: Listen to AI responses in multiple languages
- 📤 **Export Chats**: Share your conversations with others
- 🌙 **Beautiful UI**: Modern Material Design 3 interface with dark mode support
- 🔒 **Privacy First**: All processing happens locally on your device

## 🛠️ Technical Stack

- **Language**: Kotlin
- **UI Framework**: Jetpack Compose
- **Architecture**: MVVM with Repository Pattern
- **Database**: Room
- **AI Engine**: MediaPipe LLM Inference
- **TTS**: Android Text-to-Speech

## 📸 Screenshots

<div align="center">
  <img width="1080" height="2340" alt="orbit_ai (1)" src="https://github.com/user-attachments/assets/2c084ea1-c7db-467b-a3a6-abadde159a61" /><br>
  <img width="1080" height="2340" alt="orbit_ai (2)" src="https://github.com/user-attachments/assets/c7c1e216-a00d-4ede-9a11-c20b0d125ad9" /><br>
  <img width="1080" height="2340" alt="orbit_ai (3)" src="https://github.com/user-attachments/assets/47a7aa89-5dba-4710-ac8f-9425502ea61b" /><br>
  <img width="1080" height="2340" alt="orbit_ai (4)" src="https://github.com/user-attachments/assets/531aa9e8-269c-4028-9cd5-ec137e927ff8" /><br>
<img width="1080" height="2340" alt="orbit_ai (5)" src="https://github.com/user-attachments/assets/0b4aa0ac-4b93-4a0f-a611-a290b49e2133" />




</div>

## 🚀 Getting Started

### Prerequisites

- Android Studio Hedgehog | 2023.1.1 or newer
- Android 7.0 (API level 24) or higher
- A device with at least 4GB of RAM for optimal performance

### Installation

1. Clone the repository:
   ```bash
   git clone https://github.com/aakashkailashiya/orbit-ai.git
   ```

2. Open the project in Android Studio

3. Build and run the app:
   ```bash
   ./gradlew assembleDebug
   ```

4. Install the APK on your device or emulator

## 🤖 AI Models

OrbitAI supports multiple AI models with different capabilities and resource requirements:

| Model | Size | Description | Performance |
|-------|------|-------------|-------------|
| 💎 Gemma 2B | 1.4 GB | Balanced performance and quality | ⭐⭐⭐⭐ |
| 🧠 Phi-2 | 1.6 GB | Strong reasoning capabilities | ⭐⭐⭐⭐ |
| ✨ Gemma 1B | 0.7 GB | Lightweight, fastest response | ⭐⭐⭐ |
| 🚀 Gemma 7B | 7.8 GB | Highest quality responses | ⭐⭐⭐⭐⭐ |

## 🧩 System Prompts

Customize your AI experience with these personality presets:

- 🤖 **Orbit Assistant**: Friendly and helpful
- 💻 **Code Generator**: Expert programming assistant
- ✍️ **Creative Writer**: Master storyteller
- 📝 **Summarizer**: Condenses complex information
- 👶 **ELI5**: Explains topics simply
- 🎭 **Roleplayer**: Adopts various personas

## 🔧 Configuration

### Model Settings

1. Open the app drawer
2. Tap on "🤖 Browse Models"
3. Select and download your preferred model
4. Wait for the download to complete

### TTS Settings

1. Open the app drawer
2. Tap on "⚙️ Settings"
3. Select your preferred language:
   - 🇺🇸 English
   - 🇮🇳 Hindi
4. Enable "Auto-speak bot responses" if desired

## 📝 Usage

### Starting a Conversation

1. Launch OrbitAI
2. Wait for the model to load (check the bottom bar)
3. Type your message in the input field
4. Tap the send button or press Enter

### Managing Chats

- **Save Chat**: Tap the save icon in the top bar and enter a name
- **Load Chat**: Open the drawer and select from your chat history
- **Delete Chat**: Long-press on a chat in the drawer and select delete

### Customizing AI Behavior

1. Open the app drawer
2. Tap on "🧠 System Prompts"
3. Select a preset or create your own
4. Toggle "Include chat history" for context-aware responses

## 🐛 Troubleshooting

### Common Issues

**Model Loading Failed**:
- Ensure you have sufficient storage space
- Check your internet connection for initial download
- Try restarting the app

**Slow Response Times**:
- Consider using a smaller model (Gemma 1B)
- Close other apps to free up RAM
- Check if your device is overheating

**TTS Not Working**:
- Verify your device has TTS capabilities
- Check if the selected language is installed
- Try restarting the app

### Getting Help

If you encounter any issues:

1. Check the [Issues]([https://github.com/aakashkailashiya/orbit-ai/issues](https://github.com/aakashkailashiya/Orbit-Ai)) page
2. Create a new issue with:
   - Device model and Android version
   - Steps to reproduce the problem
   - Screenshots if applicable
   - Logcat output

## 🤝 Contributing

We welcome contributions! Here's how you can help:

1. Fork the repository
2. Create a feature branch: `git checkout -b feature/amazing-feature`
3. Commit your changes: `git commit -m 'Add amazing feature'`
4. Push to the branch: `git push origin feature/amazing-feature`
5. Open a Pull Request

### Development Guidelines

- Follow the existing code style
- Add comments to complex functions
- Update documentation for new features
- Include tests for new functionality

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 🙏 Acknowledgments

- [Google](https://ai.google.dev/) for the Gemma models
- [Microsoft](https://www.microsoft.com/en-us/research/) for the Phi-2 model
- [MediaPipe](https://mediapipe.dev/) for the LLM inference engine
- [Jetpack Compose](https://developer.android.com/jetpack/compose) for the modern UI toolkit

---

<div align="center">
  <p>Made by Akash Kailashiya</p>
  <p>If you like this project, please consider giving it a ⭐ on GitHub!</p>
</div>
