package com.jingqu.controller;

import com.jingqu.dto.ResponseDTO;
import com.jingqu.dto.VisitorMessageRequest;
import com.jingqu.service.KnowledgeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/visitor")
@CrossOrigin(origins = "*")
public class VisitorController {

    @Autowired
    private KnowledgeService knowledgeService;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    /**
     * 处理游客消息（HTTP接口，作为WebSocket的备份）
     */
    @PostMapping("/message")
    public ResponseDTO<String> sendMessage(@RequestBody VisitorMessageRequest request) {
        String answer = knowledgeService.findBestAnswer(request.getMessage());
        return ResponseDTO.success(answer);
    }

    /**
     * 获取欢迎语
     */
    @GetMapping("/welcome")
    public ResponseDTO<String> getWelcomeMessage() {
        String welcome = "您好！欢迎来到景区导览服务！我是您的智能导览助手。\n\n" +
                        "您可以向我咨询以下信息：\n" +
                        "• 景点开放时间和门票价格\n" +
                        "• 景区地图和游览路线\n" +
                        "• 餐厅、停车场等设施位置\n" +
                        "• 紧急求助和服务投诉\n\n" +
                        "请输入您想了解的问题，我会尽力为您解答！";
        return ResponseDTO.success(welcome);
    }
}
