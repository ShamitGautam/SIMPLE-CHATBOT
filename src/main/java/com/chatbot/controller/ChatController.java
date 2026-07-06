package com.chatbot.controller;

import com.chatbot.service.GeminiService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

/**
 * ChatController — REST API Endpoints
 *
 * POST /api/chat        → Send a message, get AI response
 * POST /api/clear       → Clear conversation history
 * GET  /api/session/new → Generate a new session ID
 */
@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*") // Allow frontend to call this API
public class ChatController {

    private final GeminiService geminiService;

    public ChatController(GeminiService geminiService) {
        this.geminiService = geminiService;
    }

    /**
     * POST /api/chat
     * Body: { "sessionId": "abc123", "message": "Hello!" }
     * Returns: { "response": "Hi there!", "historySize": 2 }
     */
    @PostMapping("/chat")
    public ResponseEntity<Map<String, Object>> chat(@RequestBody Map<String, String> request) {

        String sessionId = request.get("sessionId");
        String message = request.get("message");

        // Validate session ID
        if (sessionId == null || sessionId.trim().isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Session ID is required"));
        }

        // Get AI response (memory logic is all inside GeminiService)
        String response = geminiService.chat(sessionId, message);
        int historySize = geminiService.getHistorySize(sessionId);

        return ResponseEntity.ok(Map.of(
                "response", response,
                "historySize", historySize
        ));
    }

    /**
     * POST /api/clear
     * Body: { "sessionId": "abc123" }
     * Clears the conversation history for this session
     */
    @PostMapping("/clear")
    public ResponseEntity<Map<String, String>> clearHistory(@RequestBody Map<String, String> request) {
        String sessionId = request.get("sessionId");
        geminiService.clearHistory(sessionId);
        return ResponseEntity.ok(Map.of("status", "History cleared successfully"));
    }

    /**
     * GET /api/session/new
     * Generates a unique session ID for a new user/tab
     */
    @GetMapping("/session/new")
    public ResponseEntity<Map<String, String>> newSession() {
        String sessionId = UUID.randomUUID().toString();
        return ResponseEntity.ok(Map.of("sessionId", sessionId));
    }
}