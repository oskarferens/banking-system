package banking_system.transfer.domain.model;

import banking_system.account.domain.model.AccountId;
import banking_system.shared.domain.model.Money;
import banking_system.transfer.domain.exception.SameAccountTransferException;

import java.time.Instant;
import java.util.Objects;

public class Transfer {

    private final TransferId id;
    private final AccountId sourceAccountId;
    private final AccountId targetAccountId;
    private final Money amount;
    private final TransferStatus status;
    private final Instant timestamp;

    public Transfer(TransferId id, AccountId sourceAccountId, AccountId targetAccountId, Money amount, TransferStatus status, Instant timestamp) {
        this.id = Objects.requireNonNull(id, "TransferId cannot be null");
        this.sourceAccountId = Objects.requireNonNull(sourceAccountId, "Source AccountId cannot be null");
        this.targetAccountId = Objects.requireNonNull(targetAccountId, "Target AccountId cannot be null");
        this.amount = Objects.requireNonNull(amount, "Amount cannot be null");
        this.status = Objects.requireNonNull(status, "Status cannot be null");
        this.timestamp = Objects.requireNonNull(timestamp, "Timestamp cannot be null");

        if (sourceAccountId.equals(targetAccountId)) {
            throw new SameAccountTransferException();
        }
    }

    public TransferId getId() {
        return id;
    }

    public AccountId getSourceAccountId() {
        return sourceAccountId;
    }

    public AccountId getTargetAccountId() {
        return targetAccountId;
    }

    public Money getAmount() {
        return amount;
    }

    public TransferStatus getStatus() {
        return status;
    }

    public Instant getTimestamp() {
        return timestamp;
    }
}