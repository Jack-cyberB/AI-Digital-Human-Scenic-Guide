package com.jingqu.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("daily_statistics")
public class DailyStatistics {
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private LocalDate statDate;
    
    private Integer totalInteractions;
    
    private Integer totalVisitors;
    
    private Integer peakHour;
    
    private String popularQa;
    
    private String hotspotSpots;
    
    private String hourlyData;
    
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
