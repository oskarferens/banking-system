package banking_system.loan.domain.model.state;

import banking_system.loan.domain.model.LoanStatus;

public final class PaidOffState implements LoanState {

    public static final PaidOffState INSTANCE = new PaidOffState();

    private PaidOffState() {}

    @Override
    public LoanStatus status() {
        return LoanStatus.PAID_OFF;
    }
}