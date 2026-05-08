package com.jingqu.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jingqu.dto.NotificationRequest;
import com.jingqu.dto.ResponseDTO;
import com.jingqu.entity.EmergencyNotification;
import com.jingqu.service.NotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
@CrossOrigin(origins = "*")
public class NotificationController {

    @Autowired
    private NotificationService notificationService;

    /**
     * 获取通知列表（分页）
     */
    @GetMapping
    public ResponseDTO<Map<String, Object>> getNotificationList(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) String status) {
        
        Page<EmergencyNotification> page = notificationService.getNotificationList(pageNum, pageSize, status);
        
        Map<String, Object> result = new HashMap<>();
        result.put("records", page.getRecords());
        result.put("total", page.getTotal());
        result.put("pages", page.getPages());
        result.put("current", page.getCurrent());
        result.put("size", page.getSize());
        
        return ResponseDTO.success(result);
    }

    /**
     * 获取当前有效的通知
     */
    @GetMapping("/active")
    public ResponseDTO<List<EmergencyNotification>> getActiveNotifications() {
        List<EmergencyNotification> notifications = notificationService.getActiveNotifications();
        return ResponseDTO.success(notifications);
    }

    /**
     * 根据ID获取通知
     */
    @GetMapping("/{id}")
    public ResponseDTO<EmergencyNotification> getNotificationById(@PathVariable Long id) {
        EmergencyNotification notification = notificationService.getNotificationById(id);
        if (notification == null) {
            return ResponseDTO.error("通知不存在");
        }
        return ResponseDTO.success(notification);
    }

    /**
     * 创建新通知
     */
    @PostMapping
    public ResponseDTO<EmergencyNotification> createNotification(@RequestBody NotificationRequest request) {
        EmergencyNotification notification = new EmergencyNotification();
        notification.setTitle(request.getTitle());
        notification.setContent(request.getContent());
        notification.setNotificationType(request.getNotificationType());
        notification.setTargetScope(request.getTargetScope());
        notification.setTargetSpot(request.getTargetSpot());
        notification.setExpiryTime(request.getExpiryTime());
        notification.setCreatedBy(request.getAdminId());

        EmergencyNotification result = notificationService.createNotification(notification);
        return ResponseDTO.success("通知创建成功", result);
    }

    /**
     * 兼容前端的创建请求字段
     */
    @PostMapping("/send")
    public ResponseDTO<EmergencyNotification> sendNotification(@RequestBody NotificationRequest request) {
        return createNotification(request);
    }

    /**
     * 更新通知
     */
    @PutMapping("/{id}")
    public ResponseDTO<EmergencyNotification> updateNotification(
            @PathVariable Long id,
            @RequestBody EmergencyNotification notification) {
        notification.setId(id);
        EmergencyNotification result = notificationService.updateNotification(notification);
        return ResponseDTO.success("更新成功", result);
    }

    /**
     * 删除通知
     */
    @DeleteMapping("/{id}")
    public ResponseDTO<String> deleteNotification(@PathVariable Long id) {
        notificationService.deleteNotification(id);
        return ResponseDTO.success("删除成功", null);
    }

    /**
     * 标记通知已读
     */
    @PostMapping("/{id}/read")
    public ResponseDTO<String> markAsRead(@PathVariable Long id) {
        notificationService.incrementReadCount(id);
        return ResponseDTO.success("已标记为已读", null);
    }
}
