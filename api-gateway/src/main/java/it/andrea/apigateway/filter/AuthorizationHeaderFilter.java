package it.andrea.apigateway.filter;

import java.util.List;

import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;

import io.jsonwebtoken.Claims;
import it.andrea.apigateway.util.ValidateJwtUtil;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@RequiredArgsConstructor
@Component
public class AuthorizationHeaderFilter implements GatewayFilter {

    private final ValidateJwtUtil jwtUtil;
    private final RouterValidator routerValidator;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();

        if (routerValidator.isSecured.test(request)) {
            if (!request.getHeaders().containsKey("Authorization")) {
                ServerHttpResponse response = exchange.getResponse();
                response.setStatusCode(HttpStatus.UNAUTHORIZED);
                return response.setComplete();
            }

            String authHeader = request.getHeaders().getOrEmpty("Authorization").get(0);
            String token = authHeader.replace("Bearer ", "");

            try {
                jwtUtil.validateToken(token);
                Claims claims = jwtUtil.extractAllClaims(token);

                List<?> rawAuthorities = claims.get("authorities", List.class);
                if (rawAuthorities != null) {
                    // @formatter:off
                    List<String> authorities = rawAuthorities.stream() 
                            .map(Object::toString) 
                            .toList();
                    // @formatter:on

                    // @formatter:off
                    ServerHttpRequest modifiedRequest = request.mutate() 
                            .header("X-Auth-User", claims.getSubject()) 
                            .header("X-Auth-Roles", String.join(",", authorities)) 
                            .build();
                    // @formatter:on

                    return chain.filter(exchange.mutate().request(modifiedRequest).build());
                }
            } catch (Exception e) {
                ServerHttpResponse response = exchange.getResponse();
                response.setStatusCode(HttpStatus.UNAUTHORIZED);
                return response.setComplete();
            }
        }
        return chain.filter(exchange);
    }

}
