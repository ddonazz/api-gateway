package it.andrea.apigateway.config;

import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity.FormLoginSpec;
import org.springframework.security.config.web.server.ServerHttpSecurity.HttpBasicSpec;
import org.springframework.security.web.server.SecurityWebFilterChain;

import it.andrea.apigateway.filter.AuthorizationHeaderFilter;

@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {
	private final AuthorizationHeaderFilter authorizationHeaderFilter;

	public SecurityConfig(AuthorizationHeaderFilter authorizationHeaderFilter) {
		this.authorizationHeaderFilter = authorizationHeaderFilter;
	}

    @Bean
    SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
		return http.csrf(ServerHttpSecurity.CsrfSpec::disable) //
				.authorizeExchange(exchanges -> exchanges.anyExchange().permitAll() //
				) //
				.httpBasic(HttpBasicSpec::disable) //
				.formLogin(FormLoginSpec::disable) //
				.build();
	}

	@Bean
	RouteLocator routes(RouteLocatorBuilder builder) {
		return builder.routes() //
				.route("authentication-service", r -> r.path("/auth/**") //
						.uri("lb://AUTHENTICATION-SERVICE")) //
				.route("my-backend-service", r -> r.path("/backend/**") //
						.filters(f -> f.filter(authorizationHeaderFilter) //
								.stripPrefix(1)) //
						.uri("lb://MY-BACKEND-SERVICE")) //
				.build();
	}
}
