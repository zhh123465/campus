package com.campusforum.security;

import cn.dev33.satoken.context.SaHolder;
import cn.dev33.satoken.interceptor.SaInterceptor;
import cn.dev33.satoken.router.SaRouter;
import cn.dev33.satoken.stp.StpUtil;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class SaTokenConfig implements WebMvcConfigurer {

    private static final String[] PUBLIC_GET_PREFIXES = {
            "/api/v1/tenant/info",
            "/api/v1/posts",
            "/api/v1/comments",
            "/api/v1/spaces",
            "/api/v1/resources",
            "/api/v1/search",
            "/api/v1/achievements",
            "/api/v1/checkin/challenges",
            "/api/v1/follows",
            "/api/v1/qa",
            "/api/v1/users/",
            "/api/v1/ai/post-card/",
            "/api/v1/ai/agents",
            "/api/v1/ai/plugins",
            "/api/v1/ai/plugin"
    };

    private static final String[] PRIVATE_GET_PREFIXES = {
            "/api/v1/users/me",
            "/api/v1/messages",
            "/api/v1/notifications",
            "/api/v1/admin",
            "/api/v1/auth/me",
            "/api/v1/ai/conversations",
            "/api/v1/ai/knowledge-bases",
            "/api/v1/ai/knowledge-ingest-tasks"
    };

    private static final String[] PUBLIC_NON_GET_PATHS = {
            "/api/v1/auth/login",
            "/api/v1/auth/register",
            "/api/v1/auth/email-code",
            "/api/v1/auth/email-exists",
            "/api/v1/auth/forgot-password",
            "/api/v1/auth/reset-password",
            "/api/v1/auth/wechat-login",
            "/api/v1/auth/qq-login",
            "/api/v1/auth/github-device-code",
            "/api/v1/auth/github-device-login",
            "/api/v1/ai/post-cards/batch"
    };

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new SaInterceptor(handle -> {
            // 对 /api/v1/** 路由应用判定
            SaRouter.match("/api/v1/**")
                    // 放行认证相关接口
                    .notMatch(
                            "/api/v1/auth/login",
                            "/api/v1/auth/register",
                            "/api/v1/auth/email-code",
                            "/api/v1/auth/email-exists",
                            "/api/v1/auth/forgot-password",
                            "/api/v1/auth/reset-password",
                            "/api/v1/auth/wechat-login",
                            "/api/v1/auth/qq-login",
                            "/api/v1/auth/github-device-code",
                            "/api/v1/auth/github-device-login"
                    )
                    // 放行租户和直连文件下载/预览接口，以及游客能调用的无副作用 POST 接口
                    .notMatch(
                            "/api/v1/tenant/info",
                            "/api/v1/resources/*/download",
                            "/api/v1/resources/*/preview",
                            "/api/v1/ai/post-cards/batch"
                    )
                    .check(r -> {
                        String path = SaHolder.getRequest().getRequestPath();
                        String method = SaHolder.getRequest().getMethod();

                        if (requiresLogin(method, path)) {
                            StpUtil.checkLogin();
                        }
                    });
        })).addPathPatterns("/api/v1/**");
    }

    static boolean requiresLogin(String method, String path) {
        if ("GET".equalsIgnoreCase(method)) {
            return requiresLoginForGet(path);
        }
        if (path != null) {
            for (String publicPath : PUBLIC_NON_GET_PATHS) {
                if (publicPath.equals(path)) {
                    return false;
                }
            }
        }
        // 所有未显式放行的写操作（POST, PUT, DELETE, PATCH）均强制要求登录
        return true;
    }

    static boolean requiresLoginForGet(String path) {
        if (path == null || path.isBlank()) {
            return true;
        }
        if (isPublicResourceStream(path)) {
            return false;
        }
        if (isResourceSignedUrl(path)) {
            return true;
        }
        if (isSpaceAdminRead(path)) {
            return true;
        }
        for (String prefix : PRIVATE_GET_PREFIXES) {
            if (path.startsWith(prefix)) {
                return true;
            }
        }
        for (String prefix : PUBLIC_GET_PREFIXES) {
            if (path.startsWith(prefix)) {
                return false;
            }
        }
        return true;
    }

    private static boolean isPublicResourceStream(String path) {
        if (!path.startsWith("/api/v1/resources/")) {
            return false;
        }
        return path.endsWith("/download") || path.endsWith("/preview") || path.endsWith("/preview-text");
    }

    private static boolean isResourceSignedUrl(String path) {
        return path.startsWith("/api/v1/resources/") && path.endsWith("/signed-url");
    }

    private static boolean isSpaceAdminRead(String path) {
        return path.startsWith("/api/v1/spaces/") && path.contains("/posts/all");
    }
}
