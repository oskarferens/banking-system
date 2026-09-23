package banking_system.loan.domain.model.state;

import banking_system.loan.domain.model.LoanStatus;

public final class DefaultedState implements LoanState {

    public static final DefaultedState INSTANCE = new DefaultedState();

    private DefaultedState() {}

    @Override
    public LoanStatus status() {
        return LoanStatus.DEFAULTED;
    }
}