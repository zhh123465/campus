package com.campusforum.ai.workspace.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.campusforum.ai.workspace.domain.AiKbFavorite;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface AiKbFavoriteMapper extends BaseMapper<AiKbFavorite> {
    @Delete("DELETE FROM ai_kb_favorites WHERE tenant_id=#{tenantId} AND user_id=#{userId} AND knowledge_base_id=#{knowledgeBaseId}")
    int deleteFlag(@Param("tenantId") Long tenantId, @Param("userId") Long userId, @Param("knowledgeBaseId") String knowledgeBaseId);
}
