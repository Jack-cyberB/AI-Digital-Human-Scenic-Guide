package com.jingqu.visitor.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jingqu.visitor.data.api.WebSocketClient
import com.jingqu.visitor.data.model.ChatMessage
import com.jingqu.visitor.data.model.KnowledgeItem
import com.jingqu.visitor.data.model.KnowledgeUpdate
import com.jingqu.visitor.data.model.Notification
import com.jingqu.visitor.data.model.QuickQuestion
import com.jingqu.visitor.domain.usecase.ChatUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
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
    val routeMode: String? = null
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
    private val chatUseCase: ChatUseCase
) : ViewModel() {

    companion object { var routeCache: String? = null }

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    init {
        observeConnectionState()
        observeMessages()
        observeNotifications()
        observeKnowledgeUpdates()
        observeRouteData()
        connectToServer()
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

        _uiState.update {
            it.copy(
                messages = it.messages + userMessage,
                isTyping = true
            )
        }

        viewModelScope.launch {
            chatUseCase.sendMessage(content, _uiState.value.currentScenicSpot)
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

    override fun onCleared() {
        super.onCleared()
        chatUseCase.disconnect()
    }
}
