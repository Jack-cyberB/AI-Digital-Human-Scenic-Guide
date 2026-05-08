package com.jingqu.controller;

import com.jingqu.dto.LoginRequest;
import com.jingqu.dto.LoginResponse;
import com.jingqu.dto.ResponseDTO;
import com.jingqu.entity.Admin;
import com.jingqu.service.AdminService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@CrossOrigin(origins = "*")
public class AdminController {

    @Autowired
    private AdminService adminService;

    /**
     * 管理员登录
     */
    @PostMapping("/login")
    public ResponseDTO<LoginResponse> login(@RequestBody LoginRequest request) {
        try {
            LoginResponse response = adminService.login(request);
            return ResponseDTO.success("登录成功", response);
        } catch (Exception e) {
            return ResponseDTO.error(401, e.getMessage());
        }
    }

    /**
     * 管理员登出
     */
    @PostMapping("/logout")
    public ResponseDTO<String> logout(@RequestHeader(value = "Authorization", required = false) String token) {
        // JWT是无状态的，这里只需要返回成功即可
        // 如果使用Redis等存储，可以在这里清除token
        return ResponseDTO.success("登出成功", null);
    }

    /**
     * 获取管理员信息
     */
    @GetMapping("/profile")
    public ResponseDTO<Map<String, Object>> getProfile(@RequestHeader(value = "Authorization", required = false) String token) {
        try {
            if (token != null && token.startsWith("Bearer ")) {
                token = token.substring(7);
            }
            
            Admin admin = adminService.validateToken(token);
            if (admin == null) {
                return ResponseDTO.error(401, "无效的令牌");
            }
            
            Map<String, Object> profile = new HashMap<>();
            profile.put("id", admin.getId());
            profile.put("username", admin.getUsername());
            profile.put("realName", admin.getRealName());
            profile.put("role", admin.getRole());
            
            return ResponseDTO.success(profile);
        } catch (Exception e) {
            return ResponseDTO.error(500, "获取管理员信息失败");
        }
    }

    /**
     * 验证令牌
     */
    @GetMapping("/validate")
    public ResponseDTO<Boolean> validateToken(@RequestHeader(value = "Authorization", required = false) String token) {
        try {
            if (token != null && token.startsWith("Bearer ")) {
                token = token.substring(7);
            }
            
            Admin admin = adminService.validateToken(token);
            return ResponseDTO.success(admin != null);
        } catch (Exception e) {
            return ResponseDTO.success(false);
        }
    }
}
