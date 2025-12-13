package com.zjsu.nsq.gateway.filter;

import com.zjsu.nsq.gateway.util.JwtUtil;
import io.jsonwebtoken.Claims;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Component
public class JwtAuthenticationFilter extends AbstractGatewayFilterFactory<JwtAuthenticationFilter.Config> {

    private static final Logger logger = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    // 用于开发调试，可以设置为true来跳过认证
    private static final boolean SKIP_AUTH_FOR_DEBUG = false;

    @Autowired
    private JwtUtil jwtUtil;

    // 扩展白名单路径
    private static final List<String> WHITE_LIST = Arrays.asList(
            "/api/auth/login",
            "/api/auth/register",
            "/auth/login",           // 添加可能的前端路径
            "/auth/register",        // 添加可能的前端路径
            "/actuator/health",
            "/actuator/info",
            "/debug/",              // 调试端点
            "/test/",               // 测试端点
            "/swagger-ui",          // 如果有swagger
            "/v3/api-docs",         // OpenAPI文档
            "/webjars/",            // 静态资源
            "/swagger-resources",   // swagger资源
            "/api/users/register",  // 用户注册（如果需要）
            "/api/users/login"      // 用户登录（如果需要）
    );

    public JwtAuthenticationFilter() {
        super(Config.class);
    }

    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            ServerHttpRequest request = exchange.getRequest();
            String path = request.getPath().value();
            String method = request.getMethod().name();
            String requestId = exchange.getRequest().getId();

            // 打印请求基本信息
            logger.info("=== JWT过滤器开始处理请求 ===");
            logger.info("请求ID: {}, 方法: {}, 路径: {}", requestId, method, path);

            // 打印请求头（部分敏感信息打码）
            logger.debug("请求头信息:");
            request.getHeaders().forEach((headerName, headerValues) -> {
                if ("authorization".equalsIgnoreCase(headerName)) {
                    logger.debug("  {}: {}", headerName, "***HIDDEN***");
                } else {
                    logger.debug("  {}: {}", headerName, headerValues);
                }
            });

            // 调试模式：跳过认证
            if (SKIP_AUTH_FOR_DEBUG) {
                logger.warn("⚠️ JWT认证过滤器已跳过（调试模式），路径: {}", path);
                logger.info("=== JWT过滤器处理结束（调试模式跳过认证）===");
                return chain.filter(exchange);
            }

            // 1. 检查是否为白名单路径
            if (isWhiteList(path)) {
                logger.info("✅ 白名单路径，直接放行: {}", path);
                logger.info("=== JWT过滤器处理结束（白名单路径）===");
                return chain.filter(exchange);
            }

            // 2. 获取 Authorization 头
            String authHeader = request.getHeaders().getFirst("Authorization");
            logger.debug("Authorization头: {}",
                    authHeader != null ? (authHeader.length() > 20 ?
                            authHeader.substring(0, 20) + "..." : authHeader) : "null");

            if (!StringUtils.hasText(authHeader) || !authHeader.startsWith("Bearer ")) {
                logger.warn("❌ 请求缺少有效的 Authorization 头，路径: {}", path);
                logger.warn("   期望格式: 'Bearer <token>'");
                logger.warn("   实际值: {}", authHeader != null ? authHeader : "null");

                // 添加详细的错误信息到响应
                exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                exchange.getResponse().getHeaders().add("Content-Type", "application/json");
                exchange.getResponse().getHeaders().add("X-Auth-Error", "Missing or invalid Authorization header");

                String errorBody = String.format(
                        "{\"code\": 401, \"message\": \"未授权的访问，请提供有效的Authorization头\", " +
                                "\"path\": \"%s\", \"requiredFormat\": \"Bearer <token>\"}",
                        path
                );

                logger.info("=== JWT过滤器处理结束（缺少Authorization头）===");
                return exchange.getResponse()
                        .writeWith(Mono.just(exchange.getResponse()
                                .bufferFactory().wrap(errorBody.getBytes())));
            }

            // 3. 提取 Token
            String token = authHeader.substring(7); // 去掉 "Bearer " 前缀
            logger.debug("提取到的Token（前20位）: {}...",
                    token.length() > 20 ? token.substring(0, 20) : token);

            // 4. 验证 Token 有效性
            if (!jwtUtil.validateToken(token)) {
                logger.warn("❌ Token 验证失败，路径: {}", path);
                logger.warn("   可能原因：签名无效、格式错误或被篡改");

                exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                exchange.getResponse().getHeaders().add("Content-Type", "application/json");
                exchange.getResponse().getHeaders().add("X-Auth-Error", "Invalid token signature");

                String errorBody = String.format(
                        "{\"code\": 401, \"message\": \"Token验证失败，签名无效\", \"path\": \"%s\"}",
                        path
                );

                logger.info("=== JWT过滤器处理结束（Token验证失败）===");
                return exchange.getResponse()
                        .writeWith(Mono.just(exchange.getResponse()
                                .bufferFactory().wrap(errorBody.getBytes())));
            }

            // 5. 检查 Token 是否过期
            if (jwtUtil.isTokenExpired(token)) {
                logger.warn("❌ Token 已过期，路径: {}", path);

                exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                exchange.getResponse().getHeaders().add("Content-Type", "application/json");
                exchange.getResponse().getHeaders().add("X-Auth-Error", "Token expired");

                String errorBody = String.format(
                        "{\"code\": 401, \"message\": \"Token已过期，请重新登录\", \"path\": \"%s\"}",
                        path
                );

                logger.info("=== JWT过滤器处理结束（Token过期）===");
                return exchange.getResponse()
                        .writeWith(Mono.just(exchange.getResponse()
                                .bufferFactory().wrap(errorBody.getBytes())));
            }

            try {
                // 6. 解析 Token 获取用户信息
                logger.debug("开始解析Token...");
                Claims claims = jwtUtil.parseToken(token);

                String userId = claims.getSubject();
                String username = claims.get("username", String.class);
                String role = claims.get("role", String.class);
                String issuer = claims.getIssuer();
                String subject = claims.getSubject();
                String audience = claims.getAudience();

                // 打印详细的claims信息
                logger.info("✅ Token解析成功:");
                logger.info("   用户ID: {}", userId);
                logger.info("   用户名: {}", username);
                logger.info("   角色: {}", role);
                logger.info("   签发者: {}", issuer);
                logger.info("   主题: {}", subject);
                logger.info("   受众: {}", audience);
                logger.info("   签发时间: {}", claims.getIssuedAt());
                logger.info("   过期时间: {}", claims.getExpiration());

                // 打印所有claims（用于调试）
                logger.debug("所有Claims:");
                for (Map.Entry<String, Object> entry : claims.entrySet()) {
                    logger.debug("   {}: {}", entry.getKey(), entry.getValue());
                }

                // 7. 添加用户信息到请求头
                ServerHttpRequest mutatedRequest = request.mutate()
                        .header("X-User-Id", userId != null ? userId : "")
                        .header("X-Username", username != null ? username : "")
                        .header("X-User-Role", role != null ? role : "")
                        .header("X-Auth-Status", "authenticated")
                        .header("X-Request-Id", requestId)
                        .build();

                // 打印添加的请求头
                logger.debug("添加的请求头:");
                logger.debug("   X-User-Id: {}", userId);
                logger.debug("   X-Username: {}", username);
                logger.debug("   X-User-Role: {}", role);

                ServerWebExchange mutatedExchange = exchange.mutate()
                        .request(mutatedRequest)
                        .build();

                logger.info("✅ 用户认证成功，继续处理请求");
                logger.info("=== JWT过滤器处理结束（认证成功）===");
                return chain.filter(mutatedExchange);

            } catch (Exception e) {
                logger.error("❌ 解析 Token 失败: {}", e.getMessage(), e);

                exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                exchange.getResponse().getHeaders().add("Content-Type", "application/json");
                exchange.getResponse().getHeaders().add("X-Auth-Error", "Token parsing failed");

                String errorBody = String.format(
                        "{\"code\": 401, \"message\": \"Token解析失败: %s\", \"path\": \"%s\"}",
                        e.getMessage(), path
                );

                logger.info("=== JWT过滤器处理结束（Token解析异常）===");
                return exchange.getResponse()
                        .writeWith(Mono.just(exchange.getResponse()
                                .bufferFactory().wrap(errorBody.getBytes())));
            }
        };
    }

    private boolean isWhiteList(String path) {
        boolean isWhite = WHITE_LIST.stream().anyMatch(path::startsWith);
        logger.debug("检查白名单路径: {} -> {}", path, isWhite ? "在白名单中" : "不在白名单中");
        return isWhite;
    }

    public static class Config {
        // 配置属性
        private boolean enabled = true;
        private boolean logDetails = true;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public boolean isLogDetails() {
            return logDetails;
        }

        public void setLogDetails(boolean logDetails) {
            this.logDetails = logDetails;
        }
    }

    @Override
    public String name() {
        return "JwtAuthenticationFilter";
    }
}