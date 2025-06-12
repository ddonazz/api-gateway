package it.andrea.apigateway.filter;

import java.util.List;
import java.util.function.Predicate;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;

@Component
public class RouterValidator {

    @Value("${gateway.public-paths}")
    private List<String> publicPaths;

    // @formatter:off
    public final Predicate<ServerHttpRequest> isSecured = request -> publicPaths .stream() 
            .noneMatch(uri -> request.getURI().getPath().contains(uri));
    // @formatter:on
}
