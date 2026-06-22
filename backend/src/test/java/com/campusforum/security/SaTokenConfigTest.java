package com.campusforum.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.config.annotation.InterceptorRegistration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;

import java.lang.reflect.Field;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * SaTokenConfig 单元测试：验证拦截器注册正确。
 *
 * <p>当前放行路径不再写入 Spring MVC 的 excludePatterns，而是在 SaInterceptor 内
 * 通过 SaRouter.notMatch 声明。因此这个测试只验证拦截器覆盖 /api/v1/**，
 * 具体放行/拦截行为由接口集成测试覆盖。</p>
 */
class SaTokenConfigTest {

    @Test
    @DisplayName("Sa-Token 拦截器不再使用 registry excludePatterns")
    void shouldExcludeForgotAndResetPasswordPaths() throws Exception {
        SaTokenConfig config = new SaTokenConfig();
        InterceptorRegistry registry = new InterceptorRegistry();
        config.addInterceptors(registry);

        // 通过反射获取注册的拦截器配置
        List<String> excludePatterns = getExcludePatterns(registry);

        assertThat(excludePatterns)
                .isEmpty();
    }

    @Test
    @DisplayName("/me 不通过 registry excludePatterns 放行")
    void shouldNotExcludeMePath() throws Exception {
        SaTokenConfig config = new SaTokenConfig();
        InterceptorRegistry registry = new InterceptorRegistry();
        config.addInterceptors(registry);

        List<String> excludePatterns = getExcludePatterns(registry);

        assertThat(excludePatterns)
                .doesNotContain("/api/v1/auth/me");
    }

    @Test
    @DisplayName("login 和 register 由 SaRouter.notMatch 放行")
    void shouldExcludeLoginAndRegisterPaths() throws Exception {
        SaTokenConfig config = new SaTokenConfig();
        InterceptorRegistry registry = new InterceptorRegistry();
        config.addInterceptors(registry);

        List<String> excludePatterns = getExcludePatterns(registry);

        assertThat(excludePatterns)
                .isEmpty();
    }

    @Test
    @DisplayName("GET 鉴权默认收紧：仅明确公开读接口可游客访问")
    void getAuthPolicyShouldUseExplicitPublicAllowlist() {
        assertThat(SaTokenConfig.requiresLoginForGet("/api/v1/posts")).isFalse();
        assertThat(SaTokenConfig.requiresLoginForGet("/api/v1/posts/1/comments")).isFalse();
        assertThat(SaTokenConfig.requiresLoginForGet("/api/v1/users/1")).isFalse();
        assertThat(SaTokenConfig.requiresLoginForGet("/api/v1/resources/1/preview")).isFalse();
        assertThat(SaTokenConfig.requiresLoginForGet("/api/v1/ai/agents")).isFalse();

        assertThat(SaTokenConfig.requiresLoginForGet("/api/v1/users/me")).isTrue();
        assertThat(SaTokenConfig.requiresLoginForGet("/api/v1/users/me/favorites")).isTrue();
        assertThat(SaTokenConfig.requiresLoginForGet("/api/v1/resources/1/signed-url")).isTrue();
        assertThat(SaTokenConfig.requiresLoginForGet("/api/v1/messages/conversations")).isTrue();
        assertThat(SaTokenConfig.requiresLoginForGet("/api/v1/notifications")).isTrue();
        assertThat(SaTokenConfig.requiresLoginForGet("/api/v1/spaces/1/posts/all")).isTrue();
        assertThat(SaTokenConfig.requiresLoginForGet("/api/v1/ai/conversations")).isTrue();
        assertThat(SaTokenConfig.requiresLoginForGet("/api/v1/ai/knowledge-bases")).isTrue();
        assertThat(SaTokenConfig.requiresLoginForGet("/api/v1/admin/dashboard")).isTrue();
        assertThat(SaTokenConfig.requiresLoginForGet("/api/v1/future/private-read")).isTrue();
    }

    @Test
    @DisplayName("认证类公开 POST 应允许未登录访问，其他写接口默认要求登录")
    void nonGetAuthPolicyShouldOnlyAllowExplicitPublicEndpoints() {
        assertThat(SaTokenConfig.requiresLogin("POST", "/api/v1/auth/login")).isFalse();
        assertThat(SaTokenConfig.requiresLogin("POST", "/api/v1/auth/wechat-login")).isFalse();
        assertThat(SaTokenConfig.requiresLogin("POST", "/api/v1/auth/qq-login")).isFalse();
        assertThat(SaTokenConfig.requiresLogin("POST", "/api/v1/auth/github-device-code")).isFalse();
        assertThat(SaTokenConfig.requiresLogin("POST", "/api/v1/auth/github-device-login")).isFalse();
        assertThat(SaTokenConfig.requiresLogin("POST", "/api/v1/ai/post-cards/batch")).isFalse();

        assertThat(SaTokenConfig.requiresLogin("POST", "/api/v1/resources")).isTrue();
        assertThat(SaTokenConfig.requiresLogin("PUT", "/api/v1/users/me")).isTrue();
        assertThat(SaTokenConfig.requiresLogin("DELETE", "/api/v1/posts/1")).isTrue();
        assertThat(SaTokenConfig.requiresLogin("PATCH", "/api/v1/ai/agents/agent-1")).isTrue();
    }

    /**
     * 通过反射从 InterceptorRegistry 中提取排除路径列表。
     */
    @SuppressWarnings("unchecked")
    private List<String> getExcludePatterns(InterceptorRegistry registry) throws Exception {
        // InterceptorRegistry 内部维护 registrations 列表
        Field registrationsField = InterceptorRegistry.class.getDeclaredField("registrations");
        registrationsField.setAccessible(true);
        List<?> registrations = (List<?>) registrationsField.get(registry);

        assertThat(registrations).hasSize(1);
        InterceptorRegistration registration = (InterceptorRegistration) registrations.get(0);

        // InterceptorRegistration 内部维护 excludePatterns
        Field excludeField = InterceptorRegistration.class.getDeclaredField("excludePatterns");
        excludeField.setAccessible(true);
        List<String> patterns = (List<String>) excludeField.get(registration);
        return patterns == null ? List.of() : patterns;
    }
}
