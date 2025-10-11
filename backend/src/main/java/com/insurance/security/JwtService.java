package com.insurance.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * ╔══════════════════════════════════════════════════════════════════════════╗
 * ║                          JWT SERVICE                                    ║
 * ╠══════════════════════════════════════════════════════════════════════════╣
 * ║                                                                          ║
 * ║  Handles all JWT (JSON Web Token) operations:                           ║
 * ║  • Token generation (on login/register)                                 ║
 * ║  • Token validation (on every secured request)                          ║
 * ║  • Claims extraction (username, expiry, custom claims)                  ║
 * ║                                                                          ║
 * ║  HOW JWT WORKS:                                                          ║
 * ║  ─────────────────────────────────────────────────────────────────────  ║
 * ║  Structure: <Header>.<Payload>.<Signature>                              ║
 * ║  Each part is Base64URL encoded (NOT encrypted — just encoded!)         ║
 * ║                                                                          ║
 * ║  Header:    {"alg": "HS256", "typ": "JWT"}                             ║
 * ║  Payload:   {"sub": "john@email.com", "iat": 1234, "exp": 5678,       ║
 * ║              "role": "ROLE_CUSTOMER"}                                   ║
 * ║  Signature: HMAC_SHA256(Base64(header) + "." + Base64(payload), secret) ║
 * ║                                                                          ║
 * ║  VALIDATION: Server re-computes the signature using the same secret.   ║
 * ║  If computed signature ≠ received signature → token was tampered with. ║
 * ║  If exp timestamp < now → token is expired.                            ║
 * ║                                                                          ║
 * ║  STATELESS: Server never stores tokens. Each request is self-contained. ║
 * ║  This allows horizontal scaling (any server can validate any token).    ║
 * ║                                                                          ║
 * ║  @Slf4j: Lombok generates a Logger field:                              ║
 * ║    private static final Logger log = LoggerFactory.getLogger(JwtService.class); ║
 * ╚══════════════════════════════════════════════════════════════════════════╝
 */
@Service
@Slf4j
public class JwtService {

    /**
     * @Value: Injects values from application.yml at startup.
     * The secret key is used for HMAC-SHA256 signing.
     *
     * SECURITY: This key must be:
     * 1. At least 256 bits (32 bytes) for HS256
     * 2. Stored as environment variable in production
     * 3. Never committed to version control
     *
     * The value is Base64-encoded in application.yml and decoded here.
     */
    @Value("${application.security.jwt.secret-key}")
    private String secretKey;

    /**
     * Token validity period in milliseconds.
     * Default: 86400000ms = 24 hours.
     * After expiry, client must login again to get a new token.
     */
    @Value("${application.security.jwt.expiration}")
    private long jwtExpiration;

    // ════════════════════════════════════════════════════════════════════════
    // TOKEN GENERATION
    // ════════════════════════════════════════════════════════════════════════

    /**
     * Generates a JWT token for the given user without extra claims.
     * Called after successful login/registration.
     *
     * @param userDetails The authenticated user (implements UserDetails)
     * @return JWT token string (Header.Payload.Signature format)
     */
    public String generateToken(UserDetails userDetails) {
        return generateToken(new HashMap<>(), userDetails);
    }

    /**
     * Generates a JWT token with additional custom claims.
     *
     * INTERVIEW TIP: JWT Claims are key-value pairs in the payload.
     * Standard claims (registered): sub, iat, exp, iss, aud
     * Private claims: any custom data (role, userId, etc.)
     *
     * @param extraClaims Additional claims to embed in the token payload
     * @param userDetails The authenticated user
     * @return JWT token string
     */
    public String generateToken(Map<String, Object> extraClaims, UserDetails userDetails) {
        log.debug("Generating JWT token for user: {}", userDetails.getUsername());
        return buildToken(extraClaims, userDetails, jwtExpiration);
    }

    /**
     * Core token building logic using the JJWT builder API.
     *
     * JJWT API (0.12.x) is fluent builder style.
     * The token is signed with HMAC-SHA256 using our secret key.
     *
     * @param extraClaims  Additional custom claims for the payload
     * @param userDetails  User whose identity is encoded in the token
     * @param expiration   Validity duration in milliseconds
     * @return Signed and compacted JWT string
     */
    private String buildToken(
            Map<String, Object> extraClaims,
            UserDetails userDetails,
            long expiration
    ) {
        return Jwts.builder()
                // Standard "subject" claim: the username (email in our case)
                .subject(userDetails.getUsername())
                // "issued at": current timestamp
                .issuedAt(new Date(System.currentTimeMillis()))
                // "expiration": current time + validity period
                .expiration(new Date(System.currentTimeMillis() + expiration))
                // Additional custom claims (role, userId, etc.)
                .claims(extraClaims)
                // HMAC-SHA256 signing with our secret key
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                // Serialize to a compact, URL-safe string (Header.Payload.Signature)
                .compact();
    }

    // ════════════════════════════════════════════════════════════════════════
    // TOKEN VALIDATION
    // ════════════════════════════════════════════════════════════════════════

    /**
     * Validates a JWT token against a UserDetails object.
     * Checks: (1) token's username matches the provided UserDetails, AND
     *         (2) token has not expired.
     *
     * Called by JwtAuthenticationFilter on every secured request.
     *
     * @param token       JWT token from the Authorization header
     * @param userDetails The user loaded from database by username
     * @return true if the token is valid for this user
     */
    public boolean isTokenValid(String token, UserDetails userDetails) {
        final String username = extractUsername(token);
        boolean isValid = username.equals(userDetails.getUsername()) && !isTokenExpired(token);

        if (!isValid) {
            log.warn("JWT token validation failed for user: {}", userDetails.getUsername());
        }
        return isValid;
    }

    /**
     * Checks if the token's expiration claim is in the past.
     *
     * @param token JWT token string
     * @return true if token has expired
     */
    private boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    // ════════════════════════════════════════════════════════════════════════
    // CLAIMS EXTRACTION
    // ════════════════════════════════════════════════════════════════════════

    /**
     * Extracts the username (email) from the JWT token's "sub" (subject) claim.
     * The subject claim stores the user's email address.
     *
     * @param token JWT token string
     * @return The username (email) stored in the token
     */
    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    /**
     * Extracts the expiration date from the JWT token.
     *
     * @param token JWT token string
     * @return Token expiration Date
     */
    private Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    /**
     * Generic claim extractor using a Function resolver.
     *
     * INTERVIEW TIP: This is a functional programming pattern.
     * Function<Claims, T> is passed in to extract any specific field.
     * This avoids duplicating the parsing logic for each claim type.
     *
     * Example usage:
     *   extractClaim(token, Claims::getSubject)   → gets "sub"
     *   extractClaim(token, Claims::getExpiration) → gets "exp"
     *
     * @param token          JWT token string
     * @param claimsResolver Function that maps Claims → desired value
     * @param <T>            Return type of the extracted claim
     * @return The extracted claim value
     */
    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    /**
     * Parses the JWT token and returns all claims in the payload.
     *
     * JJWT validation flow:
     * 1. Base64URL-decode the header and payload
     * 2. Re-compute the signature using our secret key
     * 3. If computed ≠ received → throw SignatureException (token tampered)
     * 4. Check 'exp' claim → throw ExpiredJwtException if expired
     * 5. Return the Claims object with all payload data
     *
     * @param token JWT token string
     * @return All claims from the token's payload
     * @throws io.jsonwebtoken.JwtException if token is invalid or expired
     */
    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())  // sets the key for signature verification
                .build()
                .parseSignedClaims(token)
                .getPayload();               // returns the Claims (payload) object
    }

    // ════════════════════════════════════════════════════════════════════════
    // KEY MANAGEMENT
    // ════════════════════════════════════════════════════════════════════════

    /**
     * Creates the cryptographic signing key from the Base64-encoded secret.
     *
     * INTERVIEW TIP: HMAC-SHA256 (HS256) uses a symmetric key — the same key
     * is used for both signing (server) and verification (server).
     * For asymmetric JWT (RS256), you'd use a private key for signing and
     * the public key for verification — useful when third parties need to
     * verify tokens without having the signing secret.
     *
     * Keys.hmacShaKeyFor(): Creates a SecretKey from bytes.
     * JJWT validates that the key length is appropriate for the algorithm.
     *
     * @return SecretKey for HMAC-SHA256 operations
     */
    private Key getSigningKey() {
        byte[] keyBytes = Decoders.BASE64.decode(secretKey);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
