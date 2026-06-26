package com.jingqu.controller;

import com.jingqu.dto.RagFlowChatRequest;
import com.jingqu.dto.RagFlowChatResponse;
import com.jingqu.dto.ResponseDTO;
import com.jingqu.service.RagFlowService;
import com.jingqu.service.WebSocketService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@RestController
@RequestMapping("/api/ragflow")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class RagFlowController {
    private final RagFlowService ragFlowService;
    private final WebSocketService webSocketService;
    private final ExecutorService streamExecutor = Executors.newCachedThreadPool();

    @PostMapping("/chat")
    public ResponseDTO<RagFlowChatResponse> chat(@RequestBody RagFlowChatRequest request) {
        RagFlowChatResponse response = ragFlowService.chat(request);
        webSocketService.broadcastRagFlowUpdate(response);
        return ResponseDTO.success(response);
    }

    /**
     * 流式对话：通过 SSE 边生成边推送。
     * 事件类型：
     *   delta  - 正文增量片段 {"text":"..."}
     *   routes - 路线数据 {"dailyRoutes":"...","mode":"..."}
     *   done   - 结束
     *   error  - 出错 {"message":"..."}
     */
    @PostMapping(value = "/chat/stream", produces = "text/event-stream;charset=UTF-8")
    public SseEmitter chatStream(@RequestBody RagFlowChatRequest request) {
        SseEmitter emitter = new SseEmitter(180_000L);
        AtomicBoolean active = new AtomicBoolean(true);
        emitter.onCompletion(() -> active.set(false));
        emitter.onTimeout(() -> active.set(false));
        emitter.onError(e -> active.set(false));

        streamExecutor.execute(() -> {
            try {
                String answer = ragFlowService.streamDeepSeek(request, delta -> {
                    if (!active.get()) return;
                    try {
                        Map<String, String> payload = new HashMap<>();
                        payload.put("text", delta);
                        emitter.send(SseEmitter.event().name("delta").data(payload));
                    } catch (Exception sendEx) {
                        active.set(false);
                    }
                }, active);

                // 如果客户端已断开，不再尝试后续操作
                if (!active.get()) return;

                // 正文流完后，后台提取路线（不阻塞正文显示）
                if (answer != null && !answer.isBlank()) {
                    RagFlowChatResponse full = ragFlowService.buildRouteResponse(request, answer);
                    if (full.getDailyRoutes() != null && !full.getDailyRoutes().isBlank()) {
                        try {
                            Map<String, String> routePayload = new HashMap<>();
                            routePayload.put("dailyRoutes", full.getDailyRoutes());
                            routePayload.put("mode", full.getMode() != null ? full.getMode() : "city");
                            emitter.send(SseEmitter.event().name("routes").data(routePayload));
                        } catch (Exception ignored) {}
                    }
                    webSocketService.broadcastRagFlowUpdate(full);
                }
                try { emitter.send(SseEmitter.event().name("done").data("{}")); } catch (Exception ignored) {}
                emitter.complete();
            } catch (Exception e) {
                log.error("流式对话失败", e);
                try {
                    Map<String, String> err = new HashMap<>();
                    err.put("message", "AI服务暂时不可用，请稍后重试。");
                    emitter.send(SseEmitter.event().name("error").data(err));
                    emitter.complete();
                } catch (Exception ignored) {
                    try { emitter.completeWithError(e); } catch (Exception ignored2) {}
                }
            }
        });
        return emitter;
    }
}
