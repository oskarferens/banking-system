package banking_system.transfer.domain.model;

import java.util.Objects;
import java.util.UUID;

public record TransferId(String value) {
    public TransferId {
        Objects.requireNonNull(value, "TransferId value cannot be null");
    }

    public static TransferId generate() {
        return new TransferId(UUID.randomUUID().toString());
    }
}
