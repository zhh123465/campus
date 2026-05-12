package com.campusforum.notify.websocket;

import cn.dev33.satoken.stp.StpUtil;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Map;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private final NotifyWebSocketHandler handler;

    public WebSocketConfig(NotifyWebSocketHandler handler) {
        this.handler = handler;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(handler, "/ws/notify")
                .setAllowedOrigins("*")
                .addInterceptors(new HandshakeInterceptor() {
                    @Override
                    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                                   WebSocketHandler wsHandler, Map<String, Object> attributes) {
                        String query = request.getURI().getQuery();
                        if (query == null) return false;
                        String token = null;
                        for (String param : query.split("&")) {
                            if (param.startsWith("token=")) {
                                token = param.substring(6);
                                break;
                            }
                        }
                        if (token == null) return false;
                        try {
                            Object loginId = StpUtil.getLoginIdByToken(token);
                            attributes.put("userId", Long.parseLong(loginId.toString()));
                            return true;
                        } catch (Exception e) {
                            return false;
                        }
                    }

                    @Override
                    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                               WebSocketHandler wsHandler, Exception exception) {
                    }
                });
    }
}
