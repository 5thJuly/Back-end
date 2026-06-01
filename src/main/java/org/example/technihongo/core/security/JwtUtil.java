package org.example.technihongo.core.security;

import javax.crypto.SecretKey;

import org.example.technihongo.repositories.UserRepository;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import org.example.technihongo.services.interfaces.AuthTokenService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class JwtUtil {
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private JWTHelper jwtHelper;
    private static final Logger logger = LoggerFactory.getLogger(JwtUtil.class);

    @Value("${jwt.secret}")
    private String secretKey;

    @Autowired
    private AuthTokenService authTokenService;

    public int extractUserId(String token) {
        logger.info("Secret Key: {}", secretKey);
        logger.info("Token: {}", token);

        Claims claims = extractAllClaims(token);

        String email = jwtHelper.getEmailFromToken(token);
        int userId = userRepository.findByEmail(email).get().getUserId();

        if (claims == null) {
            logger.error("UID claim is missing or null");
            throw new IllegalArgumentException("Invalid token: uid claim is missing or null");
        }

        if(!authTokenService.isTokenValid(token)){
            throw new IllegalArgumentException("Token không hợp lệ!");
        }
        return userId;
    }
    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(secretKey.getBytes());
    }

    public int extractUserRoleId(String token) {
        logger.info("Secret Key: {}", secretKey);
        logger.info("Token: {}", token);

        Claims claims = extractAllClaims(token);

        String email = jwtHelper.getEmailFromToken(token);
        int roleId = userRepository.findByEmail(email).get().getRole().getRoleId();

//        if (claims == null || claims.get("cid") == null) {
        if (claims == null) {
            logger.error("uid claim is missing or null");
            throw new IllegalArgumentException("Invalid token: uid claim is missing or null");
        }

        if(!authTokenService.isTokenValid(token)){
            throw new IllegalArgumentException("Token không hợp lệ!");
        }

        //return Integer.parseInt(claims.get("cid").toString());
        return roleId;
    }

    private Claims extractAllClaims(String token) {
        try {
            return Jwts.parserBuilder()
                    .setSigningKey(getSigningKey())
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
        } catch (Exception e) {
            logger.error("Failed to extract claims: {}", e.getMessage());
            throw e;
        }
    }
}
