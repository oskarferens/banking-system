package banking_system.security.service;

import banking_system.security.domain.model.User;
import banking_system.security.domain.port.UserRepositoryPort;
import banking_system.security.infrastructure.adapter.in.web.dto.AuthResponse;
import banking_system.security.infrastructure.adapter.in.web.dto.LoginRequest;
import banking_system.security.infrastructure.adapter.in.web.dto.RegisterRequest;
import banking_system.security.infrastructure.adapter.out.jwt.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepositoryPort userRepository; // Zmieniono na port domenowy
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final UserDetailsService userDetailsService;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.findByEmail(request.email()).isPresent()) {
            throw new IllegalArgumentException("User with the same email already exists!");
        }

        // Używamy czystego obiektu domenowego User zamiast UserEntity
        User user = User.builder()
                .id(UUID.randomUUID().toString())
                .email(request.email())
                .password(passwordEncoder.encode(request.password()))
                .role("USER")
                .build();

        userRepository.save(user);

        var userDetails = userDetailsService.loadUserByUsername(user.getEmail());
        var jwtToken = jwtService.generateToken(userDetails);

        return new AuthResponse(jwtToken, user.getId(), user.getEmail());
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.email(),
                        request.password()
                )
        );

        // Pobieramy czysty obiekt domenowy
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new IllegalArgumentException("Email or password incorrect!"));

        var userDetails = userDetailsService.loadUserByUsername(user.getEmail());
        var jwtToken = jwtService.generateToken(userDetails);

        return new AuthResponse(jwtToken, user.getId(), user.getEmail());
    }
}