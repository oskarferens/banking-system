package banking_system.loan.domain.model;

import java.util.Objects;
import java.util.UUID;

public record LoanId(String value) {
    public LoanId {
        Objects.requireNonNull(value, "LoanId value cannot be null");
    }

    public static LoanId generate() {
        return new LoanId(UUID.randomUUID().toString());
    }
}