package it.andrea.apigateway.filter;

import java.util.List;

import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;

import io.jsonwebtoken.Claims;
import it.andrea.apigateway.util.ValidateJwtUtil;

@Component
public class AuthorizationHeaderGatewayFilterFactory extends AbstractGatewayFilterFactory<AuthorizationHeaderGatewayFilterFactory.Config> {
	
    private final ValidateJwtUtil jwtUtil;
    private final RouterValidator routerValidator;

    public AuthorizationHeaderGatewayFilterFactory(ValidateJwtUtil jwtUtil, RouterValidator routerValidator) {
        super(Config.class); 
        this.jwtUtil = jwtUtil;
        this.routerValidator = routerValidator;
    }

    // La classe Config è vuota perché le dipendenze sono iniettate nella factory,
    // e non ci sono parametri specifici da passare tramite YAML a questo filtro.
    public static class Config {
        // Nessun campo necessario qui per la tua logica attuale
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            ServerHttpRequest request = exchange.getRequest();

            if (routerValidator.isSecured.test(request)) {
                if (!request.getHeaders().containsKey(HttpHeaders.AUTHORIZATION)) {
                    ServerHttpResponse response = exchange.getResponse();
                    response.setStatusCode(HttpStatus.UNAUTHORIZED);
                    System.out.println("Missing Authorization header for secured route.");
                    return response.setComplete();
                }

                String authHeader = request.getHeaders().getOrEmpty(HttpHeaders.AUTHORIZATION).get(0);
                String token = authHeader.replace("Bearer ", "");

                try {
                    jwtUtil.validateToken(token);
                    Claims claims = jwtUtil.extractAllClaims(token);

                    List<?> rawAuthorities = claims.get("authorities", List.class);
                    if (rawAuthorities != null) {
                        List<String> authorities = rawAuthorities.stream()
                                .map(Object::toString)
                                .toList();

                        ServerHttpRequest modifiedRequest = request.mutate()
                                .header("X-Auth-User", claims.getSubject())
                                .header("X-Auth-Roles", String.join(",", authorities))
                                .build();

                        System.out.println("AuthorizationHeaderFilter applied. User: " + claims.getSubject());
                        return chain.filter(exchange.mutate().request(modifiedRequest).build());
                    }
                } catch (Exception e) {
                    ServerHttpResponse response = exchange.getResponse();
                    response.setStatusCode(HttpStatus.UNAUTHORIZED);
                    System.out.println("Invalid Authorization header: " + e.getMessage());
                    return response.setComplete();
                }
            }
            return chain.filter(exchange);
        };
    }

}
