package com.chatbot.service;

import com.chatbot.model.ChatMessage;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * GroqService (file kept as GeminiService for project compatibility)
 *
 * Using: Groq Cloud API — free tier, OpenAI-compatible
 *
 * Groq is 100% OpenAI API format, so only the base URL changes.
 * Extra: rate-limit handling (429) with a friendly message since
 * Groq free tier has tokens-per-minute limits.
 *
 * Groq free tier limits (as of 2026):
 *  - 30 requests/minute
 *  - 6000 tokens/minute for most models
 *  - 14,400 requests/day
 */
@Service
public class GeminiService {

    @Value("${openai.api.key}")
    private String apiKey;

    @Value("${openai.api.url}")
    private String apiUrl;

    @Value("${openai.model}")
    private String model;

    @Value("${chatbot.max.history.turns}")
    private int maxHistoryTurns;

    private final WebClient webClient;
    private final ObjectMapper objectMapper;

    // =============================================
    // SESSION MEMORY STORE
    // Key   = sessionId (unique per browser tab/user)
    // Value = List of ChatMessages (the conversation history)
    // =============================================
    private final Map<String, List<ChatMessage>> sessionHistories = new ConcurrentHashMap<>();

    public GeminiService(WebClient.Builder webClientBuilder, ObjectMapper objectMapper) {
        this.webClient = webClientBuilder.build();
        this.objectMapper = objectMapper;
    }

    /**
     * MAIN METHOD: Send a message and get a response
     */
    public String chat(String sessionId, String userInput) {

        // ── STEP 1: STRUCTURAL VALIDATION GATE ──────────────────
        if (userInput == null || userInput.trim().isEmpty()) {
            return "⚠️ Please enter a message before sending.";
        }
        userInput = userInput.trim();

        // ── STEP 2: GET OR CREATE SESSION HISTORY ───────────────
        List<ChatMessage> history = sessionHistories
                .computeIfAbsent(sessionId, id -> new ArrayList<>());

        // ── STEP 3: APPEND USER MESSAGE TO HISTORY ──────────────
        history.add(new ChatMessage("user", userInput));

        // ── STEP 4: SLIDING WINDOW (FIFO Pruning) ───────────────
        int maxMessages = maxHistoryTurns * 2;
        while (history.size() > maxMessages) {
            history.remove(0);
            if (!history.isEmpty()) history.remove(0);
        }

        // ── STEP 5: CALL GROQ API ────────────────────────────────
        try {
            String responseText = callGroqAPI(history);

            // ── STEP 6: APPEND AI RESPONSE TO HISTORY ───────────
            history.add(new ChatMessage("assistant", responseText));

            return responseText;

        } catch (WebClientResponseException e) {
            // Remove the user message we just added (keep history clean)
            history.remove(history.size() - 1);

            // ── GROQ SPECIFIC: Handle rate limit gracefully ──────
            if (e.getStatusCode().value() == 429) {
                return "⏳ Groq rate limit reached. Please wait a few seconds and try again. " +
                       "(Free tier: 30 requests/min, 6000 tokens/min)";
            }
            if (e.getStatusCode().value() == 401) {
                return "🔑 Invalid API key. Please check your Groq API key in application.properties.";
            }
            return "❌ API Error " + e.getStatusCode().value() + ": " + e.getResponseBodyAsString();

        } catch (Exception e) {
            history.remove(history.size() - 1);
            return "❌ Error: " + e.getMessage();
        }
    }

    /**
     * Calls the Groq API (OpenAI-compatible format)
     *
     * Request format — identical to OpenAI:
     * {
     *   "model": "llama-3.3-70b-versatile",
     *   "messages": [
     *     { "role": "system",    "content": "You are a helpful assistant." },
     *     { "role": "user",      "content": "My name is Vipin" },
     *     { "role": "assistant", "content": "Nice to meet you, Vipin!" },
     *     { "role": "user",      "content": "What is my name?" }
     *   ],
     *   "max_tokens": 1024,
     *   "temperature": 0.8
     * }
     *
     * Auth: Same as OpenAI — "Authorization: Bearer gsk_..."
     */
    private String callGroqAPI(List<ChatMessage> history) throws Exception {

        // Build request body
        ObjectNode requestBody = objectMapper.createObjectNode();
        requestBody.put("model", model);
        requestBody.put("max_tokens", 1024);
        requestBody.put("temperature", 0.8);

        // Build messages array
        ArrayNode messagesArray = requestBody.putArray("messages");

        // ── SYSTEM MESSAGE ──────────────────────────────────────
        ObjectNode systemMsg = messagesArray.addObject();
        systemMsg.put("role", "system");
        systemMsg.put("content",
            "You are Nova, a helpful and friendly AI assistant. " +
            "You remember everything the user tells you during this conversation " +
            "and refer back to it naturally. Be concise but thorough."
        );

        // ── CONVERSATION HISTORY ────────────────────────────────
        for (ChatMessage message : history) {
            ObjectNode msgNode = messagesArray.addObject();
            String role = message.getRole().equals("model") ? "assistant" : message.getRole();
            msgNode.put("role", role);
            msgNode.put("content", message.getParts().get(0).getText());
        }

        // ── CALL GROQ API ───────────────────────────────────────
        // Auth header is identical to OpenAI: "Authorization: Bearer <key>"
        String responseJson = webClient.post()
                .uri(apiUrl)
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .bodyValue(requestBody.toString())
                .retrieve()
                .bodyToMono(String.class)
                .block();

        // ── EXTRACT RESPONSE ────────────────────────────────────
        // Groq response format is identical to OpenAI:
        // choices[0].message.content
        JsonNode responseNode = objectMapper.readTree(responseJson);
        return responseNode
                .path("choices")
                .get(0)
                .path("message")
                .path("content")
                .asText("No response received.");
    }

    /**
     * Clear the history for a session
     */
    public void clearHistory(String sessionId) {
        sessionHistories.remove(sessionId);
    }

    /**
     * Get current history size for a session
     */
    public int getHistorySize(String sessionId) {
        List<ChatMessage> history = sessionHistories.get(sessionId);
        return history != null ? history.size() : 0;
    }
}