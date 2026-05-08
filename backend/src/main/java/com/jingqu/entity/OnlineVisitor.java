package com.jingqu.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("online_visitors")
public class OnlineVisitor {
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private String visitorId;
    
    private String sessionId;
    
    private String deviceInfo;
    
    private String currentSpot;
    
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime connectedTime;
    
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime lastActiveTime;
    
    private Integer isOnline;
}
