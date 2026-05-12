package com.campusforum.ai.service;

import java.util.List;

public interface AiService {
    String summarize(String content);
    RiskResult moderate(String content);
    List<String> recommendTags(String title, String content);
    String chat(List<ChatMessage> messages, String context);

    record RiskResult(int level, String reason) {}
    record ChatMessage(String role, String content) {}
}
