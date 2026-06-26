package com.jingqu.visitor.domain.usecase

import com.jingqu.visitor.data.api.ApiService
import com.jingqu.visitor.data.api.StreamingChatClient
import com.jingqu.visitor.data.api.WebSocketClient
import com.jingqu.visitor.data.model.ChatMessage
import com.jingqu.visitor.data.model.KnowledgeItem
import com.jingqu.visitor.data.model.KnowledgeUpdate
import com.jingqu.visitor.data.model.Notification
import com.jingqu.visitor.data.model.PlaceEnrichRequest
import com.jingqu.visitor.data.model.PlaceEnrichResponse
import com.jingqu.visitor.data.repository.PreferencesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.channels.Channel
import javax.inject.Inject

class ChatUseCase @Inject constructor(
    private val webSocketClient: WebSocketClient,
    private val preferencesRepository: PreferencesRepository,
    private val apiService: ApiService,
    private val streamingChatClient: StreamingChatClient
) {
    val connectionState: Flow<WebSocketClient.ConnectionState> = webSocketClient.connectionState
    val messages: Flow<ChatMessage> = webSocketClient.messages
    val notifications: Flow<Notification> = webSocketClient.notifications
    val knowledgeUpdates: Flow<KnowledgeUpdate> = webSocketClient.knowledgeUpdates

    private val _routeData = kotlinx.coroutines.channels.Channel<Pair<String, String>>(kotlinx.coroutines.channels.Channel.BUFFERED)
    val routeData: Flow<Pair<String, String>> = _routeData.receiveAsFlow()

    suspend fun connect() {
        val visitorId = preferencesRepository.getVisitorId()
        webSocketClient.connect(visitorId)
    }

    suspend fun connect(scenicSpot: String) {
        val visitorId = preferencesRepository.getVisitorId()
        webSocketClient.connect(visitorId, scenicSpot)
    }

    /**
     * 流式发送消息，返回流式事件供 ViewModel 边收边渲染。
     * 同时在收到 routes 事件时写入 routeData 流。
     */
    suspend fun streamMessage(content: String, scenicSpot: String = "景区入口"): Flow<StreamingChatClient.StreamEvent> {
        val message = com.jingqu.visitor.data.model.VisitorMessage(
            visitorId = preferencesRepository.getVisitorId(),
            sessionId = preferencesRepository.getSessionId(),
            message = content,
            scenicSpot = scenicSpot,
            timestamp = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())
        )
        return streamingChatClient.stream(message).buffer(Channel.UNLIMITED)
    }

    fun publishRouteData(dailyRoutes: String, mode: String) {
        _routeData.trySend(Pair(dailyRoutes, mode))
    }

    suspend fun sendMessage(content: String, scenicSpot: String = "景区入口") {
        val visitorId = preferencesRepository.getVisitorId()
        val sessionId = preferencesRepository.getSessionId()
        val response = apiService.sendMessage(
            com.jingqu.visitor.data.model.VisitorMessage(
                visitorId = visitorId,
                sessionId = sessionId,
                message = content,
                scenicSpot = scenicSpot,
                timestamp = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())
            )
        )

        if (response.isSuccessful) {
            val body = response.body()?.data
            if (body != null) {
                val content = body.answer.ifBlank { "暂时没有获取到回答，请稍后重试。" }
                // 如果有路线数据，发送到路线流
                if (!body.dailyRoutes.isNullOrBlank()) {
                    _routeData.trySend(Pair(body.dailyRoutes!!, body.mode ?: "city"))
                }
                webSocketClient.emitLocalReply(content, body.action, body.emotion)
            } else {
                webSocketClient.emitLocalError("发送失败，请稍后重试")
            }
        } else {
            webSocketClient.emitLocalError("发送失败，请稍后重试")
        }
    }

    suspend fun refreshKnowledgeCache(): List<KnowledgeItem> {
        val response = apiService.getActiveKnowledge()
        if (response.isSuccessful) {
            return response.body()?.data.orEmpty()
        }
        return emptyList()
    }

    suspend fun enrichPlace(keyword: String, city: String): PlaceEnrichResponse {
        val response = apiService.enrichPlace(PlaceEnrichRequest(keyword, city))
        if (response.isSuccessful) {
            val data = response.body()?.data
            if (data != null) return data
        }
        return PlaceEnrichResponse(aiDescription = "暂无详细介绍", aiReviews = emptyList())
    }

    fun disconnect() {
        webSocketClient.disconnect()
    }

    fun isConnected(): Boolean {
        return webSocketClient.isConnected()
    }
}
