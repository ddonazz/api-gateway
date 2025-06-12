package it.andrea.auth;

import org.springframework.security.core.userdetails.ReactiveUserDetailsService;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import reactor.core.publisher.Mono;

@Service
public class CustomUserDetailsService implements ReactiveUserDetailsService {

    private final PasswordEncoder passwordEncoder;

    public CustomUserDetailsService(PasswordEncoder passwordEncoder) {
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public Mono<UserDetails> findByUsername(String username) {
        if ("user".equals(username)) {
            // @formatter:off
			return Mono.just(User.withUsername("user") 
					.password(passwordEncoder.encode("password")) 
					.roles("USER") 
					.build());
			// @formatter:on
        }
        if ("admin".equals(username)) {
            // @formatter:off
			return Mono.just(User.withUsername("admin") 
					.password(passwordEncoder.encode("admin")) 
					.roles("ADMIN", "USER") 
					.build());
			// @formatter:on
        }
        return Mono.empty();
    }

}
