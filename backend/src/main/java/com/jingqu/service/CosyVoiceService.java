package com.jingqu.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;

@Slf4j
@Service
public class CosyVoiceService {

    @org.springframework.beans.factory.annotation.Value("${doubao.api-key:e4c5f1fc-e7fc-4cb3-a36f-4649dfe443bd}")
    private String apiKey;

    private static final String TTS_URL = "https://openspeech.bytedance.com/api/v3/tts/unidirectional";
    private static final String RESOURCE_ID = "seed-tts-2.0";

    private final ObjectMapper mapper = new ObjectMapper();

    /**
     * Generate TTS audio via 豆包 v3 API.
     * @return MP3 audio bytes, or null on failure
     */
    public byte[] synthesize(String text) {
        try {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("user", Map.of("uid", "jingqu_visitor"));

            Map<String, Object> reqParams = new LinkedHashMap<>();
            reqParams.put("text", text);
            reqParams.put("speaker", "zh_female_vv_uranus_bigtts");
            reqParams.put("audio_params", Map.of("format", "mp3", "sample_rate", 24000));
            body.put("req_params", reqParams);

            String json = mapper.writeValueAsString(body);
            log.info("豆包 v3 TTS request, text length={}", text.length());

            HttpURLConnection conn = (HttpURLConnection) new URL(TTS_URL).openConnection();
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(30000);
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("X-Api-Key", apiKey);
            conn.setRequestProperty("X-Api-Resource-Id", RESOURCE_ID);
            conn.setRequestProperty("X-Api-Request-Id", UUID.randomUUID().toString());
            conn.setDoOutput(true);

            try (OutputStream os = conn.getOutputStream()) {
                os.write(json.getBytes("UTF-8"));
            }

            int code = conn.getResponseCode();
            if (code != 200) {
                try (InputStream es = conn.getErrorStream()) {
                    String err = es != null ? new String(es.readAllBytes()) : "";
                    log.warn("豆包 v3 TTS HTTP {}: {}", code, err);
                }
                return null;
            }

            // unidirectional endpoint streams multiple newline-delimited JSON chunks,
            // each {"code":0,"data":"<base64 mp3 chunk>"}. Concatenate all audio chunks.
            ByteArrayOutputStream audioOut = new ByteArrayOutputStream();
            int chunkCount = 0;
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    String chunkJson = line.trim();
                    if (chunkJson.isEmpty()) continue;
                    // strip optional "data:" SSE prefix
                    if (chunkJson.startsWith("data:")) chunkJson = chunkJson.substring(5).trim();
                    if (chunkJson.isEmpty() || "[DONE]".equals(chunkJson)) continue;
                    try {
                        Map<String, Object> chunk = mapper.readValue(chunkJson, Map.class);
                        int c = ((Number) chunk.getOrDefault("code", 0)).intValue();
                        if (c != 0) {
                            log.warn("豆包 chunk code={} msg={}", c, chunk.get("message"));
                            continue;
                        }
                        Object data = chunk.get("data");
                        if (data instanceof String b64 && !b64.isEmpty()) {
                            audioOut.write(Base64.getDecoder().decode(b64));
                            chunkCount++;
                        }
                    } catch (Exception parseEx) {
                        log.debug("Skip non-JSON line: {}", parseEx.getMessage());
                    }
                }
            }
            byte[] audioBytes = audioOut.toByteArray();
            if (audioBytes.length > 0) {
                log.info("豆包 v3 TTS success: {} bytes from {} chunks", audioBytes.length, chunkCount);
                return audioBytes;
            }
            log.warn("豆包 v3 TTS: no audio data in response");
            return null;
        } catch (Exception e) {
            log.warn("豆包 v3 TTS error: {}", e.getMessage());
            return null;
        }
    }

    public boolean isAvailable() {
        return apiKey != null && !apiKey.isBlank();
    }
}
