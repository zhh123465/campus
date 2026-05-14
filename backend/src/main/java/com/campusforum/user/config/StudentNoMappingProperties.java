package com.campusforum.user.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Data
@Component
@ConfigurationProperties(prefix = "student-no")
public class StudentNoMappingProperties {

    private List<MappingEntry> mapping = new ArrayList<>();

    /**
     * 按前缀长度降序排列，实现最长前缀优先匹配
     */
    public List<MappingEntry> getMapping() {
        mapping.sort(Comparator.comparingInt((MappingEntry e) -> e.getPrefix().length()).reversed());
        return mapping;
    }

    @Data
    public static class MappingEntry {
        private String prefix;
        private String college;
        private String major;
        private String grade;
    }
}
