package com.walletx.authservice.security;

import com.walletx.common.exception.WalletXException;
import com.walletx.authservice.utils.constant.Constants;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * Service responsible for JWT token operations within the WalletX platform.
 *
 * <p>Handles access token generation, claim extraction, token validation,
 * and structural verification using the JJWT library.</p>
 *
 * @see JwtAuthenticationFilter
 */
@Slf4j
@Service
public class JwtTokenService {

    /**
     * Base64-encoded secret key used to sign and verify JWT tokens.
     * Injected from the centralized configuration server.
     */
    @Value("${jwt.secret}")
    private String secret;

    /**
     * Access token expiration time in milliseconds.
     * Injected from the centralized configuration server.
     */
    @Value("${jwt.expiration}")
    private Long expiration;

    /**
     * Generates a JWT access token for the given user with no extra claims.
     *
     * @param userDetails the authenticated user details
     * @return a signed JWT access token string
     */
    public String generateAccessToken(UserDetails userDetails) {
        return buildToken(new HashMap<>(), userDetails, expiration);
    }

    /**
     * Generates a JWT access token for the given user with additional custom claims.
     *
     * @param extraClaims additional claims to embed in the token payload
     * @param userDetails the authenticated user details
     * @return a signed JWT access token string
     */
    public String generateAccessToken(Map<String, Object> extraClaims, UserDetails userDetails) {
        return buildToken(extraClaims, userDetails, expiration);
    }

    /**
     * Extracts the username (email) from the given JWT token.
     *
     * @param token the JWT token string
     * @return the email address embedded in the token subject
     */
    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    /**
     * Validates whether a JWT token is valid for the given user.
     *
     * <p>A token is considered valid if the username matches
     * and the token has not expired</p>
     *
     * @param token       the JWT token string to validate
     * @param userDetails the user details to validate against
     * @return {@code true} if the token is valid, {@code false} otherwise
     */
    public boolean isTokenValid(String token, UserDetails userDetails) {
        String username = extractUsername(token);
        return username.equals(userDetails.getUsername()) && !isTokenExpired(token);
    }

    /**
     * Validates the structure, signature, and expiration of a JWT token.
     *
     * <p>This method performs structural validation only - it does not
     * verify ownership of the token. Use {@link #isTokenValid(String, UserDetails)}</p>
     * for full validation.</p>
     *
     * @param token the JWT token string to validate
     * @throws WalletXException if the token has an invalid signature, is malformed,
     *                          has expired, or contains empty claims
     */
    public void validateTokenStructure(String token) {
        try {
            Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token);
        } catch (SignatureException exception) {
            log.error("Invalid JWT signature: {}", exception.getMessage());
            throw new WalletXException(HttpStatus.UNAUTHORIZED, Constants.MessageKeys.TOKEN_INVALID_SIGNATURE);
        } catch (MalformedJwtException exception) {
            log.error("Malformed JWT token: {}", exception.getMessage());
            throw new WalletXException(HttpStatus.UNAUTHORIZED, Constants.MessageKeys.TOKEN_MALFORMED);
        } catch (ExpiredJwtException exception) {
            log.error("Expired JWT token: {}", exception.getMessage());
            throw new WalletXException(HttpStatus.UNAUTHORIZED, Constants.MessageKeys.TOKEN_EXPIRED);
        } catch (IllegalArgumentException exception) {
            log.error("JWT claims string is empty: {}", exception.getMessage());
            throw new WalletXException(HttpStatus.UNAUTHORIZED, Constants.MessageKeys.TOKEN_EMPTY);
        }
    }

    /**
     * Extracts a specific claim from the given JWT token using a resolver function.
     *
     * <p>This is a generic method that supports extracting any claim type.
     * Common usage: </p>
     * <pre>
     *     String email = extractClaim(token, Claims::getSubject);
     *     Date expiration = extractClaim(token, Claims::getExpiration);
     * </pre>
     *
     * @param <T>            The type of the claim to extract
     * @param claimsResolver A function that extracts the desired claim from {@link Claims}
     * @return the extracted claim value
     */
    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    /**
     * Builds a signed JWT token with the given claims, user details, and expiration.
     *
     * @param extraClaims     Additional claims to include in the token payload
     * @param userDetails     The authenticated user details
     * @param tokenExpiration the token expiration time in milliseconds
     * @return a compact signed JWT token string
     */
    private String buildToken(Map<String, Object> extraClaims, UserDetails userDetails, Long tokenExpiration) {
        return Jwts.builder()
                .claims(extraClaims)
                .subject(userDetails.getUsername())
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + tokenExpiration))
                .signWith(getSigningKey())
                .compact();
    }

    /**
     * Checks whether the given JWT token has expired.
     *
     * @param token the JWT token string
     * @return {@code true} if the token has expired, {@code false} otherwise
     */
    private boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    /**
     * Extracts the expiration date from the given JWT token.
     *
     * @param token the JWT token string
     * @return the expiration {@link Date} of the token
     */
    private Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    /**
     * Parses and returns all claims from the given JWT token.
     *
     * @param token the JWT token string
     * @return the {@link Claims} payload of the token
     */
    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * Decodes the base64-encoded secret and builds the HMAC-SHA signing key.
     *
     * <p>The secret must be a valid Base64-encoded string with sufficient
     * entropy for HMAC-SHA256 signing.</p>
     *
     * @return the {@link SecretKey} used for sing and verifying JWT tokens
     */
    private SecretKey getSigningKey() {
        byte[] keyBytes = Decoders.BASE64.decode(secret);
        return Keys.hmacShaKeyFor(keyBytes);
    }

}
