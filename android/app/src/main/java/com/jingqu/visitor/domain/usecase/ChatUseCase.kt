package com.jingqu.visitor.domain.usecase

import com.jingqu.visitor.data.api.ApiService
import com.jingqu.visitor.data.api.WebSocketClient
import com.jingqu.visitor.data.model.ChatMessage
import com.jingqu.visitor.data.model.KnowledgeItem
import com.jingqu.visitor.data.model.KnowledgeUpdate
import com.jingqu.visitor.data.model.Notification
import com.jingqu.visitor.data.repository.PreferencesRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ChatUseCase @Inject constructor(
    private val webSocketClient: WebSocketClient,
    private val preferencesRepository: PreferencesRepository,
    private val apiService: ApiService
) {
    val connectionState: Flow<WebSocketClient.ConnectionState> = webSocketClient.connectionState
    val messages: Flow<ChatMessage> = webSocketClient.messages
    val notifications: Flow<Notification> = webSocketClient.notifications
    val knowledgeUpdates: Flow<KnowledgeUpdate> = webSocketClient.knowledgeUpdates

    suspend fun connect() {
        val visitorId = preferencesRepository.getVisitorId()
        webSocketClient.connect(visitorId)
    }

    suspend fun connect(scenicSpot: String) {
        val visitorId = preferencesRepository.getVisitorId()
        webSocketClient.connect(visitorId, scenicSpot)
    }

    suspend fun sendMessage(content: String, scenicSpot: String = "景区入口") {
        if (webSocketClient.isConnected()) {
            webSocketClient.sendChatMessage(content, scenicSpot)
            return
        }

        val visitorId = preferencesRepository.getVisitorId()
        val sessionId = preferencesRepository.getSessionId()
        val response = apiService.sendMessage(
            com.jingqu.visitor.data.model.VisitorMessage(
                visitorId = visitorId,
                sessionId = sessionId,
                message = content,
                scenicSpot = scenicSpot,
                timestamp = java.time.LocalDateTime.now().toString()
            )
        )

        if (response.isSuccessful) {
            val body = response.body()?.data
            if (!body.isNullOrBlank()) {
                webSocketClient.emitLocalReply(body)
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

    fun disconnect() {
        webSocketClient.disconnect()
    }

    fun isConnected(): Boolean {
        return webSocketClient.isConnected()
    }
}
