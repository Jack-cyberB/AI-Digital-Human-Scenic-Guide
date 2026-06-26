package com.jingqu.controller;

import com.jingqu.service.CosyVoiceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/tts")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class TtsController {

    private final CosyVoiceService cosyVoiceService;

    @PostMapping("/speak")
    public ResponseEntity<byte[]> speak(@RequestBody Map<String, String> request) {
        String text = request.getOrDefault("text", "");
        if (text.isBlank()) return ResponseEntity.badRequest().build();

        byte[] audio = cosyVoiceService.synthesize(text);
        if (audio == null || audio.length == 0) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build();
        }

        return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType("audio/mpeg"))
            .header(HttpHeaders.CONTENT_DISPOSITION, "inline")
            .body(audio);
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        boolean available = cosyVoiceService.isAvailable();
        return ResponseEntity.ok(Map.of("cosyvoice", available));
    }
}
