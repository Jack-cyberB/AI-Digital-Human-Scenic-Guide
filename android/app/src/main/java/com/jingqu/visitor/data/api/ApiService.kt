package com.jingqu.visitor.data.api

import com.jingqu.visitor.data.model.ApiResponse
import com.jingqu.visitor.data.model.KnowledgeItem
import com.jingqu.visitor.data.model.PlaceDetailDTO
import com.jingqu.visitor.data.model.PlaceEnrichRequest
import com.jingqu.visitor.data.model.PlaceEnrichResponse
import com.jingqu.visitor.data.model.RagFlowChatResponse
import com.jingqu.visitor.data.model.VisitorMessage
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

interface ApiService {

    @GET("api/visitor/welcome")
    suspend fun getWelcomeMessage(): Response<ApiResponse<String>>

    @POST("api/ragflow/chat")
    suspend fun sendMessage(@Body message: VisitorMessage): Response<ApiResponse<RagFlowChatResponse>>

    @GET("api/knowledge/all")
    suspend fun getActiveKnowledge(): Response<ApiResponse<List<KnowledgeItem>>>

    @POST("api/place/enrich")
    suspend fun enrichPlace(@Body request: PlaceEnrichRequest): Response<ApiResponse<PlaceEnrichResponse>>
}
