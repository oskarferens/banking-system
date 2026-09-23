package banking_system.loan.domain.model;

import banking_system.loan.domain.model.state.*;

public enum LoanStatus {
    PENDING_APPROVAL {
        @Override
        public LoanState toState() { return PendingApprovalState.INSTANCE; }
    },
    REJECTED {
        @Override
        public LoanState toState() { return RejectedState.INSTANCE; }
    },
    ACTIVE {
        @Override
        public LoanState toState() { return ActiveState.INSTANCE; }
    },
    OVERDUE {
        @Override
        public LoanState toState() { return OverdueState.INSTANCE; }
    },
    PAID_OFF {
        @Override
        public LoanState toState() { return PaidOffState.INSTANCE; }
    },
    DEFAULTED {
        @Override
        public LoanState toState() { return DefaultedState.INSTANCE; }
    };

    public abstract LoanState toState();
}