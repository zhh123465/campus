package com.campusforum.ai.workspace.domain;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("ai_plugin_installs")
public class AiPluginInstall {
    private Long id;
    private Long tenantId;
    private Long userId;
    private String pluginId;
}
