package banking_system.loan.domain.model.state;

import banking_system.loan.domain.model.Loan;
import banking_system.loan.domain.model.LoanStatus;

public final class PendingApprovalState implements LoanState {

    public static final PendingApprovalState INSTANCE = new PendingApprovalState();

    private PendingApprovalState() {}

    @Override
    public LoanStatus status() {
        return LoanStatus.PENDING_APPROVAL;
    }

    @Override
    public LoanState approve(Loan loan) {
        return ActiveState.INSTANCE;
    }

    @Override
    public LoanState reject(Loan loan) {
        return RejectedState.INSTANCE;
    }
}