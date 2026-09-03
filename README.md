# Agent Notes (Notsey) 🎙️🤖

> **Zero-friction voice and text notes bridge for mobile and local AI agents with an embedded MCP server, Room persistence, and Google Gemini AI.**

[![Platform](https://img.shields.io/badge/Platform-Android-3DDC84?style=flat&logo=android&logoColor=white)](https://developer.android.com/)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.2.21-7F52FF?style=flat&logo=kotlin&logoColor=white)](https://kotlinlang.org/)
[![Compose](https://img.shields.io/badge/Jetpack_Compose-Material_3-4285F4?style=flat&logo=jetpackcompose&logoColor=white)](https://developer.android.com/jetpack/compose)
[![AI](https://img.shields.io/badge/Gemini_API-Enabled-8E75FF?style=flat&logo=google&logoColor=white)](https://ai.google.dev/)
[![MCP](https://img.shields.io/badge/Protocol-Model_Context_Protocol_(MCP)-0FA958?style=flat)](https://modelcontextprotocol.io/)

---

## 📱 Overview

**Agent Notes** is an offline-first Android mobile application designed to seamlessly bridge human thoughts with autonomous AI coding agents (such as Claude Code, Google Antigravity, Cursor, and Windsurf). 

Capture fleeting thoughts on your phone via high-fidelity voice recording or quick text notes, automatically enrich them with Google Gemini (speech-to-text, summaries, actionable tasks, and semantic tags), and expose them to your desktop AI development workflows through an **embedded Model Context Protocol (MCP) server** running directly on your Android device over Wi-Fi or USB.

---

## ✨ Key Features

### 1. 🎙️ High-Fidelity Voice Recording
* **Dynamic Waveform Visualizer**: Live horizontal emerald bars reacting to real-time mic amplitude.
* **Monospace Digital Timer**: High-visibility recording clock with millisecond precision.
* **Live Speech-to-Text**: Real-time transcript preview with pulsing green cursor and language selector.
* **Sound Level Meter**: Dot-matrix audio level indicator ensuring clear audio capture.
* **Circular Ergonomic Controls**: Quick Pause, Stop, and Save buttons engineered for single-thumb mobile usage.

### 2. 🤖 Embedded MCP Server (Agent Bridge)
* **On-Device Server**: Runs a lightweight HTTP/SSE JSON-RPC Model Context Protocol server directly on the Android device.
* **Desktop Agent Integration**: Connect desktop agents (Claude Code, Antigravity, etc.) over Wi-Fi or USB port forwarding (`adb forward tcp:8080 tcp:8080`).
* **Available MCP Tools**:
  * `get_recent_notes`: Retrieve recent notes with filter criteria.
  * `search_notes`: Semantic and text search across note transcripts and titles.
  * `get_note_detail`: Fetch full transcript, AI summary, and related audio metadata.
  * `create_note`: Allow agents to create notes or reminders on the phone.
  * `update_agent_status`: Mark notes as `PROCESSED` or leave agent feedback directly in the note detail.
  * `list_tasks` & `toggle_task`: Inspect and update action items extracted from notes.

### 3. ✨ Gemini AI Intelligence
* **Automatic Transcription**: Converts voice recordings to high-accuracy text.
* **Smart Summaries**: Generates concise executive summaries for rapid scanning.
* **Task Extraction**: Automatically parses action items and creates checkable todo tasks.
* **Semantic Tagging**: Classifies notes with colored pills (`#agent`, `#bug`, `#android`, `#idea`, `#ai`, `#todo`).
* **Vector Embeddings**: Generates embeddings for semantic similarity and note clustering.

### 4. 💾 Offline-First Persistence
* Powered by **Room SQLite Database** with KSP codegen.
* Relational tables for Notes, Tasks, Audio Assets, Embeddings, Tags, and Agent Activity Logs.
* Built-in **Database Inspector Screen** for real-time inspection of database tables and vector records.

---

## 🎨 User Interface & Design System

The app follows a modern, emerald-and-mint design system:
* **Primary Accent**: Emerald Green (`#0FA958`) & Emerald Dark (`#059669`)
* **Background & Surfaces**: Clean Off-White (`#FAFBFA`) and Card Surfaces (`#FFFFFF`) with subtle borders (`#E5E7EB`)
* **Semantic Tags**:
  * `#agent` (Soft Purple: `#EDE9FE` / `#7C3AED`)
  * `#bug` / `PENDING` (Soft Red: `#FEE2E2` / `#EF4444`)
  * `#android` / `#todo` (Soft Blue: `#E0F2FE` / `#0284C7`)
  * `#idea` (Soft Amber: `#FEF3C7` / `#D97706`)
  * `#ai` / `PROCESSED` (Soft Emerald: `#D1FAE5` / `#059669`)

### Main Screens
1. **Home Screen (`NotesListScreen`)**:
   * Branded robot header with unread notification badge.
   * Personal greeting: *"Good morning, Zack 👋"*.
   * Hero glowing *"Tap to record"* button with background waveform.
   * 4 Quick Action cards: *Text Note*, *Video Note*, *Quick Idea*, and *Import*.
   * Filterable stream of recent notes with status pills and audio playback.
2. **Note Detail Screen (`NoteDetailScreen`)**:
   * Waveform audio player card with duration, file size, and play/pause controls.
   * Copyable full transcript card.
   * AI Summary card with expandable view.
   * Agent Response card displaying bridge state from Claude Code / Antigravity.
   * Horizontal carousel of related notes and quick action pills.
3. **Voice Recording Screen (`CaptureScreen`)**:
   * Pulsing red recording badge, digital timer, and animated waveform.
   * Live transcript card with blinking cursor and tag selector.
4. **Agent Bridge Screen (`AgentBridgeScreen`)**:
   * Server status toggle, local IP/Port readout, and live agent connection logs.
5. **Tasks Screen (`TasksScreen`)**:
   * Checkable tasks extracted from notes with completion filters.

---

## 🏗️ Architecture & Tech Stack

```
app/src/main/java/com/example/
├── MainActivity.kt               # Bottom navigation and screen routing
├── ai/
│   ├── GeminiAiService.kt        # Gemini API integration (transcription, summary, tasks)
│   └── GeminiNoteEnhancer.kt     # Note enhancement pipeline
├── audio/
│   ├── AudioRecorderManager.kt   # Microphone capture, metering, and file storage
│   └── AudioPlayerManager.kt     # Media player with progress tracking
├── data/
│   ├── AppDatabase.kt            # Room database configuration
│   ├── NoteEntity.kt             # Note data model
│   ├── TaskEntity.kt             # Task data model
│   ├── AudioEntity.kt            # Audio metadata model
│   ├── EmbeddingEntity.kt        # Vector embedding store
│   └── NoteRepository.kt         # Repository pattern unifying DB and AI operations
├── server/
│   ├── AgentMcpServer.kt         # Embedded MCP Server & JSON-RPC API
│   └── NetworkUtils.kt           # Local Wi-Fi IP discovery
└── ui/
    ├── MainViewModel.kt          # MVI/MVVM UI state management
    ├── components/               # Reusable cards, waveforms, tag pills
    ├── screens/                  # Compose screen layouts
    └── theme/                    # Color tokens, Typography, and Theme
```

| Layer | Technologies |
| :--- | :--- |
| **UI Framework** | Jetpack Compose, Material 3, Accompanist |
| **State Management** | Android ViewModel, Kotlin `StateFlow`, Coroutines |
| **Local Storage** | Room SQLite 2.7 with KSP (Kotlin Symbol Processing) |
| **Audio** | Android MediaRecorder, MediaPlayer, AudioRecord |
| **AI / LLM** | Google Gemini API via OkHttp / Retrofit / Moshi |
| **Testing** | Robolectric, Roborazzi (Native Compose Screenshot Tests), JUnit 4 |
| **Tooling** | Gradle 9.3.1, Android Gradle Plugin 8.9, JDK 21 |

---

## 🚀 Getting Started

### Prerequisites
* **Android Studio**: Ladybug (2024.2) or later
* **JDK**: Version 21 (e.g. Eclipse Adoptium or OpenJDK 21)
* **Android SDK**: Platform API 36 (Android 15/16)
* **Target Device**: Physical Android phone (API 24+) or Android Emulator

### Environment Configuration
1. Copy `.env.example` to `.env`:
   ```bash
   cp .env.example .env
   ```
2. Add your **Gemini API Key**:
   ```env
   GEMINI_API_KEY=your_gemini_api_key_here
   ```

### Building & Running

#### 1. Via Command Line (Gradle Wrapper)
Make sure `JAVA_HOME` points to your JDK 21 installation:
```bash
# On Windows (PowerShell)
$env:JAVA_HOME = "C:\Users\<username>\.jdk\jdk-21.0.12.1+1"
$env:PATH = "$env:JAVA_HOME\bin;$env:PATH"

# Build debug APK
.\gradlew.bat assembleDebug

# Install directly to connected device
.\gradlew.bat installDebug
```

#### 2. Via Android CLI (`android`)
```bash
# Deploy to connected device
android run
```

---

## 🔗 Connecting Desktop AI Agents via MCP

When the app is running on your phone, start the Agent Bridge from the **Agents** tab:

1. **Forward the port via ADB**:
   ```bash
   adb forward tcp:8080 tcp:8080
   ```
2. **Add to Claude Desktop or Antigravity MCP Config**:
   ```json
   {
     "mcpServers": {
       "agent-notes": {
         "url": "http://127.0.0.1:8080/sse"
       }
     }
   }
   ```
3. Your desktop agent can now read voice notes, search transcripts, and mark tasks complete automatically!

---

## 🧪 Testing & Screenshot Previews

The project includes **Roborazzi** and **Robolectric** tests that render pixel-perfect screenshots of Compose screens without requiring a physical device or emulator.

To run the tests and generate screenshots:
```bash
.\gradlew.bat testDebugUnitTest
```

Generated screenshots are saved to:
```
app/src/test/screenshots/
```

---

## 📄 License

This project is licensed under the Apache License 2.0.
