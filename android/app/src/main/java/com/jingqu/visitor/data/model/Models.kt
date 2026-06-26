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

data class RagFlowChatResponse(
    @SerializedName("answer") val answer: String,
    @SerializedName("emotion") val emotion: String? = null,
    @SerializedName("action") val action: String? = null,
    @SerializedName("avatarTarget") val avatarTarget: String? = null,
    @SerializedName("dailyRoutes") val dailyRoutes: String? = null,
    @SerializedName("mode") val mode: String? = null,
    @SerializedName("sessionId") val sessionId: String? = null,
    @SerializedName("visitorId") val visitorId: String? = null,
    @SerializedName("scenicSpot") val scenicSpot: String? = null,
    @SerializedName("citations") val citations: List<String>? = null,
    @SerializedName("source") val source: String? = null,
    @SerializedName("timestamp") val timestamp: String? = null
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

// === POI / Place models ===

data class PoiDetailData(
    val name: String = "",
    val address: String = "",
    val city: String = "",
    val lat: Double = 0.0,
    val lng: Double = 0.0,
    val category: String = "",       // 景点/美食/饮品/购物/住宿
    val categoryTag: String = "",    // Amap type e.g. "风景名胜"
    val rating: Double? = null,
    val reviewCount: Int? = null,
    val openingHours: String = "",
    val phone: String = "",
    val cost: String = "",
    val photos: List<String> = emptyList(),
    val aiDescription: String? = null,
    val aiReviews: List<ReviewCard> = emptyList()
)

data class ReviewCard(
    val userName: String = "",
    val avatarUrl: String = "",
    val content: String = "",
    val likeCount: Int = 0,
    val rating: Int = 5  // 1-5
)

enum class PoiCategory(
    val label: String,
    val amapType: String,
    val emoji: String,
    val color: Long
) {
    SCENIC("景点", "风景名胜|公园广场|风景名胜相关", "🏔", 0xFFFF6B35),  // 🏔
    FOOD("美食", "餐饮服务|中餐厅|异国餐厅|小吃快餐", "🍜", 0xFFFF4757),   // 🍜
    DRINK("饮品", "茶艺馆|咖啡馆|冷饮店|糕饼店", "🥤", 0xFF2ED573),      // 🥤
    SHOPPING("购物", "购物服务|综合商场|特色商街", "🛍", 0xFF1E90FF),     // 🛍
    HOTEL("住宿", "住宿服务|宾馆酒店|旅馆招待所", "🏨", 0xFF9B59B6)       // 🏨
}

data class PlaceEnrichRequest(
    val keyword: String,
    val city: String
)

data class PlaceEnrichResponse(
    val aiDescription: String? = null,
    val aiReviews: List<ReviewCard> = emptyList()
)

/**
 * Matches backend PlaceDetailDTO response shape
 */
data class PlaceDetailDTO(
    val name: String = "",
    val address: String = "",
    val city: String = "",
    val lat: Double? = null,
    val lng: Double? = null,
    val category: String? = null,
    val categoryTag: String? = null,
    val rating: Double? = null,
    val reviewCount: Int? = null,
    val openingHours: String? = null,
    val phone: String? = null,
    val cost: String? = null,
    val photos: List<String>? = null,
    val aiDescription: String? = null,
    val aiReviews: List<ReviewCard>? = null
)
