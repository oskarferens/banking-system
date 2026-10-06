package banking_system.security.infrastructure.adapter.out.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Date;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceTest {

    private static final String SECRET = "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970";
    private static final String OTHER_SECRET = "5A7134743777217A25432A462D4A614E645267556B58703273357638792F423F";

    private JwtService jwtService;
    private UserDetails userDetails;

    @BeforeEach
    void setUp() {
        jwtService = jwtServiceWith(SECRET, 3_600_000L);
        userDetails = userDetails("jan@bank.se");
    }

    private static JwtService jwtServiceWith(String secret, long expirationMillis) {
        JwtService service = new JwtService();
        ReflectionTestUtils.setField(service, "secretKey", secret);
        ReflectionTestUtils.setField(service, "jwtExpiration", expirationMillis);
        return service;
    }

    private static UserDetails userDetails(String username) {
        return User.withUsername(username).password("irrelevant").authorities("ROLE_USER").build();
    }

    @Test
    @DisplayName("a generated token carries the username as its subject")
    void tokenCarriesUsernameAsSubject() {
        String token = jwtService.generateToken(userDetails);

        assertThat(jwtService.extractUsername(token)).isEqualTo("jan@bank.se");
    }

    @Test
    @DisplayName("a generated token has the three JWT parts")
    void tokenHasThreeParts() {
        String token = jwtService.generateToken(userDetails);

        assertThat(token.split("\\.")).hasSize(3);
    }

    @Test
    @DisplayName("a fresh token is valid for the user it was issued to")
    void validForMatchingUser() {
        String token = jwtService.generateToken(userDetails);

        assertThat(jwtService.isTokenValid(token, userDetails)).isTrue();
    }

    @Test
    @DisplayName("the username comparison ignores case")
    void usernameComparisonIsCaseInsensitive() {
        String token = jwtService.generateToken(userDetails);

        assertThat(jwtService.isTokenValid(token, userDetails("JAN@BANK.SE"))).isTrue();
    }

    @Test
    @DisplayName("a token is not valid for a different user")
    void invalidForDifferentUser() {
        String token = jwtService.generateToken(userDetails);

        assertThat(jwtService.isTokenValid(token, userDetails("other@bank.se"))).isFalse();
    }

    @Test
    @DisplayName("extra claims survive a generate/parse round trip")
    void extraClaimsRoundTrip() {
        String token = jwtService.generateToken(Map.of("role", "ADMIN"), userDetails);

        String role = jwtService.extractClaim(token, claims -> claims.get("role", String.class));

        assertThat(role).isEqualTo("ADMIN");
    }

    @Test
    @DisplayName("the expiration lies in the future")
    void expirationIsInTheFuture() {
        String token = jwtService.generateToken(userDetails);

        Date expiration = jwtService.extractClaim(token, Claims::getExpiration);

        assertThat(expiration).isAfter(new Date());
    }

    @Test
    @DisplayName("an expired token is rejected with ExpiredJwtException")
    void expiredTokenIsRejected() {
        String expiredToken = jwtServiceWith(SECRET, -1_000L).generateToken(userDetails);

        assertThatThrownBy(() -> jwtService.isTokenValid(expiredToken, userDetails))
                .isInstanceOf(ExpiredJwtException.class);
    }

    @Test
    @DisplayName("a token signed with a different secret is rejected")
    void tokenSignedWithDifferentSecretIsRejected() {
        String foreignToken = jwtServiceWith(OTHER_SECRET, 3_600_000L).generateToken(userDetails);

        assertThatThrownBy(() -> jwtService.extractUsername(foreignToken))
                .isInstanceOf(JwtException.class);
    }

    @Test
    @DisplayName("a malformed token is rejected")
    void malformedTokenIsRejected() {
        assertThatThrownBy(() -> jwtService.extractUsername("not-a-jwt"))
                .isInstanceOf(JwtException.class);
    }
}