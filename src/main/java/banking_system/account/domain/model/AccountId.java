package banking_system.account.domain.model;

import java.util.Objects;
import java.util.UUID;

public record AccountId(String value) {
    public AccountId {
        Objects.requireNonNull(value, "AccountId value cannot be null");
    }

    public static AccountId generate() {
        return new AccountId(UUID.randomUUID().toString());
    }
}
