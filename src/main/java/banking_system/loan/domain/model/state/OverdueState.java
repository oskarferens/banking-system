package banking_system.loan.domain.model.state;

import banking_system.loan.domain.model.Loan;
import banking_system.loan.domain.model.LoanStatus;
import banking_system.shared.domain.model.Money;

import java.math.BigDecimal;
import java.time.Instant;

public final class OverdueState implements LoanState {

    public static final OverdueState INSTANCE = new OverdueState();

    private static final BigDecimal DAILY_PENALTY_RATE = new BigDecimal("0.005"); //0,5% a day on each overdue installment
    private static final int DEFAULT_THRESHOLD_DAYS = 15;

    private OverdueState() {}

    @Override
    public LoanStatus status() {
        return LoanStatus.OVERDUE;
    }

    @Override
    public LoanState recordPayment(Loan loan, Money amount) {
        loan.applyPayment(amount);
        return loan.isFullySettled() ? PaidOffState.INSTANCE : ActiveState.INSTANCE;
    }

    @Override
    public LoanState markDefaulted(Loan loan) {
        return DefaultedState.INSTANCE;
    }

    @Override
    public LoanState processEndOfDay(Loan loan, Instant asOf) {
        loan.markInstallmentsOverdueAsOf(asOf);
        loan.applyOverduePenalty(DAILY_PENALTY_RATE);

        return loan.hasInstallmentOverdueBeyond(DEFAULT_THRESHOLD_DAYS, asOf)
                ? DefaultedState.INSTANCE
                : OverdueState.INSTANCE;
    }
}