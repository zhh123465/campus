package com.campusforum.infra;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.TenantLineInnerInterceptor;
import com.campusforum.tenant.TenantContext;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.LongValue;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Set;

@Configuration
public class MyBatisPlusConfig {

    /** 不需要做租户隔离的系统表/字典表 */
    private static final Set<String> TENANT_IGNORE_TABLES = Set.of(
            "tenants", "achievements", "sensitive_words"
    );

    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();

        // 多租户 SQL 自动改写（仅 multi 模式启用）
        interceptor.addInnerInterceptor(new TenantLineInnerInterceptor(() ->
                new LongValue(TenantContext.getTenantId())) {
            @Override
            protected Expression getTenantId(org.apache.ibatis.mapping.MappedStatement ms,
                                             String tableName) {
                if (TENANT_IGNORE_TABLES.contains(tableName)) {
                    return null;
                }
                return new LongValue(TenantContext.getTenantId());
            }
        });

        // 分页插件
        PaginationInnerInterceptor pagination = new PaginationInnerInterceptor(DbType.MYSQL);
        pagination.setMaxLimit(100L);
        interceptor.addInnerInterceptor(pagination);

        return interceptor;
    }
}
