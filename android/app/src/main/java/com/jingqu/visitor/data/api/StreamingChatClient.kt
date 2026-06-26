package com.jingqu.visitor.data.api

import android.util.Log
import com.google.gson.Gson
import com.jingqu.visitor.BuildConfig
import com.jingqu.visitor.data.model.VisitorMessage
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.sse.EventSource
import okhttp3.sse.EventSourceListener
import okhttp3.sse.EventSources
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 通过 SSE 消费后端 /api/ragflow/chat/stream 流式接口。
 * 事件: delta(正文增量) / routes(路线数据) / done(结束) / error(出错)
 */
@Singleton
class StreamingChatClient @Inject constructor(
    private val okHttpClient: OkHttpClient
) {
    private val gson = Gson()

    sealed class StreamEvent {
        data class Delta(val text: String) : StreamEvent()
        data class Routes(val dailyRoutes: String, val mode: String) : StreamEvent()
        object Done : StreamEvent()
        data class Error(val message: String) : StreamEvent()
    }

    private data class DeltaPayload(val text: String?)
    private data class RoutesPayload(val dailyRoutes: String?, val mode: String?)
    private data class ErrorPayload(val message: String?)

    fun stream(message: VisitorMessage): Flow<StreamEvent> = callbackFlow {
        val json = gson.toJson(message)
        val url = BuildConfig.BASE_URL + "api/ragflow/chat/stream"
        Log.d(TAG, "Starting SSE stream to $url")
        val request = Request.Builder()
            .url(url)
            .header("Accept", "text/event-stream")
            .post(json.toRequestBody("application/json; charset=utf-8".toMediaType()))
            .build()

        var eventCount = 0
        val listener = object : EventSourceListener() {
            override fun onEvent(eventSource: EventSource, id: String?, type: String?, data: String) {
                eventCount++
                if (eventCount <= 3 || eventCount % 200 == 0) {
                    Log.d(TAG, "SSE event #$eventCount type=$type")
                }
                when (type) {
                    "delta" -> {
                        val p = runCatching { gson.fromJson(data, DeltaPayload::class.java) }.getOrNull()
                        p?.text?.let { trySend(StreamEvent.Delta(it)) }
                    }
                    "routes" -> {
                        val p = runCatching { gson.fromJson(data, RoutesPayload::class.java) }.getOrNull()
                        if (p?.dailyRoutes != null) {
                            trySend(StreamEvent.Routes(p.dailyRoutes, p.mode ?: "city"))
                        }
                    }
                    "error" -> {
                        val p = runCatching { gson.fromJson(data, ErrorPayload::class.java) }.getOrNull()
                        trySend(StreamEvent.Error(p?.message ?: "AI服务暂时不可用，请稍后重试。"))
                    }
                    "done" -> {
                        trySend(StreamEvent.Done)
                    }
                }
            }

            override fun onClosed(eventSource: EventSource) {
                Log.d(TAG, "SSE closed by server, total events=$eventCount")
                trySend(StreamEvent.Done)
                close()
            }

            override fun onFailure(eventSource: EventSource, t: Throwable?, response: okhttp3.Response?) {
                Log.e(TAG, "SSE failure after $eventCount events, response=${response?.code}", t)
                trySend(StreamEvent.Error("连接中断，请稍后重试。"))
                close()
            }
        }

        val eventSource = EventSources.createFactory(okHttpClient).newEventSource(request, listener)
        awaitClose { eventSource.cancel() }
    }

    companion object {
        private const val TAG = "StreamingChatClient"
    }
}
