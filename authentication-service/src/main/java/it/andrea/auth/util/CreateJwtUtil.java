package it.andrea.auth.util;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;

@Component
public class CreateJwtUtil {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration}")
    private Long expiration;

    private SecretKey getSigningKey() {
        byte[] keyBytes = Decoders.BASE64.decode(secret);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    public String generateToken(UserDetails userDetails) {
        // @formatter:off
		Map<String, Object> claims = new HashMap<>();
		claims.put("authorities", userDetails.getAuthorities().stream() 
				.map(GrantedAuthority::getAuthority) 
				.toList()); 
		// @formatter:on

        return createToken(claims, userDetails.getUsername());
    }

    private String createToken(Map<String, Object> claims, String subject) {
        // @formatter:off
		return Jwts.builder() 
				.claims(claims) 
				.subject(subject) 
				.issuedAt(new Date(System.currentTimeMillis())) 
				.expiration(new Date(System.currentTimeMillis() + expiration)) 
				.signWith(getSigningKey()) 
				.compact();
		// @formatter:on
    }

}
