package com.jingqu.visitor.data.api

import com.jingqu.visitor.data.model.ApiResponse
import com.jingqu.visitor.data.model.AIReply
import com.jingqu.visitor.data.model.KnowledgeItem
import com.jingqu.visitor.data.model.VisitorMessage
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface ApiService {

    @GET("api/visitor/welcome")
    suspend fun getWelcomeMessage(): Response<ApiResponse<String>>

    @POST("api/visitor/message")
    suspend fun sendMessage(@Body message: VisitorMessage): Response<ApiResponse<String>>

    @GET("api/knowledge/all")
    suspend fun getActiveKnowledge(): Response<ApiResponse<List<KnowledgeItem>>>
}
