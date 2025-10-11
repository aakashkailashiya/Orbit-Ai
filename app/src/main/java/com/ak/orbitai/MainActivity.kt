package com.ak.orbitai

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.ak.orbitai.ui.theme.OrbitAiTheme
import com.google.mediapipe.tasks.genai.llminference.LlmInference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.net.URL
import java.text.SimpleDateFormat
import java.util.*

// Enhanced data classes
data class Message(
    val id: Long,
    val sender: String,
    var text: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isError: Boolean = false,
    val isStreaming: Boolean = false
)

data class SystemPrompt(
    val id: Long,
    val name: String,
    val content: String
)

data class ChatHistory(
    val id: Long,
    val name: String,
    val messages: List<Message>,
    val createdAt: Long = System.currentTimeMillis()
)

enum class ModelState {
    IDLE,
    LOADING,
    LOADED,
    ERROR,
    GENERATING
}

data class ModelInfo(
    val name: String,
    val path: String,
    val size: String = "",
    val loadedAt: Long = System.currentTimeMillis()
)

enum class TTSLanguage(val code: String, val displayName: String) {
    ENGLISH("en", "English"),
    HINDI("hi", "Hindi")
}

data class DownloadableModel(
    val name: String,
    val description: String,
    val url: String,
    val size: String
)


val modelStore = listOf(
    // Original Models
    DownloadableModel(
        name = "gemma-2b-it-cpu-int4.task",
        description = "Gemma 2B is a lightweight, state-of-the-art open model from Google. This is a 4-bit quantized version for CPU.",
        url = "https://storage.googleapis.com/mediapipe-models/genai_llminference_task/gemma_2b_it_cpu_int4.task",
        size = "1.4 GB"
    ),
    DownloadableModel(
        name = "phi-2-cpu-int4.task",
        description = "Phi-2 is a small, powerful model from Microsoft, ideal for various tasks. This is a 4-bit quantized version for CPU.",
        url = "https://storage.googleapis.com/mediapipe-models/genai_llminference_task/phi_2_cpu_int4.task",
        size = "1.6 GB"
    ),

    // Added Models (Gemma 1B and a slightly larger Gemma 7B variant for GPU-capable devices)
    DownloadableModel(
        name = "gemma-1b-it-cpu-int4.task",
        description = "The lightest Gemma model (1B parameters), optimized for the smallest memory footprint on mobile CPUs.",
        url = "https://storage.googleapis.com/mediapipe-models/genai_llminference_task/gemma_1b_it_cpu_int4.task",
        size = "0.7 GB"
    ),
    DownloadableModel(
        name = "gemma-7b-it-gpu-int8.task",
        description = "A powerful Gemma 7B instruction-tuned model, 8-bit quantized for better performance on mobile GPUs.",
        url = "https://storage.googleapis.com/mediapipe-models/genai_llminference_task/gemma_7b_it_gpu_int8.task",
        size = "7.8 GB"
    )
)

class MainActivity : ComponentActivity() {
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var textToSpeech: TextToSpeech

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize SharedPreferences
        sharedPreferences = getSharedPreferences("OrbitAI_Prefs", Context.MODE_PRIVATE)

        // Initialize TextToSpeech
        textToSpeech = TextToSpeech(this) { status ->
            if (status == TextToSpeech.SUCCESS) {
                // Set default language to English
                val result = textToSpeech.setLanguage(Locale.ENGLISH)
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    // Language not supported, handle error
                }
            }
        }

        setContent {
            OrbitAiTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    ChatScreen(sharedPreferences, textToSpeech)
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        textToSpeech.shutdown()
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ChatScreen(
    sharedPreferences: SharedPreferences,
    textToSpeech: TextToSpeech
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)

    var userMessage by remember { mutableStateOf("") }
    val messages = remember { mutableStateListOf<Message>() }
    var llmInference by remember { mutableStateOf<LlmInference?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var modelInfo by remember { mutableStateOf<ModelInfo?>(null) }
    var modelState by remember { mutableStateOf(ModelState.IDLE) }
    var showSystemPromptDialog by remember { mutableStateOf(false) }
    var showSaveChatDialog by remember { mutableStateOf(false) }
    var includeHistory by remember { mutableStateOf(true) }
    var showSettingsDialog by remember { mutableStateOf(false) }
    var showModelStoreDialog by remember { mutableStateOf(false) }
    var selectedTTSLanguage by remember { mutableStateOf(TTSLanguage.ENGLISH) }
    var autoSpeakBotResponses by remember { mutableStateOf(false) }

    // System prompts
    val systemPrompts = remember {
        mutableStateListOf(
            SystemPrompt(1, "Orbit Assistant", "You are Orbit, a friendly and helpful AI assistant. Your goal is to provide clear, accurate, and concise information. Be conversational and engaging."),
            SystemPrompt(2, "Code Generator", "You are an expert programmer. You provide high-quality, efficient, and well-documented code. When asked for code, provide a complete, runnable example."),
            SystemPrompt(3, "Creative Writer", "You are a master storyteller. Your task is to craft imaginative, vivid, and emotionally resonant stories, poems, or scripts. Use rich language and imagery."),
            SystemPrompt(4, "Summarizer", "You are a skilled summarizer. Condense long texts into short, easy-to-understand summaries, capturing the key points."),
            SystemPrompt(5, "ELI5", "You explain complex topics simply, using analogies and avoiding jargon, as if talking to a five-year-old."),
            SystemPrompt(6, "Roleplayer", "You are a versatile roleplaying character. Adopt the persona given by the user and act out scenarios. Stay in character.")
        )
    }
    var selectedSystemPrompt by remember { mutableStateOf(systemPrompts[0]) }

    // Chat histories
    val chatHistories = remember { mutableStateListOf<ChatHistory>() }

    val clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val listState = rememberLazyListState()

    // Load chat histories on start
    LaunchedEffect(Unit) {
        loadChatHistories(context) { histories ->
            chatHistories.clear()
            chatHistories.addAll(histories)
        }
    }

    // Auto-scroll to bottom when new messages arrive
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            delay(100)
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    // Load saved preferences
    LaunchedEffect(Unit) {
        val savedLanguageCode = sharedPreferences.getString("tts_language", TTSLanguage.ENGLISH.code)
        selectedTTSLanguage = TTSLanguage.values().find { it.code == savedLanguageCode } ?: TTSLanguage.ENGLISH
        autoSpeakBotResponses = sharedPreferences.getBoolean("auto_speak_responses", false)
    }

    // Auto-load model on app start
    LaunchedEffect(Unit) {
        val savedModelPath = sharedPreferences.getString("saved_model_path", null)

        if (savedModelPath != null && File(savedModelPath).exists()) {
            val savedModelName = sharedPreferences.getString("saved_model_name", "Saved Model")
            modelInfo = ModelInfo(savedModelName ?: "Saved Model", savedModelPath)
            modelState = ModelState.LOADING

            initializeLlm(context, savedModelPath, llmInference) { newLlm, error ->
                llmInference = newLlm
                errorMessage = error
                modelState = if (error == null) ModelState.LOADED else ModelState.ERROR

                if (error == null) {
                    Toast.makeText(context, "✅ Saved model loaded", Toast.LENGTH_SHORT).show()
                } else {
                    coroutineScope.launch {
                        loadDefaultModel(context) { newModelInfo, loadError ->
                            modelInfo = newModelInfo
                            errorMessage = loadError
                            modelState = if (loadError == null) ModelState.LOADED else ModelState.ERROR
                        }
                    }
                }
            }
        } else {
            coroutineScope.launch {
                loadDefaultModel(context) { newModelInfo, error ->
                    modelInfo = newModelInfo
                    errorMessage = error
                    modelState = if (error == null) ModelState.LOADED else ModelState.ERROR
                }
            }
        }
    }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
        onResult = { uri: Uri? ->
            uri?.let {
                coroutineScope.launch {
                    loadCustomModel(context, uri) { newModelInfo, error ->
                        modelInfo = newModelInfo
                        errorMessage = error
                        modelState = if (error == null) ModelState.LOADED else ModelState.ERROR

                        if (error == null) {
                            sharedPreferences.edit()
                                .putString("saved_model_path", newModelInfo?.path)
                                .putString("saved_model_name", newModelInfo?.name)
                                .apply()

                            Toast.makeText(context, "✅ Model loaded and saved", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        }
    )

    // Initialize LLM when model path changes
    LaunchedEffect(modelInfo?.path) {
        modelInfo?.path?.let { path ->
            initializeLlm(context, path, llmInference) { newLlm, error ->
                llmInference = newLlm
                errorMessage = error
                modelState = if (error == null) ModelState.LOADED else ModelState.ERROR
            }
        }
    }

    fun speakText(text: String) {
        val locale = when (selectedTTSLanguage) {
            TTSLanguage.ENGLISH -> Locale.ENGLISH
            TTSLanguage.HINDI -> Locale("hi", "IN")
        }

        textToSpeech.language = locale
        textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
    }

    fun sendMessage() {
        if (userMessage.isBlank() || modelState != ModelState.LOADED) return

        val userText = userMessage.trim()
        messages.add(
            Message(
                id = System.currentTimeMillis(),
                sender = "You",
                text = userText
            )
        )
        userMessage = ""

        val botMessageId = System.currentTimeMillis() + 1
        val botMessage = Message(
            id = botMessageId,
            sender = "Bot",
            text = "",
            isStreaming = true
        )
        messages.add(botMessage)

        modelState = ModelState.GENERATING

        coroutineScope.launch {
            try {
                val contextMessages = if (includeHistory) {
                    val contextList = mutableListOf<String>()
                    contextList.add("System: ${selectedSystemPrompt.content}")
                    val recentMessages = messages.takeLast(10)
                    recentMessages.forEach { msg ->
                        if (!msg.isStreaming) {
                            contextList.add("${msg.sender}: ${msg.text}")
                        }
                    }
                    contextList.joinToString("\n")
                } else {
                    "System: ${selectedSystemPrompt.content}\nYou: $userText"
                }

                val response = withContext(Dispatchers.IO) {
                    llmInference?.generateResponse(contextMessages) ?: "Error: Model not loaded"
                }

                val botMessageIndex = messages.indexOfFirst { it.id == botMessageId }
                if (botMessageIndex != -1) {
                    messages[botMessageIndex] = messages[botMessageIndex].copy(
                        text = response,
                        isStreaming = false
                    )
                    if (autoSpeakBotResponses) {
                        speakText(response)
                    }
                }
            } catch (e: Exception) {
                val botMessageIndex = messages.indexOfFirst { it.id == botMessageId }
                if (botMessageIndex != -1) {
                    messages[botMessageIndex] = messages[botMessageIndex].copy(
                        text = "Error: ${e.localizedMessage ?: "Unknown error"}",
                        isError = true,
                        isStreaming = false
                    )
                }
                e.printStackTrace()
            } finally {
                modelState = ModelState.LOADED
            }
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            AppDrawer(
                chatHistories = chatHistories,
                onChatSelected = {
                    messages.clear()
                    messages.addAll(it.messages)
                    coroutineScope.launch { drawerState.close() }
                },
                onNewChat = {
                    messages.clear()
                    coroutineScope.launch { drawerState.close() }
                },
                onBrowseModels = {
                    showModelStoreDialog = true
                    coroutineScope.launch { drawerState.close() }
                },
                onExportChat = {
                    exportChat(context, messages.toList())
                    coroutineScope.launch { drawerState.close() }
                },
                onAbout = {
                    // show about dialog
                    coroutineScope.launch { drawerState.close() }
                },
                onDeleteChat = {
                    try {
                        deleteChat(context, it.id) { success, message ->
                            if (success) {
                                Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                                loadChatHistories(context) { histories ->
                                    chatHistories.clear()
                                    chatHistories.addAll(histories)
                                }
                            } else {
                                Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                            }
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                        Toast.makeText(context, "Failed to delete chat: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                    }
                }
            )
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text("OrbitAI", fontWeight = FontWeight.Bold)
                            Text(
                                buildAnnotatedString {
                                    append("Model: ")
                                    withStyle(SpanStyle(fontWeight = FontWeight.SemiBold)) {
                                        append(modelInfo?.name ?: "Not loaded")
                                    }
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = when (modelState) {
                                    ModelState.LOADED -> Color(0xFF4CAF50)
                                    ModelState.ERROR -> MaterialTheme.colorScheme.error
                                    ModelState.GENERATING -> MaterialTheme.colorScheme.primary
                                    else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                }
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = { coroutineScope.launch { drawerState.open() } }) {
                            Icon(Icons.Default.Menu, contentDescription = "Menu")
                        }
                    },
                    actions = {
                        // Model loading indicator
                        if (modelState == ModelState.LOADING) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                        }

                        IconButton(
                            onClick = { showSettingsDialog = true },
                            enabled = modelState != ModelState.LOADING && modelState != ModelState.GENERATING
                        ) {
                            Icon(Icons.Outlined.Settings, contentDescription = "Settings")
                        }

                        IconButton(
                            onClick = { showSystemPromptDialog = true },
                            enabled = modelState != ModelState.LOADING && modelState != ModelState.GENERATING
                        ) {
                            Icon(Icons.Filled.Psychology, contentDescription = "System Prompt")
                        }

                        IconButton(
                            onClick = {
                                try {
                                    filePickerLauncher.launch(arrayOf("*/*"))
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                    Toast.makeText(context, "Failed to open file picker", Toast.LENGTH_SHORT).show()
                                }
                            },
                            enabled = modelState != ModelState.LOADING && modelState != ModelState.GENERATING
                        ) {
                            Icon(Icons.Outlined.AddCircle, contentDescription = "Load Model")
                        }

                        IconButton(
                            onClick = { showSaveChatDialog = true },
                            enabled = messages.isNotEmpty()
                        ) {
                            Icon(Icons.Outlined.Save, contentDescription = "Save Chat")
                        }

                        IconButton(
                            onClick = {
                                messages.clear()
                                Toast.makeText(context, "Chat cleared", Toast.LENGTH_SHORT).show()
                            },
                            enabled = messages.isNotEmpty()
                        ) {
                            Icon(Icons.Outlined.Delete, contentDescription = "Clear Chat")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                // Welcome card
                AnimatedVisibility(
                    visible = messages.isEmpty(),
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    WelcomeCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        onLoadModel = {
                            try {
                                filePickerLauncher.launch(arrayOf("*/*"))
                            } catch (e: Exception) {
                                e.printStackTrace()
                                Toast.makeText(context, "Failed to open file picker", Toast.LENGTH_SHORT).show()
                            }
                        },
                        onLoadChat = { coroutineScope.launch { drawerState.open() } }
                    )
                }

                // Error/Status card
                AnimatedVisibility(
                    visible = errorMessage != null,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    StatusCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        message = errorMessage,
                        isError = true,
                        onDismiss = { errorMessage = null }
                    )
                }

                // System prompt indicator
                AnimatedVisibility(
                    visible = selectedSystemPrompt.content.isNotEmpty(),
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    SystemPromptIndicator(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        prompt = selectedSystemPrompt,
                        onIncludeHistoryToggle = { includeHistory = it },
                        includeHistory = includeHistory
                    )
                }

                // Messages list
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    state = listState,
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(messages, key = { it.id }) { message ->
                        MessageBubble(
                            message = message,
                            onCopy = {
                                try {
                                    val clip = ClipData.newPlainText("message", message.text)
                                    clipboardManager.setPrimaryClip(clip)
                                    Toast.makeText(context, "📋 Copied", Toast.LENGTH_SHORT).show()
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                    Toast.makeText(context, "Failed to copy text", Toast.LENGTH_SHORT).show()
                                }
                            },
                            onSpeak = { speakText(message.text) }
                        )
                    }

                    // Typing indicator
                    if (modelState == ModelState.GENERATING) {
                        item {
                            TypingIndicator()
                        }
                    }
                }

                // Input area
                InputArea(
                    message = userMessage,
                    onMessageChange = { userMessage = it },
                    onSend = ::sendMessage,
                    isModelReady = modelState == ModelState.LOADED,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                )
            }
        }
    }

    // Settings Dialog
    if (showSettingsDialog) {
        SettingsDialog(
            selectedTTSLanguage = selectedTTSLanguage,
            onLanguageSelected = { language ->
                selectedTTSLanguage = language
                sharedPreferences.edit()
                    .putString("tts_language", language.code)
                    .apply()

                // Update TTS language
                val locale = when (language) {
                    TTSLanguage.ENGLISH -> Locale.ENGLISH
                    TTSLanguage.HINDI -> Locale("hi", "IN")
                }
                textToSpeech.language = locale

                Toast.makeText(context, "TTS language changed to ${language.displayName}", Toast.LENGTH_SHORT).show()
            },
            autoSpeakEnabled = autoSpeakBotResponses,
            onAutoSpeakChange = { enabled ->
                autoSpeakBotResponses = enabled
                sharedPreferences.edit()
                    .putBoolean("auto_speak_responses", enabled)
                    .apply()
            },
            onDismiss = { showSettingsDialog = false }
        )
    }

    if (showModelStoreDialog) {
        ModelStoreDialog(
            models = modelStore,
            onDismiss = { showModelStoreDialog = false },
            onModelSelected = {
                showModelStoreDialog = false
                coroutineScope.launch {
                    downloadAndLoadModel(context, it, sharedPreferences) { newModelInfo, error ->
                        if (newModelInfo != null) {
                            modelInfo = newModelInfo
                            Toast.makeText(context, "✅ Model loaded: ${newModelInfo.name}", Toast.LENGTH_SHORT).show()
                        }
                        errorMessage = error
                        modelState = if (error == null) ModelState.LOADED else ModelState.ERROR
                    }
                }
            }
        )
    }

    // System Prompt Dialog
    if (showSystemPromptDialog) {
        SystemPromptDialog(
            systemPrompts = systemPrompts,
            selectedPrompt = selectedSystemPrompt,
            onPromptSelected = { selectedSystemPrompt = it },
            onPromptAdded = { prompt ->
                systemPrompts.add(prompt)
                selectedSystemPrompt = prompt
            },
            onPromptDeleted = { prompt ->
                systemPrompts.remove(prompt)
                if (selectedSystemPrompt.id == prompt.id && systemPrompts.isNotEmpty()) {
                    selectedSystemPrompt = systemPrompts[0]
                }
            },
            onDismiss = { showSystemPromptDialog = false }
        )
    }

    // Save Chat Dialog
    if (showSaveChatDialog) {
        SaveChatDialog(
            onSave = { name ->
                saveChat(context, name, messages.toList()) { success, message ->
                    if (success) {
                        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                        // Refresh chat histories
                        loadChatHistories(context) { histories ->
                            chatHistories.clear()
                            chatHistories.addAll(histories)
                        }
                    } else {
                        Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                    }
                }
                showSaveChatDialog = false
            },
            onDismiss = { showSaveChatDialog = false }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppDrawer(
    chatHistories: List<ChatHistory>,
    onChatSelected: (ChatHistory) -> Unit,
    onNewChat: () -> Unit,
    onBrowseModels: () -> Unit,
    onExportChat: () -> Unit,
    onAbout: () -> Unit,
    onDeleteChat: (ChatHistory) -> Unit
) {
    ModalDrawerSheet {
        Text("OrbitAI", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(16.dp))
        Divider()
        NavigationDrawerItem(
            label = { Text("New Chat") },
            selected = false,
            onClick = onNewChat,
            icon = { Icon(Icons.Outlined.Add, contentDescription = "New Chat") }
        )
        Divider()
        Text("Chat History", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(16.dp))
        LazyColumn(modifier = Modifier.weight(1f)) {
            if (chatHistories.isEmpty()) {
                item {
                    Text(
                        "No saved chats",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
            items(chatHistories) { chat ->
                NavigationDrawerItem(
                    label = { Text(chat.name, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                    selected = false,
                    onClick = { onChatSelected(chat) },
                    icon = { Icon(Icons.Outlined.Chat, contentDescription = chat.name) },
                    badge = {
                        IconButton(onClick = { onDeleteChat(chat) }) {
                            Icon(Icons.Outlined.DeleteOutline, contentDescription = "Delete Chat")
                        }
                    }
                )
            }
        }
        Divider()
        NavigationDrawerItem(
            label = { Text("Browse Models") },
            selected = false,
            onClick = onBrowseModels,
            icon = { Icon(Icons.Outlined.CloudDownload, contentDescription = "Browse Models") }
        )
        NavigationDrawerItem(
            label = { Text("Export Chat") },
            selected = false,
            onClick = onExportChat,
            icon = { Icon(Icons.Outlined.IosShare, contentDescription = "Export Chat") }
        )
        NavigationDrawerItem(
            label = { Text("About OrbitAI") },
            selected = false,
            onClick = onAbout,
            icon = { Icon(Icons.Outlined.Info, contentDescription = "About OrbitAI") }
        )
    }
}


// Other composables and helper functions would go here...

@Composable
fun WelcomeCard(modifier: Modifier = Modifier, onLoadModel: () -> Unit, onLoadChat: () -> Unit) {
    Card(modifier = modifier) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Welcome to OrbitAI")
            Button(onClick = onLoadModel) { Text("Load Model") }
            Button(onClick = onLoadChat) { Text("Load Chat") }
        }
    }
}

@Composable
fun StatusCard(
    modifier: Modifier = Modifier,
    message: String?,
    isError: Boolean,
    onDismiss: () -> Unit
) {
    Card(modifier = modifier) {
        Row(modifier = Modifier.padding(16.dp)) {
            Text(message ?: "", modifier = Modifier.weight(1f))
            IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, null) }
        }
    }
}

@Composable
fun SystemPromptIndicator(
    modifier: Modifier = Modifier,
    prompt: SystemPrompt,
    onIncludeHistoryToggle: (Boolean) -> Unit,
    includeHistory: Boolean
) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        Text(prompt.name, modifier = Modifier.weight(1f))
        Switch(checked = includeHistory, onCheckedChange = onIncludeHistoryToggle)
    }
}

@Composable
fun MessageBubble(message: Message, onCopy: () -> Unit, onSpeak: () -> Unit) {
    Row {
        Text(message.text)
        IconButton(onClick = onCopy) { Icon(Icons.Default.ContentCopy, null) }
        IconButton(onClick = onSpeak) { Icon(Icons.Default.VolumeUp, null) }
    }
}

@Composable
fun TypingIndicator() {
    Text("AI is thinking...")
}

@Composable
fun InputArea(
    message: String,
    onMessageChange: (String) -> Unit,
    onSend: () -> Unit,
    isModelReady: Boolean,
    modifier: Modifier = Modifier
) {
    val isSendEnabled = isModelReady && message.isNotBlank()

    Card(
        modifier = modifier,
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(24.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = message,
                onValueChange = onMessageChange,
                modifier = Modifier.weight(1f),
                enabled = isModelReady,
                placeholder = { Text("Message OrbitAI...") },
                maxLines = 4,
                shape = RoundedCornerShape(20.dp),
                colors = TextFieldDefaults.colors(
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    disabledIndicatorColor = Color.Transparent
                )
            )

            Spacer(modifier = Modifier.width(8.dp))

            IconButton(
                onClick = onSend,
                enabled = isSendEnabled,
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(
                        if (isSendEnabled) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
                    )
            ) {
                Icon(
                    Icons.Filled.Send,
                    contentDescription = "Send",
                    tint = if (isSendEnabled) MaterialTheme.colorScheme.onPrimary
                    else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
fun SettingsDialog(
    selectedTTSLanguage: TTSLanguage,
    onLanguageSelected: (TTSLanguage) -> Unit,
    autoSpeakEnabled: Boolean,
    onAutoSpeakChange: (Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    var showLanguageMenu by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Settings") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                // TTS Language Setting
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("TTS Language", modifier = Modifier.weight(1f))
                    Box {
                        TextButton(onClick = { showLanguageMenu = true }) {
                            Text(selectedTTSLanguage.displayName)
                        }
                        DropdownMenu(
                            expanded = showLanguageMenu,
                            onDismissRequest = { showLanguageMenu = false }
                        ) {
                            TTSLanguage.values().forEach { language ->
                                DropdownMenuItem(
                                    text = { Text(language.displayName) },
                                    onClick = {
                                        onLanguageSelected(language)
                                        showLanguageMenu = false
                                    }
                                )
                            }
                        }
                    }
                }

                // Auto-speak bot responses setting
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Auto-speak bot responses", modifier = Modifier.weight(1f))
                    Switch(
                        checked = autoSpeakEnabled,
                        onCheckedChange = onAutoSpeakChange
                    )
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("Done")
            }
        }
    )
}

@Composable
fun ModelStoreDialog(
    models: List<DownloadableModel>,
    onDismiss: () -> Unit,
    onModelSelected: (DownloadableModel) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Browse Models") },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(models) { model ->
                    Card(elevation = CardDefaults.cardElevation(2.dp)) {
                        Column(Modifier.padding(16.dp)) {
                            Text(model.name, style = MaterialTheme.typography.titleMedium)
                            Text(model.description, style = MaterialTheme.typography.bodyMedium)
                            Spacer(Modifier.height(8.dp))
                            Text("Size: ${model.size}", style = MaterialTheme.typography.bodySmall)
                            Button(onClick = { onModelSelected(model) }) {
                                Text("Download and Load")
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun SystemPromptDialog(
    systemPrompts: List<SystemPrompt>,
    selectedPrompt: SystemPrompt,
    onPromptSelected: (SystemPrompt) -> Unit,
    onPromptAdded: (SystemPrompt) -> Unit,
    onPromptDeleted: (SystemPrompt) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(onDismissRequest = onDismiss, title = { Text("System Prompts") }, text = { Text("Select a prompt") }, confirmButton = { Button(onClick = onDismiss) { Text("OK") } })
}

@Composable
fun SaveChatDialog(onSave: (String) -> Unit, onDismiss: () -> Unit) {
    var name by remember { mutableStateOf("") }
    AlertDialog(onDismissRequest = onDismiss, title = { Text("Save Chat") }, text = { TextField(value = name, onValueChange = { name = it }) }, confirmButton = { Button(onClick = { onSave(name) }) { Text("Save") } })
}

// Model and Chat Helper Functions

suspend fun downloadAndLoadModel(
    context: Context,
    model: DownloadableModel,
    sharedPreferences: SharedPreferences,
    onResult: (ModelInfo?, String?) -> Unit
) {
    withContext(Dispatchers.Main) {
        onResult(null, "Downloading ${model.name}...")
    }
    withContext(Dispatchers.IO) {
        try {
            val modelFile = File(context.filesDir, model.name)
            if (!modelFile.exists()) {
                val url = URL(model.url)
                url.openStream().use { input ->
                    modelFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
            }
            withContext(Dispatchers.Main) {
                sharedPreferences.edit()
                    .putString("saved_model_path", modelFile.absolutePath)
                    .putString("saved_model_name", model.name)
                    .apply()
                onResult(ModelInfo(model.name, modelFile.absolutePath, model.size), null)
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                onResult(null, "Failed to download model: ${e.localizedMessage}")
            }
        }
    }
}

suspend fun loadDefaultModel(context: Context, onResult: (ModelInfo?, String?) -> Unit) {
    withContext(Dispatchers.IO) {
        try {
            val modelName = "gemma-2b-it-cpu-int4.task"
            val modelFile = File(context.filesDir, modelName)
            if (!modelFile.exists()) {
                context.assets.open(modelName).use { inputStream ->
                    modelFile.outputStream().use { outputStream ->
                        inputStream.copyTo(outputStream)
                    }
                }
            }
            withContext(Dispatchers.Main) {
                onResult(ModelInfo(modelName, modelFile.absolutePath), null)
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                onResult(null, "Default model not found in assets")
            }
        }
    }
}

suspend fun loadCustomModel(context: Context, uri: Uri, onResult: (ModelInfo?, String?) -> Unit) {
    withContext(Dispatchers.IO) {
        try {
            val fileName = uri.path?.split("/")?.lastOrNull() ?: "custom_model.task"
            val file = File(context.filesDir, fileName)
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                file.outputStream().use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            }
            withContext(Dispatchers.Main) {
                onResult(ModelInfo(fileName, file.absolutePath), null)
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                onResult(null, "Failed to load model: ${e.localizedMessage}")
            }
        }
    }
}

suspend fun initializeLlm(
    context: Context,
    modelPath: String,
    currentLlm: LlmInference?,
    onResult: (LlmInference?, String?) -> Unit
) {
    withContext(Dispatchers.IO) {
        try {
            currentLlm?.close()
            val options = LlmInference.LlmInferenceOptions.builder()
                .setModelPath(modelPath)
                .setMaxTokens(1024)
                .build()

            val llm = LlmInference.createFromOptions(context, options)
            withContext(Dispatchers.Main) {
                onResult(llm, null)
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                onResult(null, "Invalid model file: ${e.localizedMessage}")
            }
        }
    }
}

fun saveChat(context: Context, name: String, messages: List<Message>, onResult: (Boolean, String) -> Unit) {
    // Implementation for saving chat
}

fun loadChatHistories(context: Context, onResult: (List<ChatHistory>) -> Unit) {
    // Implementation for loading chat histories
    onResult(emptyList()) // Return empty list for now
}

fun deleteChat(context: Context, id: Long, onResult: (Boolean, String) -> Unit) {
    // Implementation for deleting a chat
}

fun exportChat(context: Context, messages: List<Message>) {
    // Implementation for exporting chat
}