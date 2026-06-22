package com.campusforum.user.service;

import cn.dev33.satoken.session.SaSession;
import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.campusforum.infra.audit.AuditLogService;
import com.campusforum.infra.email.EmailCodeScene;
import com.campusforum.infra.metrics.SecurityMetrics;
import com.campusforum.infra.security.LoginLockoutService;
import com.campusforum.infra.security.SecurityProperties;
import com.campusforum.infra.security.TrustedProxyResolver;
import com.campusforum.social.service.GithubOAuthClient;
import com.campusforum.social.service.GithubTokenPollResult;
import com.campusforum.social.service.GithubUserInfo;
import com.campusforum.social.service.QqOAuthClient;
import com.campusforum.social.service.QqUserInfo;
import com.campusforum.tenant.TenantContext;
import com.campusforum.tenant.cache.ActiveTenantCache;
import com.campusforum.user.config.StudentNoMappingProperties;
import com.campusforum.user.domain.User;
import com.campusforum.user.mapper.UserMapper;
import com.campusforum.wechat.service.WechatMiniProgramClient;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mindrot.jbcrypt.BCrypt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UserServiceSocialLoginTest {

    private UserService userService;
    private UserMapper userMapper;
    private QqOAuthClient qqOAuthClient;
    private GithubOAuthClient githubOAuthClient;
    private ActiveTenantCache activeTenantCache;
    private MockedStatic<StpUtil> stpUtilMock;
    private MockedStatic<BCrypt> bcryptMock;

    @BeforeEach
    void setUp() {
        userMapper = mock(UserMapper.class);
        qqOAuthClient = mock(QqOAuthClient.class);
        githubOAuthClient = mock(GithubOAuthClient.class);
        activeTenantCache = mock(ActiveTenantCache.class);
        when(activeTenantCache.getCode(1L)).thenReturn("default");

        userService = new UserService(
                userMapper,
                mock(StudentNoMappingProperties.class),
                activeTenantCache,
                mock(LoginLockoutService.class),
                mock(EmailVerificationCodeService.class),
                mock(TrustedProxyResolver.class),
                mock(HttpServletRequest.class),
                mock(SecurityProperties.class),
                mock(AuditLogService.class),
                mock(SecurityMetrics.class),
                mock(WechatMiniProgramClient.class),
                qqOAuthClient,
                githubOAuthClient);

        stpUtilMock = mockStatic(StpUtil.class);
        SaSession session = mock(SaSession.class);
        stpUtilMock.when(() -> StpUtil.login(any())).thenAnswer(inv -> null);
        stpUtilMock.when(StpUtil::getSession).thenReturn(session);

        bcryptMock = mockStatic(BCrypt.class);
        bcryptMock.when(() -> BCrypt.gensalt(10)).thenReturn("$2a$10$dummy.salt.value.padding.padding..");
        bcryptMock.when(() -> BCrypt.hashpw(any(), any())).thenReturn("$2a$10$new.hash.value.for.social.login.padding");

        TenantContext.setTenantId(1L);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
        stpUtilMock.close();
        bcryptMock.close();
    }

    @Test
    @DisplayName("QQ 登录：首次登录应创建本地用户并写入 qqOpenid")
    void qqLoginShouldCreateLocalUser() {
        when(qqOAuthClient.getUserInfo("qq-openid", "token"))
                .thenReturn(new QqUserInfo("qq-openid", "QQ昵称", "https://q.qlogo.cn/avatar.jpg"));
        when(userMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);
        when(userMapper.insert(any(User.class))).thenAnswer(inv -> {
            User user = inv.getArgument(0);
            user.setId(11L);
            return 1;
        });

        var user = userService.loginByQq("qq-openid", "token");

        assertThat(user.getId()).isEqualTo(11L);
        assertThat(user.getNickname()).isEqualTo("QQ昵称");
        verify(userMapper).insert(org.mockito.ArgumentMatchers.<User>argThat(saved ->
                "qq-openid".equals(saved.getQqOpenid())
                        && saved.getEmail().startsWith("qq_")
                        && saved.getEmail().endsWith("@qq.local")));
    }

    @Test
    @DisplayName("GitHub 设备登录：authorization_pending 时不创建用户")
    void githubDeviceLoginShouldReturnPendingWithoutCreatingUser() {
        when(githubOAuthClient.pollToken("device-code"))
                .thenReturn(new GithubTokenPollResult(true, 5, null));

        UserService.GithubLoginResult result = userService.loginByGithubDeviceCode("device-code");

        assertThat(result.pending()).isTrue();
        assertThat(result.retryAfterSeconds()).isEqualTo(5);
        org.mockito.Mockito.verifyNoInteractions(userMapper);
    }

    @Test
    @DisplayName("GitHub 设备登录：授权完成后创建本地用户并写入 githubId")
    void githubDeviceLoginShouldCreateLocalUserAfterAuthorized() {
        when(githubOAuthClient.pollToken("device-code"))
                .thenReturn(new GithubTokenPollResult(false, 0, "gh-token"));
        when(githubOAuthClient.getUserInfo("gh-token"))
                .thenReturn(new GithubUserInfo("12345", "octo", "Octo Cat", "https://avatars.githubusercontent.com/u/12345"));
        when(userMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);
        when(userMapper.insert(any(User.class))).thenAnswer(inv -> {
            User user = inv.getArgument(0);
            user.setId(12L);
            return 1;
        });

        UserService.GithubLoginResult result = userService.loginByGithubDeviceCode("device-code");

        assertThat(result.pending()).isFalse();
        assertThat(result.user().getId()).isEqualTo(12L);
        assertThat(result.user().getNickname()).isEqualTo("Octo Cat");
        verify(userMapper).insert(org.mockito.ArgumentMatchers.<User>argThat(saved ->
                "12345".equals(saved.getGithubId())
                        && saved.getEmail().startsWith("github_")
                        && saved.getEmail().endsWith("@github.local")));
    }
}
