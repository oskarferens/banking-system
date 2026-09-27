package banking_system.loan.domain.model.state;

import banking_system.loan.domain.exception.InvalidLoanStateTransitionException;
import banking_system.loan.domain.model.Loan;
import banking_system.loan.domain.model.LoanStatus;
import banking_system.shared.domain.model.Money;

import java.time.Instant;

public interface LoanState {

    LoanStatus status();

    default LoanState approve(Loan loan) {
        throw illegalTransition("approve");
    }

    default LoanState reject(Loan loan) {
        throw illegalTransition("reject");
    }

    default LoanState recordPayment(Loan loan, Money amount) {
        throw illegalTransition("record a payment on");
    }

    default LoanState markOverdue(Loan loan) {
        throw illegalTransition("mark overdue");
    }

    default LoanState markDefaulted(Loan loan) {
        throw illegalTransition("mark defaulted");
    }

    default LoanState processEndOfDay(Loan loan, Instant asOf) {
        return this;
    }

    private InvalidLoanStateTransitionException illegalTransition(String action) {
        return new InvalidLoanStateTransitionException("Cannot " + action + " a loan in state " + status());
    }
}