package com.jingqu.visitor.ui.screens

import android.content.Context
import android.media.MediaPlayer
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import android.util.Log
import com.jingqu.visitor.BuildConfig
import com.jingqu.visitor.data.api.StreamingChatClient
import com.jingqu.visitor.data.api.WebSocketClient
import com.jingqu.visitor.data.model.ChatMessage
import com.jingqu.visitor.data.model.KnowledgeItem
import com.jingqu.visitor.data.model.KnowledgeUpdate
import com.jingqu.visitor.data.model.Notification
import com.jingqu.visitor.data.model.PlaceEnrichRequest
import com.jingqu.visitor.data.model.PoiDetailData
import com.jingqu.visitor.data.model.QuickQuestion
import com.jingqu.visitor.data.model.ReviewCard
import com.jingqu.visitor.domain.usecase.ChatUseCase
import com.live2d.demo.full.LAppDelegate
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.io.FileOutputStream
import java.util.Locale
import java.util.concurrent.TimeUnit
import javax.inject.Inject

data class ChatUiState(
    val messages: List<ChatMessage> = emptyList(),
    val isConnected: Boolean = false,
    val isConnecting: Boolean = true,
    val connectionError: String? = null,
    val currentNotification: Notification? = null,
    val isTyping: Boolean = false,
    val currentScenicSpot: String = "景区入口",
    val quickQuestions: List<QuickQuestion> = defaultQuickQuestions,
    val routeDataJson: String? = null,
    val routeMode: String? = null,
    val isSpeaking: Boolean = false,
    val isMuted: Boolean = false,
    // POI detail state
    val selectedPoiData: PoiDetailData? = null,
    val isShowingPlaceDetail: Boolean = false,
    val aiDescription: String? = null,
    val aiReviews: List<ReviewCard> = emptyList(),
    val isLoadingAiEnrich: Boolean = false
) {
    companion object {
        val defaultQuickQuestions = listOf(
            QuickQuestion(1, "路线规划", "route", "请为我规划一份详细的北京一日游路线，包括上午和下午的景点安排、交通方式和美食推荐"),
            QuickQuestion(2, "景点讲解", "spot", "请详细介绍一下这里的著名景点，包括历史背景和游览建议"),
            QuickQuestion(3, "餐饮推荐", "restaurant", "附近有什么特色餐厅推荐？请具体到店名和招牌菜"),
            QuickQuestion(4, "交通指引", "transport", "各个景点之间怎么走最方便？推荐交通方式")
        )
    }
}

@HiltViewModel
class MainViewModel @Inject constructor(
    private val chatUseCase: ChatUseCase,
    @ApplicationContext private val appContext: Context
) : ViewModel() {

    companion object { var routeCache: String? = null }

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    // 豆包 TTS via backend. Separate OkHttpClient (trusts self-signed cert) to avoid
    // connection-pool contention with the SSE streaming client.
    private val ttsHttpClient by lazy {
        val tm = arrayOf<javax.net.ssl.TrustManager>(object : javax.net.ssl.X509TrustManager {
            override fun checkClientTrusted(c: Array<java.security.cert.X509Certificate>, a: String) {}
            override fun checkServerTrusted(c: Array<java.security.cert.X509Certificate>, a: String) {}
            override fun getAcceptedIssuers(): Array<java.security.cert.X509Certificate> = arrayOf()
        })
        val ssl = javax.net.ssl.SSLContext.getInstance("TLS").apply { init(null, tm, java.security.SecureRandom()) }
        OkHttpClient.Builder()
            .sslSocketFactory(ssl.socketFactory, tm[0] as javax.net.ssl.X509TrustManager)
            .hostnameVerifier { _, _ -> true }
            .connectTimeout(5, TimeUnit.SECONDS).readTimeout(30, TimeUnit.SECONDS).build()
    }
    private var currentMediaPlayer: MediaPlayer? = null
    private var mouthAnimating = false
    private var isFirstDelta = false

    init {
        observeConnectionState()
        observeMessages()
        observeNotifications()
        observeKnowledgeUpdates()
        observeRouteData()
        connectToServer()
    }

    fun toggleMute() {
        _uiState.update { it.copy(isMuted = !it.isMuted) }
        if (_uiState.value.isMuted) stopSpeaking()
    }

    /** 调豆包 TTS → 播放音频 + 嘴部动画 */
    private fun speak(text: String) {
        if (text.isBlank() || _uiState.value.isMuted) return
        val ttsText = if (text.length > 500) text.take(500) else text
        Log.d("TTS", "speak called, textLen=${text.length}, ttsLen=${ttsText.length}")
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isSpeaking = true) }
                startMouthAnimation()
                val audioBytes = withContext(Dispatchers.IO) {
                    val json = """{"text":${com.google.gson.Gson().toJson(ttsText)}}"""
                    val body = json.toRequestBody("application/json".toMediaType())
                    val url = BuildConfig.BASE_URL + "api/tts/speak"
                    Log.d("TTS", "Requesting: $url")
                    val req = Request.Builder().url(url).post(body).build()
                    val resp = ttsHttpClient.newCall(req).execute()
                    Log.d("TTS", "Response: code=${resp.code}")
                    if (resp.isSuccessful) resp.body?.bytes() else null
                }
                if (audioBytes == null || audioBytes.size < 100) {
                    Log.e("TTS", "No audio data (size=${audioBytes?.size ?: 0})")
                    stopSpeaking(); return@launch
                }
                Log.d("TTS", "Got audio: ${audioBytes.size} bytes")
                val file = File(appContext.cacheDir, "tts_${System.currentTimeMillis()}.mp3")
                FileOutputStream(file).use { it.write(audioBytes) }
                withContext(Dispatchers.Main) {
                    currentMediaPlayer?.release()
                    currentMediaPlayer = MediaPlayer().apply {
                        setAudioStreamType(android.media.AudioManager.STREAM_MUSIC)
                        setDataSource(file.absolutePath)
                        setOnCompletionListener { Log.d("TTS", "Playback completed"); stopSpeaking(); file.delete() }
                        setOnErrorListener { _, what, extra -> Log.e("TTS", "MediaPlayer error: $what/$extra"); stopSpeaking(); file.delete(); true }
                        prepare(); start()
                        Log.d("TTS", "MediaPlayer started, duration=${duration}ms")
                    }
                }
            } catch (e: Exception) {
                Log.e("TTS", "exception: ${e.message}", e)
                stopSpeaking()
            }
        }
    }

    private fun stopSpeaking() {
        currentMediaPlayer?.let { try { it.stop() } catch (_: Exception) {}; it.release() }
        currentMediaPlayer = null
        LAppDelegate.setLipSync(0.0f); mouthAnimating = false
        _uiState.update { it.copy(isSpeaking = false) }
    }

    private fun startMouthAnimation() {
        if (mouthAnimating) return
        mouthAnimating = true
        viewModelScope.launch {
            var open = true
            while (mouthAnimating) {
                LAppDelegate.setLipSync(if (open) 0.6f else 0.0f)
                open = !open; delay(120)
            }
            LAppDelegate.setLipSync(0.0f)
        }
    }

    /** Live2D: play random expression + motion when AI starts responding */
    private fun triggerLive2DAnimation() {
        LAppDelegate.playRandomExpression()
        LAppDelegate.playRandomMotion()
    }

    private fun observeRouteData() {
        viewModelScope.launch {
            chatUseCase.routeData.collect { (json, mode) ->
                _uiState.update { it.copy(routeDataJson = json, routeMode = mode) }
                routeCache = json
            }
        }
    }

    private fun observeConnectionState() {
        // REST API模式：直接标记已连接
        _uiState.update { it.copy(isConnected = true, isConnecting = false) }
        addWelcomeMessage()
    }

    private fun observeMessages() {
        viewModelScope.launch {
            chatUseCase.messages.collect { message ->
                _uiState.update {
                    it.copy(
                        messages = it.messages + message,
                        isTyping = false
                    )
                }
            }
        }
    }

    private fun observeNotifications() {
        viewModelScope.launch {
            chatUseCase.notifications.collect { notification ->
                _uiState.update {
                    it.copy(currentNotification = notification)
                }
            }
        }
    }

    private fun observeKnowledgeUpdates() {
        viewModelScope.launch {
            chatUseCase.knowledgeUpdates.collect { update ->
                val detail = when {
                    update.action.isNotBlank() -> "知识库已更新：${update.action}"
                    else -> "知识库已更新，请刷新页面获取最新信息。"
                }
                val refreshed = chatUseCase.refreshKnowledgeCache()
                val refreshedNotice = if (refreshed.isNotEmpty()) "已同步 ${refreshed.size} 条最新知识。" else "已触发最新知识同步。"
                _uiState.update {
                    it.copy(
                        messages = it.messages + ChatMessage(
                            content = "$detail\n$refreshedNotice",
                            isFromUser = false,
                            type = "SYSTEM"
                        ),
                        connectionError = null
                    )
                }
            }
        }
    }

    private fun addWelcomeMessage() {
        if (_uiState.value.messages.isEmpty()) {
            val welcomeMessage = ChatMessage(
                content = "您好！欢迎来到景区导览服务！\n\n我是您的智能导览助手。您可以向我咨询以下信息：\n\n• 景点开放时间和门票价格\n• 景区地图和游览路线\n• 餐厅、停车场等设施位置\n• 紧急求助和服务投诉\n\n请输入您想了解的问题，我会尽力为您解答！",
                isFromUser = false,
                type = "WELCOME"
            )
            _uiState.update {
                it.copy(messages = listOf(welcomeMessage))
            }
        }
    }

    private fun connectToServer() {
        viewModelScope.launch {
            chatUseCase.connect(_uiState.value.currentScenicSpot)
        }
    }

    fun sendMessage(content: String) {
        if (content.isBlank()) return

        val spot = _uiState.value.currentScenicSpot
        val fullContent = if (spot != "景区入口" && !content.contains(spot)) "在${spot}，$content" else content

        val userMessage = ChatMessage(
            content = fullContent,
            isFromUser = true
        )

        // 预先插入一条空的 AI 气泡，流式增量往里追加
        val streamingId = "ai-" + System.currentTimeMillis()
        val placeholder = ChatMessage(
            id = streamingId,
            content = "",
            isFromUser = false,
            type = "TEXT"
        )

        _uiState.update {
            it.copy(
                messages = it.messages + userMessage + placeholder,
                isTyping = true
            )
        }

        viewModelScope.launch {
            val builder = StringBuilder()
            var deltaCount = 0
            var lastUiUpdate = 0L
            var spoken = false
            isFirstDelta = true
            chatUseCase.streamMessage(content, spot).collect { event ->
                when (event) {
                    is StreamingChatClient.StreamEvent.Delta -> {
                        deltaCount++
                        builder.append(event.text)
                        val now = System.currentTimeMillis()
                        if (now - lastUiUpdate > 50 || builder.length < 10) {
                            updateStreamingMessage(streamingId, builder.toString())
                            lastUiUpdate = now
                        }
                        if (deltaCount <= 3 || deltaCount % 500 == 0) {
                            Log.d("StreamVM", "delta #$deltaCount, total=${builder.length}c")
                        }
                        // Trigger Live2D animation on first delta
                        if (isFirstDelta) {
                            isFirstDelta = false
                            triggerLive2DAnimation()
                            _uiState.update { it.copy(isSpeaking = true) }
                        }
                        if (_uiState.value.isTyping) _uiState.update { it.copy(isTyping = false) }
                    }
                    is StreamingChatClient.StreamEvent.Routes -> {
                        Log.d("StreamVM", "routes received: ${event.mode}")
                        chatUseCase.publishRouteData(event.dailyRoutes, event.mode)
                        _uiState.update { it.copy(routeDataJson = event.dailyRoutes, routeMode = event.mode) }
                        routeCache = event.dailyRoutes
                    }
                    is StreamingChatClient.StreamEvent.Done -> {
                        Log.d("StreamVM", "done: builder.length=${builder.length}c, deltaCount=$deltaCount")
                        if (builder.isBlank()) {
                            updateStreamingMessage(streamingId, "暂时没有获取到回答，请稍后重试。")
                            _uiState.update { it.copy(isTyping = false) }
                        } else {
                            updateStreamingMessage(streamingId, builder.toString())
                            _uiState.update { it.copy(isTyping = false) }
                            // Speak full response via TTS (guard against duplicate Done events)
                            if (!spoken) {
                                spoken = true
                                speak(builder.toString())
                            }
                        }
                    }
                    is StreamingChatClient.StreamEvent.Error -> {
                        Log.e("StreamVM", "error: ${event.message}")
                        updateStreamingMessage(streamingId, event.message, type = "ERROR")
                        _uiState.update { it.copy(isTyping = false) }
                    }
                }
            }
            Log.d("StreamVM", "stream collection ended, builder.length=${builder.length}c")
            updateStreamingMessage(streamingId, builder.toString())
            _uiState.update { it.copy(isTyping = false) }
        }
    }

    private fun updateStreamingMessage(id: String, content: String, type: String = "TEXT") {
        _uiState.update { state ->
            state.copy(messages = state.messages.map { msg ->
                if (msg.id == id) msg.copy(content = content, type = type) else msg
            })
        }
    }

    fun sendQuickQuestion(question: String) {
        sendMessage(question)
    }

    fun dismissNotification() {
        _uiState.update {
            it.copy(currentNotification = null)
        }
    }

    fun reconnect() {
        _uiState.update {
            it.copy(isConnecting = true, connectionError = null)
        }
        connectToServer()
    }

    fun updateScenicSpot(scenicSpot: String) {
        _uiState.update { it.copy(currentScenicSpot = scenicSpot) }
    }

    // === POI Detail ===

    fun selectPoi(poi: PoiDetailData) {
        _uiState.update {
            it.copy(
                selectedPoiData = poi,
                isShowingPlaceDetail = true,
                aiDescription = null,
                aiReviews = emptyList(),
                isLoadingAiEnrich = false
            )
        }
    }

    fun dismissPoiDetail() {
        _uiState.update {
            it.copy(
                isShowingPlaceDetail = false,
                selectedPoiData = null,
                aiDescription = null,
                aiReviews = emptyList()
            )
        }
    }

    fun loadAiEnrich() {
        val poi = _uiState.value.selectedPoiData ?: return
        _uiState.update { it.copy(isLoadingAiEnrich = true) }
        viewModelScope.launch {
            try {
                val response = chatUseCase.enrichPlace(poi.name, poi.city)
                _uiState.update {
                    it.copy(
                        aiDescription = response.aiDescription,
                        aiReviews = response.aiReviews,
                        isLoadingAiEnrich = false
                    )
                }
            } catch (e: Exception) {
                Log.e("MainViewModel", "AI enrich failed: ${e.message}")
                _uiState.update {
                    it.copy(
                        aiDescription = "AI讲解暂时不可用，请稍后重试。",
                        isLoadingAiEnrich = false
                    )
                }
            }
        }
    }

    /**
     * Load AI enrichment with callback (used by MapRouteScreen which has local POI state)
     */
    fun loadAiEnrichForPoi(poi: PoiDetailData, onResult: (desc: String?, reviews: List<ReviewCard>) -> Unit) {
        viewModelScope.launch {
            try {
                val response = chatUseCase.enrichPlace(poi.name, poi.city)
                onResult(response.aiDescription, response.aiReviews)
            } catch (e: Exception) {
                Log.e("MainViewModel", "AI enrich failed: ${e.message}")
                onResult("AI讲解暂时不可用，请稍后重试。", emptyList())
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        stopSpeaking()
        chatUseCase.disconnect()
    }
}
