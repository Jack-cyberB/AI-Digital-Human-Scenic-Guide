package com.jingqu.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("knowledge_base")
public class KnowledgeBase {
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private String questionPattern;
    
    private String answer;
    
    private String keywords;
    
    private String category;
    
    private Integer priority;
    
    private Integer status;
    
    private Integer viewCount;
    
    private Integer successCount;
    
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
    
    @TableLogic
    private Integer deleted;
}
