package com.jingqu.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("visitor_interactions")
public class VisitorInteraction {
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private String visitorId;
    
    private String sessionId;
    
    private String question;
    
    private String answer;
    
    private String interactionType;
    
    private String scenicSpot;

    private String routeTarget;

    private Integer retrievedDocsCount;

    private Integer fallbackUsed;

    private Long modelLatencyMs;

    private String finalAnswerSource;

    private String knowledgeSources;
    
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime interactionTime;
    
    @TableLogic
    private Integer deleted;
}
