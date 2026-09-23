package banking_system.loan.domain.model;

import banking_system.shared.domain.model.Money;

import java.time.Instant;
import java.util.Objects;

public class Installment {

    private final int number;
    private final Money amount;
    private final Instant dueDate;
    private InstallmentStatus status;

    public Installment(int number, Money amount, Instant dueDate, InstallmentStatus status) {
        this.number = number;
        this.amount = Objects.requireNonNull(amount, "Installment amount cannot be null");
        this.dueDate = Objects.requireNonNull(dueDate, "Due date cannot be null");
        this.status = Objects.requireNonNull(status, "Status cannot be null");
    }

    void markPaid() {
        this.status = InstallmentStatus.PAID;
    }

    void markOverdue() {
        if (this.status == InstallmentStatus.PENDING) {
            this.status = InstallmentStatus.OVERDUE;
        }
    }

    public int getNumber() { return number; }
    public Money getAmount() { return amount; }
    public Instant getDueDate() { return dueDate; }
    public InstallmentStatus getStatus() { return status; }
    public boolean isSettled() { return status == InstallmentStatus.PAID; }
}