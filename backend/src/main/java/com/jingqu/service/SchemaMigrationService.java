package com.jingqu.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class SchemaMigrationService implements ApplicationRunner {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(ApplicationArguments args) {
        List<Map.Entry<String, String>> statements = Arrays.asList(
            Map.entry("route_target", "ALTER TABLE visitor_interactions ADD COLUMN route_target VARCHAR(64) NULL COMMENT 'AI route target'"),
            Map.entry("retrieved_docs_count", "ALTER TABLE visitor_interactions ADD COLUMN retrieved_docs_count INT DEFAULT 0 COMMENT 'Retrieved docs count'"),
            Map.entry("fallback_used", "ALTER TABLE visitor_interactions ADD COLUMN fallback_used TINYINT DEFAULT 0 COMMENT 'Whether fallback was used'"),
            Map.entry("model_latency_ms", "ALTER TABLE visitor_interactions ADD COLUMN model_latency_ms BIGINT DEFAULT 0 COMMENT 'Model latency ms'"),
            Map.entry("final_answer_source", "ALTER TABLE visitor_interactions ADD COLUMN final_answer_source VARCHAR(32) DEFAULT 'fallback' COMMENT 'Answer source'"),
            Map.entry("knowledge_sources", "ALTER TABLE visitor_interactions ADD COLUMN knowledge_sources TEXT NULL COMMENT 'Knowledge sources'")
        );

        for (Map.Entry<String, String> entry : statements) {
            try {
                if (!columnExists("visitor_interactions", entry.getKey())) {
                    jdbcTemplate.execute(entry.getValue());
                }
            } catch (Exception e) {
                log.warn("Schema migration skipped or failed: {}", entry.getKey(), e);
            }
        }
    }

    private boolean columnExists(String tableName, String columnName) {
        Integer count = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = ? AND column_name = ?",
            Integer.class,
            tableName,
            columnName
        );
        return count != null && count > 0;
    }
}
