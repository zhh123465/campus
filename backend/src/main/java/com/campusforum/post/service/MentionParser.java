package com.campusforum.post.service;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 从内容中解析 @username 提及，去重后返回昵称集合。
 */
public final class MentionParser {

    /** 匹配 @昵称：中文/英文/数字/下划线/连字符，1-30 字符 */
    private static final Pattern MENTION_PATTERN = Pattern.compile("@([\\w\\u4e00-\\u9fa5-]{1,30})");

    private MentionParser() {}

    /**
     * 提取内容中所有被 @ 的用户昵称（去重）。
     */
    public static Set<String> extract(String content) {
        if (content == null || content.isBlank()) return Set.of();
        Set<String> names = new LinkedHashSet<>();
        Matcher m = MENTION_PATTERN.matcher(content);
        while (m.find()) {
            names.add(m.group(1));
        }
        return names;
    }
}
