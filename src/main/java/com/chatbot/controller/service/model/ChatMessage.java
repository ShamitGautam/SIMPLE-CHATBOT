package com.chatbot.model;

import java.util.List;

/**
 * Represents a single message in the conversation history.
 * 
 * Gemini API expects this structure:
 * {
 *   "role": "user" | "model",
 *   "parts": [{ "text": "message content" }]
 * }
 */
public class ChatMessage {

    private String role;         // "user" or "model"
    private List<Part> parts;    // The actual message content

    // ---- Constructors ----

    public ChatMessage() {}

    // Convenience constructor - most messages have just one text part
    public ChatMessage(String role, String text) {
        this.role = role;
        this.parts = List.of(new Part(text));
    }

    // ---- Getters & Setters ----

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public List<Part> getParts() { return parts; }
    public void setParts(List<Part> parts) { this.parts = parts; }

    // ---- Inner class: Part ----
    // Each message can have multiple "parts" (text, images, etc.)
    public static class Part {
        private String text;

        public Part() {}
        public Part(String text) { this.text = text; }

        public String getText() { return text; }
        public void setText(String text) { this.text = text; }
    }
}