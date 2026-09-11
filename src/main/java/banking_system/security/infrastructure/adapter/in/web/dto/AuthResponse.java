package banking_system.security.infrastructure.adapter.in.web.dto;

public record AuthResponse(
        String token,
        String userId,
        String email
) {}