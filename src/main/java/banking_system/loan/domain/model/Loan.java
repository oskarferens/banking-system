package banking_system.loan.domain.model;

import banking_system.account.domain.model.AccountId;
import banking_system.loan.domain.exception.InvalidLoanStateTransitionException;
import banking_system.loan.domain.model.state.LoanState;
import banking_system.shared.domain.model.Money;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

public class Loan {

    private final LoanId id;
    private final AccountId borrowerAccountId;
    private final Money principal;
    private final BigDecimal annualInterestRate;
    private final int termInMonths;
    private final Instant createdAt;
    private final List<Installment> installments;
    private LoanState state;

    public Loan(LoanId id, AccountId borrowerAccountId, Money principal, BigDecimal annualInterestRate,
                int termInMonths, Instant createdAt, List<Installment> installments, LoanState state) {
        this.id = Objects.requireNonNull(id, "LoanId cannot be null");
        this.borrowerAccountId = Objects.requireNonNull(borrowerAccountId, "Borrower AccountId cannot be null");
        this.principal = Objects.requireNonNull(principal, "Principal cannot be null");
        this.annualInterestRate = Objects.requireNonNull(annualInterestRate, "Interest rate cannot be null");
        this.createdAt = Objects.requireNonNull(createdAt, "CreatedAt cannot be null");
        this.installments = Objects.requireNonNull(installments, "Installments cannot be null");
        this.state = Objects.requireNonNull(state, "LoanState cannot be null");
        this.termInMonths = termInMonths;

        if (termInMonths <= 0) {
            throw new IllegalArgumentException("Loan term must be at least 1 month");
        }
        if (principal.isNegative() || principal.amount().signum() == 0) {
            throw new IllegalArgumentException("Loan principal must be positive");
        }
        if (annualInterestRate.signum() < 0) {
            throw new IllegalArgumentException("Interest rate cannot be negative");
        }
    }

    public void approve() {
        this.state = state.approve(this);
    }

    public void reject() {
        this.state = state.reject(this);
    }

    public void recordPayment(Money amount) {
        this.state = state.recordPayment(this, amount);
    }

    public void markOverdue() {
        this.state = state.markOverdue(this);
    }

    public void markDefaulted() {
        this.state = state.markDefaulted(this);
    }

    // Public only because LoanState implementations are in a separate subpackage.
    // recordPayment() remains the intended API and controls payment eligibility.
    public void applyPayment(Money amount) {
        Installment nextUnpaid = installments.stream()
                .filter(installment -> !installment.isSettled())
                .findFirst()
                .orElseThrow(() -> new InvalidLoanStateTransitionException("Loan has no outstanding installments to pay"));

        if (amount.amount().compareTo(nextUnpaid.getAmount().amount()) != 0) {
            throw new IllegalArgumentException(
                    "Payment amount " + amount.amount() + " does not match the due installment amount " + nextUnpaid.getAmount().amount());
        }

        nextUnpaid.markPaid();
    }

    // Same caveat as applyPayment()- public only because LoanState needs it, not an intended entry point.
    public boolean isFullySettled() {
        return installments.stream().allMatch(Installment::isSettled);
    }

    public LoanId getId() { return id; }
    public AccountId getBorrowerAccountId() { return borrowerAccountId; }
    public Money getPrincipal() { return principal; }
    public BigDecimal getAnnualInterestRate() { return annualInterestRate; }
    public int getTermInMonths() { return termInMonths; }
    public Instant getCreatedAt() { return createdAt; }
    public List<Installment> getInstallments() { return installments; }
    public LoanStatus getStatus() { return state.status(); }
}