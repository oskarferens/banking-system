package banking_system.loan.domain.model.state;

import banking_system.loan.domain.model.LoanStatus;

public final class RejectedState implements LoanState {

    public static final RejectedState INSTANCE = new RejectedState();

    private RejectedState() {}

    @Override
    public LoanStatus status() {
        return LoanStatus.REJECTED;
    }
}