package banking_system.account.domain.model;

import java.util.Objects;

public record AccountNumber(String value) {
    public AccountNumber {
        Objects.requireNonNull(value, "AccountNumber value cannot be null");
        if (!value.matches("^SE\\d{22}$")) {
            throw new IllegalArgumentException("Invalid Swedish account number format. Expected format: SE + 22 digits");
        }
    }
}
