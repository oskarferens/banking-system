package banking_system.security.service;

import banking_system.security.domain.model.User;
import banking_system.security.domain.port.UserRepositoryPort;
import banking_system.security.infrastructure.adapter.in.web.dto.AuthResponse;
import banking_system.security.infrastructure.adapter.in.web.dto.LoginRequest;
import banking_system.security.infrastructure.adapter.in.web.dto.RegisterRequest;
import banking_system.security.infrastructure.adapter.out.jwt.JwtService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    private static final String EMAIL = "jan@bank.se";

    @Mock
    private UserRepositoryPort userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private UserDetailsService userDetailsService;

    @InjectMocks
    private AuthService authService;

    private static UserDetails userDetails() {
        return org.springframework.security.core.userdetails.User
                .withUsername(EMAIL).password("ENCODED").authorities("ROLE_USER").build();
    }

    private static User storedUser() {
        return User.builder().id("user-1").email(EMAIL).password("ENCODED").role("USER").build();
    }

    @Test
    @DisplayName("register() stores a USER with an encoded password and returns a token")
    void registerCreatesUserAndReturnsToken() {
        UserDetails userDetails = userDetails();
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());
        when(passwordEncoder.encode("secret123")).thenReturn("ENCODED");
        when(userDetailsService.loadUserByUsername(EMAIL)).thenReturn(userDetails);
        when(jwtService.generateToken(userDetails)).thenReturn("jwt-token");

        AuthResponse response = authService.register(new RegisterRequest(EMAIL, "secret123"));

        ArgumentCaptor<User> savedUser = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(savedUser.capture());
        assertThat(savedUser.getValue().getEmail()).isEqualTo(EMAIL);
        assertThat(savedUser.getValue().getPassword()).isEqualTo("ENCODED");
        assertThat(savedUser.getValue().getRole()).isEqualTo("USER");
        assertThat(response.token()).isEqualTo("jwt-token");
        assertThat(response.email()).isEqualTo(EMAIL);
        assertThat(response.userId()).isEqualTo(savedUser.getValue().getId());
    }

    @Test
    @DisplayName("register() rejects an email that is already taken")
    void registerRejectsDuplicateEmail() {
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(storedUser()));

        assertThatThrownBy(() -> authService.register(new RegisterRequest(EMAIL, "secret123")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("already exists");

        verify(userRepository, never()).save(any(User.class));
        verifyNoInteractions(passwordEncoder, jwtService);
    }

    @Test
    @DisplayName("login() authenticates the credentials and returns a token")
    void loginReturnsToken() {
        UserDetails userDetails = userDetails();
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(storedUser()));
        when(userDetailsService.loadUserByUsername(EMAIL)).thenReturn(userDetails);
        when(jwtService.generateToken(userDetails)).thenReturn("jwt-token");

        AuthResponse response = authService.login(new LoginRequest(EMAIL, "secret123"));

        ArgumentCaptor<Authentication> authentication = ArgumentCaptor.forClass(Authentication.class);
        verify(authenticationManager).authenticate(authentication.capture());
        assertThat(authentication.getValue().getPrincipal()).isEqualTo(EMAIL);
        assertThat(authentication.getValue().getCredentials()).isEqualTo("secret123");
        assertThat(response.token()).isEqualTo("jwt-token");
        assertThat(response.userId()).isEqualTo("user-1");
        assertThat(response.email()).isEqualTo(EMAIL);
    }

    @Test
    @DisplayName("login() with bad credentials propagates BadCredentialsException and issues no token")
    void loginWithBadCredentialsFails() {
        when(authenticationManager.authenticate(any(Authentication.class)))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        assertThatThrownBy(() -> authService.login(new LoginRequest(EMAIL, "wrong")))
                .isInstanceOf(BadCredentialsException.class);

        verifyNoInteractions(userRepository, userDetailsService, jwtService);
    }

    @Test
    @DisplayName("login() fails when the authenticated user is missing from the repository")
    void loginFailsWhenUserMissingFromRepository() {
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(new LoginRequest(EMAIL, "secret123")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("incorrect");

        verifyNoInteractions(jwtService);
    }
}