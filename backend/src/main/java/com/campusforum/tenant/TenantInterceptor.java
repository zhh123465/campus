package com.campusforum.tenant;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Slf4j
@Component
public class TenantInterceptor implements HandlerInterceptor {

    @Value("${tenant.mode:standalone}")
    private String tenantMode;

    private static final long DEFAULT_TENANT_ID = 1L;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response,
                             Object handler) {
        if ("standalone".equals(tenantMode)) {
            TenantContext.setTenantId(DEFAULT_TENANT_ID);
            return true;
        }

        // 多租户模式：按优先级解析 tenant_id
        Long tenantId = resolveTenantId(request);
        TenantContext.setTenantId(tenantId);
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                Object handler, Exception ex) {
        TenantContext.clear();
    }

    private Long resolveTenantId(HttpServletRequest request) {
        // 1. HTTP Header: X-Tenant-Id
        String header = request.getHeader("X-Tenant-Id");
        if (header != null) {
            return Long.parseLong(header);
        }
        // 2. 子域名解析 (预留)
        String host = request.getServerName();
        if (host != null && host.contains(".")) {
            String subdomain = host.split("\\.")[0];
            log.debug("Resolved tenant from subdomain: {}", subdomain);
        }
        // 3. 默认租户
        return DEFAULT_TENANT_ID;
    }
}
