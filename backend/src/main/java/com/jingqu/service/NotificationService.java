package com.jingqu.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jingqu.entity.EmergencyNotification;
import com.jingqu.mapper.EmergencyNotificationMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
public class NotificationService {

    @Autowired
    private EmergencyNotificationMapper notificationMapper;

    /**
     * 获取通知列表（分页）
     */
    public Page<EmergencyNotification> getNotificationList(int pageNum, int pageSize, String status) {
        Page<EmergencyNotification> page = new Page<>(pageNum, pageSize);
        
        QueryWrapper<EmergencyNotification> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("deleted", 0);
        
        if (status != null && !status.isEmpty()) {
            queryWrapper.eq("status", status);
        }
        
        queryWrapper.orderByDesc("created_at");
        
        return notificationMapper.selectPage(page, queryWrapper);
    }

    /**
     * 获取所有已推送的通知
     */
    public List<EmergencyNotification> getActiveNotifications() {
        QueryWrapper<EmergencyNotification> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("deleted", 0);
        queryWrapper.eq("status", 1);
        queryWrapper.ge("expiry_time", LocalDateTime.now());
        queryWrapper.orderByDesc("push_time");
        
        return notificationMapper.selectList(queryWrapper);
    }

    /**
     * 根据ID获取通知
     */
    public EmergencyNotification getNotificationById(Long id) {
        return notificationMapper.selectById(id);
    }

    /**
     * 创建新通知
     */
    @Transactional
    public EmergencyNotification createNotification(EmergencyNotification notification) {
        notification.setStatus(1);
        notification.setPushTime(LocalDateTime.now());
        notification.setReadCount(0);
        notificationMapper.insert(notification);
        
        log.info("创建通知: id={}, title={}", notification.getId(), notification.getTitle());
        return notification;
    }

    /**
     * 更新通知
     */
    @Transactional
    public EmergencyNotification updateNotification(EmergencyNotification notification) {
        notificationMapper.updateById(notification);
        
        log.info("更新通知: id={}", notification.getId());
        return notification;
    }

    /**
     * 删除通知
     */
    @Transactional
    public void deleteNotification(Long id) {
        EmergencyNotification notification = new EmergencyNotification();
        notification.setId(id);
        notification.setDeleted(1);
        notificationMapper.updateById(notification);
        
        log.info("删除通知: id={}", id);
    }

    /**
     * 更新通知已读数
     */
    @Transactional
    public void incrementReadCount(Long id) {
        EmergencyNotification notification = notificationMapper.selectById(id);
        if (notification != null) {
            notification.setReadCount(notification.getReadCount() + 1);
            notificationMapper.updateById(notification);
        }
    }

    /**
     * 标记通知为已过期
     */
    @Transactional
    public void expireNotifications() {
        QueryWrapper<EmergencyNotification> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("deleted", 0);
        queryWrapper.eq("status", 1);
        queryWrapper.lt("expiry_time", LocalDateTime.now());
        
        List<EmergencyNotification> notifications = notificationMapper.selectList(queryWrapper);
        
        for (EmergencyNotification notification : notifications) {
            notification.setStatus(2);
            notificationMapper.updateById(notification);
        }
        
        if (!notifications.isEmpty()) {
            log.info("标记 {} 条通知为已过期", notifications.size());
        }
    }
}
