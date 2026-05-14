package com.campusforum.ai.service;

import java.util.List;

public interface AiService {
    String summarize(String content);
    RiskResult moderate(String content);
    List<String> recommendTags(String title, String content);
    String chat(List<ChatMessage> messages, String context);

    /**
     * 检查打卡内容是否与挑战主题相关。
     * @param theme 挑战主题（名称 + 描述）
     * @param content 打卡内容
     * @return true 表示内容符合主题
     */
    default boolean checkRelevance(String theme, String content) {
        return true; // 默认通过
    }

    record RiskResult(int level, String reason) {}
    record ChatMessage(String role, String content) {}
}
