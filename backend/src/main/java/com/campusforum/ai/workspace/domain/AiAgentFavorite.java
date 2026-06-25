package com.campusforum.ai.workspace.domain;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("ai_agent_favorites")
public class AiAgentFavorite {
    private Long id;
    private Long tenantId;
    private Long userId;
    private String agentId;
}
