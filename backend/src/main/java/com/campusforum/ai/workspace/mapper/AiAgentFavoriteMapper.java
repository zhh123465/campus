package com.campusforum.ai.workspace.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.campusforum.ai.workspace.domain.AiAgentFavorite;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface AiAgentFavoriteMapper extends BaseMapper<AiAgentFavorite> {
    @Delete("DELETE FROM ai_agent_favorites WHERE tenant_id=#{tenantId} AND user_id=#{userId} AND agent_id=#{agentId}")
    int deleteFlag(@Param("tenantId") Long tenantId, @Param("userId") Long userId, @Param("agentId") String agentId);
}
