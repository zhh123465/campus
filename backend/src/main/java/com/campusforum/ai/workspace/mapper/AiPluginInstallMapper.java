package com.campusforum.ai.workspace.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.campusforum.ai.workspace.domain.AiPluginInstall;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface AiPluginInstallMapper extends BaseMapper<AiPluginInstall> {
    @Delete("DELETE FROM ai_plugin_installs WHERE tenant_id=#{tenantId} AND user_id=#{userId} AND plugin_id=#{pluginId}")
    int deleteFlag(@Param("tenantId") Long tenantId, @Param("userId") Long userId, @Param("pluginId") String pluginId);
}
