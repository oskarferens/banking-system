package banking_system.loan.domain.port;

import banking_system.loan.domain.model.Loan;
import banking_system.shared.domain.model.Money;

import java.util.List;

public interface LoanUseCase {
    Loan applyForLoan(String borrowerAccountId, Money principal, int termInMonths);
    Loan approveLoan(String loanId);
    Loan rejectLoan(String loanId);
    Loan recordPayment(String loanId, Money amount);
    Loan getLoan(String loanId);
    List<Loan> getLoansForAccount(String accountId);
}