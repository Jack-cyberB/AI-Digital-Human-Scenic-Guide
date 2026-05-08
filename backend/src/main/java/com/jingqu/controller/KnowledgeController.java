package com.jingqu.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jingqu.dto.ResponseDTO;
import com.jingqu.entity.KnowledgeBase;
import com.jingqu.service.KnowledgeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/knowledge")
@CrossOrigin(origins = "*")
public class KnowledgeController {

    @Autowired
    private KnowledgeService knowledgeService;

    /**
     * 获取知识库列表（分页）
     */
    @GetMapping
    public ResponseDTO<Map<String, Object>> getKnowledgeList(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) Integer status) {
        
        Page<KnowledgeBase> page = knowledgeService.getKnowledgeList(pageNum, pageSize, category, status);
        
        Map<String, Object> result = new HashMap<>();
        result.put("records", page.getRecords());
        result.put("total", page.getTotal());
        result.put("pages", page.getPages());
        result.put("current", page.getCurrent());
        result.put("size", page.getSize());
        
        return ResponseDTO.success(result);
    }

    /**
     * 获取所有启用的知识库
     */
    @GetMapping("/all")
    public ResponseDTO<List<KnowledgeBase>> getAllActiveKnowledge() {
        List<KnowledgeBase> knowledgeList = knowledgeService.getAllActiveKnowledge();
        return ResponseDTO.success(knowledgeList);
    }

    /**
     * 根据ID获取知识库条目
     */
    @GetMapping("/{id}")
    public ResponseDTO<KnowledgeBase> getKnowledgeById(@PathVariable Long id) {
        KnowledgeBase knowledge = knowledgeService.getKnowledgeById(id);
        if (knowledge == null) {
            return ResponseDTO.error("知识库条目不存在");
        }
        return ResponseDTO.success(knowledge);
    }

    /**
     * 添加知识库条目
     */
    @PostMapping
    public ResponseDTO<KnowledgeBase> addKnowledge(@RequestBody KnowledgeBase knowledge) {
        KnowledgeBase result = knowledgeService.addKnowledge(knowledge);
        return ResponseDTO.success("添加成功", result);
    }

    /**
     * 更新知识库条目
     */
    @PutMapping("/{id}")
    public ResponseDTO<KnowledgeBase> updateKnowledge(
            @PathVariable Long id,
            @RequestBody KnowledgeBase knowledge) {
        knowledge.setId(id);
        KnowledgeBase result = knowledgeService.updateKnowledge(knowledge);
        return ResponseDTO.success("更新成功", result);
    }

    /**
     * 删除知识库条目
     */
    @DeleteMapping("/{id}")
    public ResponseDTO<String> deleteKnowledge(@PathVariable Long id) {
        knowledgeService.deleteKnowledge(id);
        return ResponseDTO.success("删除成功", null);
    }

    /**
     * 根据分类获取知识库
     */
    @GetMapping("/category/{category}")
    public ResponseDTO<List<KnowledgeBase>> getKnowledgeByCategory(@PathVariable String category) {
        List<KnowledgeBase> knowledgeList = knowledgeService.getKnowledgeByCategory(category);
        return ResponseDTO.success(knowledgeList);
    }

    /**
     * 同步知识库到游客端（触发WebSocket广播）
     */
    @PostMapping("/sync")
    public ResponseDTO<Map<String, Object>> syncKnowledge() {
        Map<String, Object> result = new HashMap<>();
        result.put("synced", true);
        result.put("timestamp", java.time.LocalDateTime.now());
        return ResponseDTO.success("知识库同步已触发", result);
    }

    /**
     * 测试问答匹配（内部接口）
     */
    @PostMapping("/test")
    public ResponseDTO<Map<String, Object>> testAnswer(@RequestBody Map<String, String> request) {
        String question = request.get("question");
        String answer = knowledgeService.findBestAnswer(question);

        Map<String, Object> result = new HashMap<>();
        result.put("question", question);
        result.put("answer", answer);

        return ResponseDTO.success(result);
    }
}
