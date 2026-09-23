package banking_system.loan.domain.model.state;

import banking_system.loan.domain.model.Loan;
import banking_system.loan.domain.model.LoanStatus;
import banking_system.shared.domain.model.Money;

public final class ActiveState implements LoanState {

    public static final ActiveState INSTANCE = new ActiveState();

    private ActiveState() {}

    @Override
    public LoanStatus status() {
        return LoanStatus.ACTIVE;
    }

    @Override
    public LoanState recordPayment(Loan loan, Money amount) {
        loan.applyPayment(amount);
        return loan.isFullySettled() ? PaidOffState.INSTANCE : ActiveState.INSTANCE;
    }

    @Override
    public LoanState markOverdue(Loan loan) {
        return OverdueState.INSTANCE;
    }
}