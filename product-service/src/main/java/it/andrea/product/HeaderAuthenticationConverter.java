package it.andrea.product;

import java.util.Arrays;
import java.util.List;

import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.web.server.authentication.ServerAuthenticationConverter;
import org.springframework.web.server.ServerWebExchange;

import reactor.core.publisher.Mono;

public class HeaderAuthenticationConverter implements ServerAuthenticationConverter {
	
	private static final String USER_HEADER = "X-Auth-User";
    private static final String ROLES_HEADER = "X-Auth-Roles";
    private static final String ROLE_PREFIX = "ROLE_";

    @Override
    public Mono<Authentication> convert(ServerWebExchange exchange) {
        HttpHeaders headers = exchange.getRequest().getHeaders();

        String username = headers.getFirst(USER_HEADER);
        String rolesHeader = headers.getFirst(ROLES_HEADER);

        if (username == null || rolesHeader == null) {
            return Mono.empty();
        }

        List<SimpleGrantedAuthority> authorities = Arrays.stream(rolesHeader.split(","))
                .map(String::trim)
                .filter(role -> !role.isEmpty())
                .map(role -> new SimpleGrantedAuthority(ROLE_PREFIX + role))
                .toList();

        Authentication authentication = new UsernamePasswordAuthenticationToken(username, null, authorities);
        
        return Mono.just(authentication);
    }

}
