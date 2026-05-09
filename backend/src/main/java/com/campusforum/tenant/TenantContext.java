package com.campusforum.tenant;

/**
 * 租户上下文 — 基于 ThreadLocal 的租户信息传递。
 */
public final class TenantContext {

    private static final ThreadLocal<Long> TENANT_HOLDER = new ThreadLocal<>();
    private static final ThreadLocal<String> TENANT_CODE_HOLDER = new ThreadLocal<>();

    private TenantContext() {}

    public static void setTenantId(Long tenantId) {
        TENANT_HOLDER.set(tenantId);
    }

    public static Long getTenantId() {
        return TENANT_HOLDER.get();
    }

    public static void setTenantCode(String code) {
        TENANT_CODE_HOLDER.set(code);
    }

    public static String getTenantCode() {
        return TENANT_CODE_HOLDER.get();
    }

    public static void clear() {
        TENANT_HOLDER.remove();
        TENANT_CODE_HOLDER.remove();
    }
}
