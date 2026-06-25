package com.campusforum.ai.workspace.domain;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("ai_kb_favorites")
public class AiKbFavorite {
    private Long id;
    private Long tenantId;
    private Long userId;
    private String knowledgeBaseId;
}
