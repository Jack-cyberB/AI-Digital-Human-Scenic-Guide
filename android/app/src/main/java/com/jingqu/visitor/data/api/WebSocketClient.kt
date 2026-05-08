package com.jingqu.visitor.data.api

import android.util.Log
import com.google.gson.Gson
import com.jingqu.visitor.BuildConfig
import com.jingqu.visitor.data.model.AIReply
import com.jingqu.visitor.data.model.ChatMessage
import com.jingqu.visitor.data.model.KnowledgeUpdate
import com.jingqu.visitor.data.model.Notification
import com.jingqu.visitor.data.model.WebSocketMessage
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import okhttp3.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WebSocketClient @Inject constructor() {

    private var webSocket: WebSocket? = null
    private val client = OkHttpClient.Builder()
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .pingInterval(15, TimeUnit.SECONDS)
        .build()

    private val gson = Gson()

    private val _connectionState = Channel<ConnectionState>(Channel.BUFFERED)
    val connectionState: Flow<ConnectionState> = _connectionState.receiveAsFlow()

    private val _messages = Channel<ChatMessage>(Channel.BUFFERED)
    val messages: Flow<ChatMessage> = _messages.receiveAsFlow()

    private val _notifications = Channel<Notification>(Channel.BUFFERED)
    val notifications: Flow<Notification> = _notifications.receiveAsFlow()

    private val _knowledgeUpdates = Channel<KnowledgeUpdate>(Channel.BUFFERED)
    val knowledgeUpdates: Flow<KnowledgeUpdate> = _knowledgeUpdates.receiveAsFlow()

    private var visitorId: String = ""
    private var sessionId: String = ""
    private var scenicSpot: String = "景区入口"

    sealed class ConnectionState {
        object Connected : ConnectionState()
        object Disconnected : ConnectionState()
        data class Error(val message: String) : ConnectionState()
    }

    fun connect(visitorId: String, scenicSpot: String? = null) {
        this.visitorId = visitorId
        this.sessionId = java.util.UUID.randomUUID().toString()
        this.scenicSpot = scenicSpot ?: "景区入口"

        val request = Request.Builder()
            .url(BuildConfig.WS_URL)
            .build()

        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                Log.d(TAG, "WebSocket connected")
                _connectionState.trySend(ConnectionState.Connected)

                val connectMessage = WebSocketMessage(
                    type = "VISITOR_CONNECT",
                    target = null,
                    payload = mapOf(
                        "visitorId" to visitorId,
                        "sessionId" to sessionId,
                        "scenicSpot" to scenicSpot.orEmpty().ifBlank { this@WebSocketClient.scenicSpot }
                    ),
                    timestamp = currentTimestamp()
                )
                sendMessage(connectMessage)
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                Log.d(TAG, "WebSocket message received: $text")
                handleMessage(text)
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                Log.d(TAG, "WebSocket closing: $code $reason")
                webSocket.close(1000, null)
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                Log.d(TAG, "WebSocket closed: $code $reason")
                _connectionState.trySend(ConnectionState.Disconnected)
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Log.e(TAG, "WebSocket failure", t)
                _connectionState.trySend(ConnectionState.Error(t.message ?: "Unknown error"))
            }
        })
    }

    private fun handleMessage(text: String) {
        try {
            val message = gson.fromJson(text, WebSocketMessage::class.java)

            when (message.type) {
                "AI_RESPONSE" -> {
                    val reply = gson.fromJson(gson.toJson(message.payload), AIReply::class.java)
                    val chatMessage = ChatMessage(
                        content = reply.answer,
                        isFromUser = false,
                        type = reply.messageType
                    )
                    _messages.trySend(chatMessage)
                }

                "NOTIFICATION" -> {
                    val notification = gson.fromJson(gson.toJson(message.payload), Notification::class.java)
                    _notifications.trySend(notification)
                }

                "KNOWLEDGE_UPDATE" -> {
                    val update = gson.fromJson(gson.toJson(message.payload), KnowledgeUpdate::class.java)
                    _knowledgeUpdates.trySend(update)
                }

                "HEARTBEAT" -> {
                    Log.d(TAG, "Heartbeat received")
                }

                "ERROR" -> {
                    val chatMessage = ChatMessage(
                        content = message.payload?.toString() ?: "发生错误",
                        isFromUser = false,
                        type = "ERROR"
                    )
                    _messages.trySend(chatMessage)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing message", e)
        }
    }

    fun sendChatMessage(content: String, scenicSpot: String = "景区入口") {
        val message = WebSocketMessage(
            type = "VISITOR_MESSAGE",
            target = null,
            payload = mapOf(
                "visitorId" to visitorId,
                "sessionId" to sessionId,
                "message" to content,
                "scenicSpot" to scenicSpot
            ),
            timestamp = currentTimestamp()
        )
        sendMessage(message)
    }

    fun emitLocalReply(content: String) {
        _messages.trySend(
            ChatMessage(
                content = content,
                isFromUser = false,
                type = "TEXT"
            )
        )
    }

    fun emitLocalError(content: String) {
        _messages.trySend(
            ChatMessage(
                content = content,
                isFromUser = false,
                type = "ERROR"
            )
        )
    }

    private fun sendMessage(message: WebSocketMessage) {
        val json = gson.toJson(message)
        Log.d(TAG, "Sending message: $json")
        webSocket?.send(json)
    }

    fun disconnect() {
        webSocket?.close(1000, "User disconnected")
        webSocket = null
    }

    fun isConnected(): Boolean {
        return webSocket != null
    }

    private fun currentTimestamp(): String {
        return SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault()).format(Date())
    }

    companion object {
        private const val TAG = "WebSocketClient"
    }
}
