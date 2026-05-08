package com.jingqu.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jingqu.entity.KnowledgeBase;
import com.jingqu.mapper.KnowledgeBaseMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

@Slf4j
@Service
public class KnowledgeService {

    @Autowired
    private KnowledgeBaseMapper knowledgeMapper;

    /**
     * 获取知识库列表（分页）
     */
    public Page<KnowledgeBase> getKnowledgeList(int pageNum, int pageSize, String category, Integer status) {
        Page<KnowledgeBase> page = new Page<>(pageNum, pageSize);
        
        QueryWrapper<KnowledgeBase> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("deleted", 0);
        
        if (category != null && !category.isEmpty()) {
            queryWrapper.eq("category", category);
        }
        if (status != null) {
            queryWrapper.eq("status", status);
        }
        
        queryWrapper.orderByDesc("priority").orderByDesc("updated_at");
        
        return knowledgeMapper.selectPage(page, queryWrapper);
    }

    /**
     * 获取所有启用的知识库条目
     */
    public List<KnowledgeBase> getAllActiveKnowledge() {
        QueryWrapper<KnowledgeBase> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("deleted", 0);
        queryWrapper.eq("status", 1);
        queryWrapper.orderByDesc("priority");
        
        return knowledgeMapper.selectList(queryWrapper);
    }

    /**
     * 根据ID获取知识库条目
     */
    public KnowledgeBase getKnowledgeById(Long id) {
        return knowledgeMapper.selectById(id);
    }

    /**
     * 添加知识库条目
     */
    @Transactional
    public KnowledgeBase addKnowledge(KnowledgeBase knowledge) {
        knowledge.setViewCount(0);
        knowledge.setSuccessCount(0);
        knowledge.setStatus(1);
        knowledgeMapper.insert(knowledge);
        
        log.info("添加知识库条目: id={}", knowledge.getId());
        return knowledge;
    }

    /**
     * 更新知识库条目
     */
    @Transactional
    public KnowledgeBase updateKnowledge(KnowledgeBase knowledge) {
        knowledgeMapper.updateById(knowledge);
        
        log.info("更新知识库条目: id={}", knowledge.getId());
        return knowledge;
    }

    /**
     * 删除知识库条目
     */
    @Transactional
    public void deleteKnowledge(Long id) {
        KnowledgeBase knowledge = new KnowledgeBase();
        knowledge.setId(id);
        knowledge.setDeleted(1);
        knowledgeMapper.updateById(knowledge);
        
        log.info("删除知识库条目: id={}", id);
    }

    /**
     * 根据用户消息查找最佳匹配答案
     */
    public String findBestAnswer(String userMessage) {
        if (userMessage == null || userMessage.trim().isEmpty()) {
            return "您好，有什么可以帮助您的吗？请告诉我您想了解的景区信息。";
        }
        
        String normalizedMessage = userMessage.toLowerCase().trim();
        
        QueryWrapper<KnowledgeBase> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("deleted", 0);
        queryWrapper.eq("status", 1);
        queryWrapper.orderByDesc("priority");
        
        List<KnowledgeBase> knowledgeList = knowledgeMapper.selectList(queryWrapper);
        
        // 增加查询次数
        if (!knowledgeList.isEmpty()) {
            for (KnowledgeBase kb : knowledgeList) {
                if (matchesPattern(kb, normalizedMessage)) {
                    kb.setViewCount(kb.getViewCount() + 1);
                    knowledgeMapper.updateById(kb);
                    break;
                }
            }
        }
        
        // 查找最佳匹配
        KnowledgeBase bestMatch = null;
        int bestScore = 0;
        
        for (KnowledgeBase kb : knowledgeList) {
            int score = calculateMatchScore(kb, normalizedMessage);
            if (score > bestScore) {
                bestScore = score;
                bestMatch = kb;
            }
        }
        
        if (bestMatch != null && bestScore >= 30) {
            // 更新成功匹配次数
            bestMatch.setSuccessCount(bestMatch.getSuccessCount() + 1);
            knowledgeMapper.updateById(bestMatch);
            
            return bestMatch.getAnswer();
        }
        
        // 默认回复
        return generateDefaultResponse(normalizedMessage);
    }

    /**
     * 计算匹配分数
     */
    private int calculateMatchScore(KnowledgeBase kb, String message) {
        int score = 0;
        
        // 检查问题模式匹配
        if (kb.getQuestionPattern() != null) {
            String[] patterns = kb.getQuestionPattern().split("[|,，]");
            for (String pattern : patterns) {
                pattern = pattern.trim().toLowerCase();
                if (!pattern.isEmpty()) {
                    if (message.contains(pattern)) {
                        score += 50;
                    }
                    if (message.contains("的") && pattern.contains(message.substring(message.indexOf("的") + 1))) {
                        score += 30;
                    }
                }
            }
        }
        
        // 检查关键词匹配
        if (kb.getKeywords() != null) {
            String[] keywords = kb.getKeywords().split("[,，]");
            for (String keyword : keywords) {
                keyword = keyword.trim().toLowerCase();
                if (!keyword.isEmpty() && message.contains(keyword)) {
                    score += 20;
                }
            }
        }
        
        // 加上优先级权重
        score += Math.min(kb.getPriority() * 2, 20);
        
        return score;
    }

    /**
     * 检查是否匹配模式
     */
    private boolean matchesPattern(KnowledgeBase kb, String message) {
        if (kb.getQuestionPattern() == null) {
            return false;
        }
        
        String[] patterns = kb.getQuestionPattern().split("[|,，]");
        for (String pattern : patterns) {
            pattern = pattern.trim().toLowerCase();
            if (!pattern.isEmpty() && message.contains(pattern)) {
                return true;
            }
        }
        
        return false;
    }

    /**
     * 生成默认回复
     */
    private String generateDefaultResponse(String message) {
        // 根据消息关键词生成智能回复
        if (message.contains("门票") || message.contains("票") || message.contains("价格")) {
            return "关于门票价格，建议您访问景区官网或咨询售票窗口。成人票150元/人，学生票75元/人，老人票（65岁以上）免费。网上购票可享受9折优惠。";
        }
        
        if (message.contains("开放") || message.contains("时间") || message.contains("几点")) {
            return "景区开放时间为每天 08:00-18:00（旺季延长至18:30），建议您提前做好行程规划，合理安排游玩时间。";
        }
        
        if (message.contains("停车") || message.contains("车") || message.contains("开车")) {
            return "景区内设有3个停车场：1号门（地下300车位）、2号门（地上200车位）、3号门（地上150车位）。停车费首小时5元，后续2元/小时。";
        }
        
        if (message.contains("吃") || message.contains("餐厅") || message.contains("餐饮") || message.contains("饭")) {
            return "景区内有多家餐厅：山顶餐厅（川菜、湘菜）、湖边咖啡厅（西餐、下午茶）、游客中心餐厅（快餐、小吃）。营业时间 10:00-20:00。";
        }
        
        if (message.contains("洗手间") || message.contains("厕所") || message.contains("卫生间") || message.contains("wc")) {
            return "景区内设有多个卫生间，分别位于入口大厅、各景点附近及餐饮区，请留意指示牌。";
        }
        
        if (message.contains("紧急") || message.contains("急救") || message.contains("报警")) {
            return "紧急求助请拨打：景区热线 400-XXX-XXXX；报警 110；急救 120。景区内设有多个紧急求助点，红色按钮一键求助。";
        }
        
        // 默认回复
        return "您好！我已经收到您的问题。关于这个问题，建议您：\n1. 查看景区导览图和指示牌\n2. 拨打客服热线 400-XXX-XXXX 咨询\n3. 前往最近的游客服务中心寻求帮助\n\n请问还有什么可以帮您的吗？";
    }

    /**
     * 获取知识库分类统计
     */
    public List<KnowledgeBase> getKnowledgeByCategory(String category) {
        QueryWrapper<KnowledgeBase> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("deleted", 0);
        queryWrapper.eq("status", 1);
        queryWrapper.eq("category", category);
        queryWrapper.orderByDesc("priority");
        
        return knowledgeMapper.selectList(queryWrapper);
    }
}
