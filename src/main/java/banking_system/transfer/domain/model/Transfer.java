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
    private final Money fee;
    private final TransferStatus status;
    private final Instant timestamp;
    private final String title;

    public Transfer(TransferId id, AccountId sourceAccountId, AccountId targetAccountId, Money amount, Money fee, TransferStatus status, Instant timestamp, String title) {
        this.id = Objects.requireNonNull(id, "TransferId cannot be null");
        this.sourceAccountId = Objects.requireNonNull(sourceAccountId, "Source AccountId cannot be null");
        this.targetAccountId = Objects.requireNonNull(targetAccountId, "Target AccountId cannot be null");
        this.amount = Objects.requireNonNull(amount, "Amount cannot be null");
        this.fee = Objects.requireNonNull(fee, "Fee cannot be null");
        this.status = Objects.requireNonNull(status, "Status cannot be null");
        this.timestamp = Objects.requireNonNull(timestamp, "Timestamp cannot be null");
        this.title = Objects.requireNonNull(title, "Title cannot be null");

        if (sourceAccountId.equals(targetAccountId)) {
            throw new SameAccountTransferException();
        }
    }

    public TransferId getId() { return id; }
    public AccountId getSourceAccountId() { return sourceAccountId; }
    public AccountId getTargetAccountId() { return targetAccountId; }
    public Money getAmount() { return amount; }
    public Money getFee() { return fee; }
    public TransferStatus getStatus() { return status; }
    public Instant getTimestamp() { return timestamp; }
    public String getTitle() { return title; }
}