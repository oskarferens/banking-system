package banking_system.loan.domain.model.state;

import banking_system.loan.domain.model.Loan;
import banking_system.loan.domain.model.LoanStatus;
import banking_system.shared.domain.model.Money;

public final class OverdueState implements LoanState {

    public static final OverdueState INSTANCE = new OverdueState();

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
}