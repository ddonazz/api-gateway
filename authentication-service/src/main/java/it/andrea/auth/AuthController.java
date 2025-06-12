package it.andrea.auth;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.ReactiveAuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import it.andrea.auth.util.CreateJwtUtil;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

	private final ReactiveAuthenticationManager authenticationManager;
	private final CreateJwtUtil jwtUtil;
	private final CustomUserDetailsService userDetailsService;

	@PostMapping("/login")
	public Mono<ResponseEntity<AuthResponse>> login(@RequestBody LoginRequest loginRequest) {
		return authenticationManager 
				.authenticate(
						new UsernamePasswordAuthenticationToken(loginRequest.getUsername(), loginRequest.getPassword())) //
				.flatMap(authentication -> userDetailsService.findByUsername(authentication.getName())) //
				.map(userDetails -> {
					String token = jwtUtil.generateToken(userDetails);
					return ResponseEntity.ok(new AuthResponse(token));
				}).switchIfEmpty(Mono.just(ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()));
	}

}
