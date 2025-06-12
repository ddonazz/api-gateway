package it.andrea.mybackend;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api")
public class MyBackendController {

	@GetMapping("/hello")
	public Mono<String> hello(@RequestHeader(value = "X-Auth-User", required = false) String username) {
		if (username != null) {
			return Mono.just("Hello " + username + " from My Reactive Backend Service (Port: 8081)!");
		}
		return Mono.just("Hello from My Reactive Backend Service (Port: 8081)!");
	}

	@GetMapping("/users")
	public Mono<String> getUsers(@RequestHeader(value = "X-Auth-Roles", required = false) String roles) {
		if (roles != null) {
			return Mono.just("Users list for roles: " + roles + " from My Reactive Backend Service (Port: 8081)!");
		}
		return Mono.just("Users list from My Reactive Backend Service (Port: 8081)!");
	}

}