package banking_system.security.infrastructure.adapter.out.jwt;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    private static final String SECRET = "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970";

    @Mock
    private UserDetailsService userDetailsService;

    private JwtService jwtService;
    private JwtAuthenticationFilter filter;
    private UserDetails userDetails;

    @BeforeEach
    void setUp() {
        jwtService = jwtServiceWith(3_600_000L);
        filter = new JwtAuthenticationFilter(jwtService, userDetailsService);
        userDetails = userDetails("jan@bank.se");
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private static JwtService jwtServiceWith(long expirationMillis) {
        JwtService service = new JwtService();
        ReflectionTestUtils.setField(service, "secretKey", SECRET);
        ReflectionTestUtils.setField(service, "jwtExpiration", expirationMillis);
        return service;
    }

    private static UserDetails userDetails(String username) {
        return User.withUsername(username).password("irrelevant").authorities("ROLE_USER").build();
    }

    private MockFilterChain runFilter(String authorizationHeader) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        if (authorizationHeader != null) {
            request.addHeader("Authorization", authorizationHeader);
        }
        MockFilterChain chain = new MockFilterChain();
        filter.doFilter(request, new MockHttpServletResponse(), chain);
        return chain;
    }

    @Test
    @DisplayName("a request without an Authorization header passes through unauthenticated")
    void requestWithoutHeaderPassesThrough() throws Exception {
        MockFilterChain chain = runFilter(null);

        assertThat(chain.getRequest()).isNotNull();
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verifyNoInteractions(userDetailsService);
    }

    @Test
    @DisplayName("a non-Bearer Authorization header passes through unauthenticated")
    void nonBearerHeaderPassesThrough() throws Exception {
        MockFilterChain chain = runFilter("Basic dXNlcjpwYXNz");

        assertThat(chain.getRequest()).isNotNull();
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verifyNoInteractions(userDetailsService);
    }

    @Test
    @DisplayName("a valid Bearer token authenticates the request with the user's authorities")
    void validBearerTokenAuthenticatesRequest() throws Exception {
        String token = jwtService.generateToken(userDetails);
        when(userDetailsService.loadUserByUsername("jan@bank.se")).thenReturn(userDetails);

        MockFilterChain chain = runFilter("Bearer " + token);

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertThat(authentication).isNotNull();
        assertThat(authentication.getName()).isEqualTo("jan@bank.se");
        List<String> roles = authentication.getAuthorities().stream().map(GrantedAuthority::getAuthority).toList();
        assertThat(roles).containsExactly("ROLE_USER");
        assertThat(chain.getRequest()).isNotNull();
    }

    @Test
    @DisplayName("the Bearer prefix is matched case-insensitively")
    void bearerPrefixIsCaseInsensitive() throws Exception {
        String token = jwtService.generateToken(userDetails);
        when(userDetailsService.loadUserByUsername("jan@bank.se")).thenReturn(userDetails);

        runFilter("bearer " + token);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
    }

    @Test
    @DisplayName("a token wrapped in quotes is accepted")
    void quotedTokenIsAccepted() throws Exception {
        String token = jwtService.generateToken(userDetails);
        when(userDetailsService.loadUserByUsername("jan@bank.se")).thenReturn(userDetails);

        runFilter("Bearer \"" + token + "\"");

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
    }

    @Test
    @DisplayName("an invalid token is ignored and the request continues unauthenticated")
    void invalidTokenIsIgnored() throws Exception {
        MockFilterChain chain = runFilter("Bearer not.a.jwt");

        assertThat(chain.getRequest()).isNotNull();
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verifyNoInteractions(userDetailsService);
    }

    @Test
    @DisplayName("an expired token is ignored and the request continues unauthenticated")
    void expiredTokenIsIgnored() throws Exception {
        String expiredToken = jwtServiceWith(-1_000L).generateToken(userDetails);

        MockFilterChain chain = runFilter("Bearer " + expiredToken);

        assertThat(chain.getRequest()).isNotNull();
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verifyNoInteractions(userDetailsService);
    }

    @Test
    @DisplayName("a valid token for a user that no longer exists leaves the request unauthenticated")
    void unknownUserIsIgnored() throws Exception {
        String token = jwtService.generateToken(userDetails);
        when(userDetailsService.loadUserByUsername("jan@bank.se"))
                .thenThrow(new UsernameNotFoundException("User not found"));

        MockFilterChain chain = runFilter("Bearer " + token);

        assertThat(chain.getRequest()).isNotNull();
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    @DisplayName("a token that does not match the loaded user leaves the request unauthenticated")
    void tokenForDifferentUserIsRejected() throws Exception {
        String token = jwtService.generateToken(userDetails);
        when(userDetailsService.loadUserByUsername("jan@bank.se")).thenReturn(userDetails("other@bank.se"));

        runFilter("Bearer " + token);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    @DisplayName("an already authenticated request is not re-authenticated")
    void existingAuthenticationIsNotOverwritten() throws Exception {
        String token = jwtService.generateToken(userDetails);
        SecurityContextHolder.getContext().setAuthentication(new TestingAuthenticationToken("someone", "pw"));

        runFilter("Bearer " + token);

        assertThat(SecurityContextHolder.getContext().getAuthentication().getName()).isEqualTo("someone");
        verifyNoInteractions(userDetailsService);
    }
}