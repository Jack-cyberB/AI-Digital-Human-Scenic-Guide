package com.jingqu.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.jingqu.dto.LoginRequest;
import com.jingqu.dto.LoginResponse;
import com.jingqu.entity.Admin;
import com.jingqu.mapper.AdminMapper;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
public class AdminService {

    @Autowired
    private AdminMapper adminMapper;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${jwt.expiration}")
    private Long jwtExpiration;

    /**
     * 管理员登录
     */
    public LoginResponse login(LoginRequest request) {
        QueryWrapper<Admin> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("username", request.getUsername());
        queryWrapper.eq("status", 1);
        
        Admin admin = adminMapper.selectOne(queryWrapper);
        
        if (admin == null) {
            throw new RuntimeException("用户名或密码错误");
        }
        
        boolean passwordMatched;
        try {
            passwordMatched = passwordEncoder.matches(request.getPassword(), admin.getPassword());
        } catch (Exception e) {
            passwordMatched = false;
        }

        if (!passwordMatched && request.getPassword() != null) {
            passwordMatched = request.getPassword().equals(admin.getPassword());
        }

        if (!passwordMatched) {
            throw new RuntimeException("用户名或密码错误");
        }
        
        // 生成JWT令牌
        String token = generateToken(admin);
        
        // 更新最后登录时间
        admin.setLastLogin(LocalDateTime.now());
        adminMapper.updateById(admin);
        
        log.info("管理员登录成功: username={}", admin.getUsername());
        
        return new LoginResponse(
            token,
            admin.getUsername(),
            admin.getRealName(),
            admin.getRole(),
            admin.getLastLogin()
        );
    }

    /**
     * 生成JWT令牌
     */
    private String generateToken(Admin admin) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("username", admin.getUsername());
        claims.put("role", admin.getRole());
        claims.put("adminId", admin.getId());

        return Jwts.builder()
                .setClaims(claims)
                .setSubject(admin.getUsername())
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + jwtExpiration))
                .signWith(getSigningKey(), SignatureAlgorithm.HS512)
                .compact();
    }

    /**
     * 验证令牌
     */
    public Admin validateToken(String token) {
        try {
            Map<String, Object> claims = Jwts.parserBuilder()
                    .setSigningKey(getSigningKey())
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
            
            String username = (String) claims.get("username");
            
            QueryWrapper<Admin> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("username", username);
            queryWrapper.eq("status", 1);
            
            return adminMapper.selectOne(queryWrapper);
            
        } catch (Exception e) {
            log.error("验证令牌失败", e);
            return null;
        }
    }

    /**
     * 获取管理员信息
     */
    public Admin getAdminById(Long id) {
        return adminMapper.selectById(id);
    }

    private Key getSigningKey() {
        byte[] keyBytes = jwtSecret.getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length < 64) {
            throw new IllegalStateException("JWT密钥长度不足，HS512至少需要64字节，请检查jwt.secret配置");
        }
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
