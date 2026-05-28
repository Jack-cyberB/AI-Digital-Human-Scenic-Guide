package com.jingqu.controller;

import com.jingqu.dto.RagFlowChatRequest;
import com.jingqu.dto.RagFlowChatResponse;
import com.jingqu.dto.ResponseDTO;
import com.jingqu.service.RagFlowService;
import com.jingqu.service.WebSocketService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/ragflow")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class RagFlowController {
    private final RagFlowService ragFlowService;
    private final WebSocketService webSocketService;

    @PostMapping("/chat")
    public ResponseDTO<RagFlowChatResponse> chat(@RequestBody RagFlowChatRequest request) {
        RagFlowChatResponse response = ragFlowService.chat(request);
        webSocketService.broadcastRagFlowUpdate(response);
        return ResponseDTO.success(response);
    }
}
