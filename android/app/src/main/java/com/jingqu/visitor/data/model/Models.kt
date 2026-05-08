package com.jingqu.visitor.data.model

import com.google.gson.annotations.SerializedName

data class ApiResponse<T>(
    @SerializedName("code") val code: Int,
    @SerializedName("message") val message: String,
    @SerializedName("data") val data: T?,
    @SerializedName("timestamp") val timestamp: String?
)

data class ChatMessage(
    val id: String = System.currentTimeMillis().toString(),
    val content: String,
    val isFromUser: Boolean,
    val timestamp: Long = System.currentTimeMillis(),
    val type: String = "TEXT"
)

data class VisitorMessage(
    @SerializedName("visitorId") val visitorId: String,
    @SerializedName("sessionId") val sessionId: String,
    @SerializedName("message") val message: String,
    @SerializedName("scenicSpot") val scenicSpot: String?,
    @SerializedName("timestamp") val timestamp: String
)

data class AIReply(
    @SerializedName("visitorId") val visitorId: String,
    @SerializedName("answer") val answer: String,
    @SerializedName("sessionId") val sessionId: String,
    @SerializedName("messageType") val messageType: String,
    @SerializedName("timestamp") val timestamp: String,
    @SerializedName("scenicSpot") val scenicSpot: String?
)

data class WebSocketMessage(
    @SerializedName("type") val type: String,
    @SerializedName("target") val target: String?,
    @SerializedName("payload") val payload: Any?,
    @SerializedName("timestamp") val timestamp: String?
)

data class Notification(
    @SerializedName("notificationId") val notificationId: Long,
    @SerializedName("title") val title: String,
    @SerializedName("content") val content: String,
    @SerializedName("type") val type: String,
    @SerializedName("timestamp") val timestamp: String?
)

data class KnowledgeUpdate(
    @SerializedName("action") val action: String,
    @SerializedName("updatedData") val updatedData: Any?
)

data class KnowledgeItem(
    @SerializedName("id") val id: Long? = null,
    @SerializedName("questionPattern") val questionPattern: String,
    @SerializedName("answer") val answer: String,
    @SerializedName("keywords") val keywords: String,
    @SerializedName("category") val category: String,
    @SerializedName("priority") val priority: Int,
    @SerializedName("status") val status: Int
)

data class QuickQuestion(
    val id: Int,
    val title: String,
    val icon: String,
    val question: String
)
