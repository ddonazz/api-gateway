package it.andrea.user;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/user")
public class UserController {

	@GetMapping("/who-am-i")
	public Mono<String> hello(@RequestHeader(value = "X-Auth-User", required = false) String username) {
		if (username == null || username.trim().isEmpty()) {
			return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST,
					"User authentication header (X-Auth-User) is missing or empty."));
		}

		if ("admin".equals(username)) {
			return Mono.just("Hello Admin!");
		}

		if ("user".equals(username)) {
			return Mono.just("Hello User!");
		}

		return Mono.error(new ResponseStatusException(HttpStatus.FORBIDDEN, "Unauthorized user: " + username));
	}

}
