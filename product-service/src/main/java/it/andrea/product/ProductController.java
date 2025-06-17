package it.andrea.product;

import java.security.Principal;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/products")
public class ProductController {

	@GetMapping("/who-am-i")
	public Mono<String> whoAmI() {
		return Mono.just("Hello! You are an authenticated user.");
	}

	@GetMapping("/admin-dashboard")
	@PreAuthorize("hasRole('ADMIN')") 
	public Mono<String> getAdminDashboard() {
		return Mono.just("Welcome to the Admin Dashboard!");
	}

	@GetMapping("/user-profile")
	@PreAuthorize("hasAnyRole('USER', 'ADMIN')") 
	public Mono<String> getUserProfile() {
		return Mono.just("Welcome to your User Profile!");
	}

	@GetMapping("/authenticated-user-info")
	public Mono<String> authenticatedUserInfo(Mono<Principal> principal) {
		return principal.map(Principal::getName);
	}
	
}
