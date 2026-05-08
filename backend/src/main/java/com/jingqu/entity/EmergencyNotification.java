package com.jingqu.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("emergency_notifications")
public class EmergencyNotification {
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private String title;
    
    private String content;
    
    private String notificationType;
    
    private String targetScope;
    
    private String targetSpot;
    
    private LocalDateTime pushTime;
    
    private LocalDateTime expiryTime;
    
    private Integer status;
    
    private Integer readCount;
    
    private String createdBy;
    
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    
    @TableLogic
    private Integer deleted;
}
