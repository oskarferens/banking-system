package banking_system.loan.application.service;

import banking_system.account.domain.exception.AccountNotFoundException;
import banking_system.account.domain.model.Account;
import banking_system.account.domain.model.AccountId;
import banking_system.account.domain.port.AccountRepositoryPort;
import banking_system.loan.domain.exception.LoanApplicationRejectedException;
import banking_system.loan.domain.exception.LoanNotFoundException;
import banking_system.loan.domain.model.Loan;
import banking_system.loan.domain.model.LoanFactory;
import banking_system.loan.domain.model.LoanId;
import banking_system.loan.domain.port.CreditScoringPolicy;
import banking_system.loan.domain.port.LoanRepositoryPort;
import banking_system.loan.domain.port.LoanUseCase;
import banking_system.shared.domain.model.Money;
import banking_system.timemachine.domain.port.TimeMachineUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class LoanService implements LoanUseCase {

    private final LoanRepositoryPort loanRepository;
    private final AccountRepositoryPort accountRepository;
    private final CreditScoringPolicy creditScoringPolicy;
    private final TimeMachineUseCase timeMachineUseCase;

    @Override
    @Transactional
    public Loan applyForLoan(String borrowerAccountId, Money principal, int termInMonths) {
        Account borrowerAccount = findAccount(borrowerAccountId);

        if (!creditScoringPolicy.isEligible(borrowerAccount, principal)) {
            throw new LoanApplicationRejectedException(
                    "Loan application for account " + borrowerAccountId + " did not pass credit scoring.");
        }

        Loan loan = LoanFactory.originate(
                borrowerAccount.getId(), principal, termInMonths, timeMachineUseCase.getCurrentTime());

        return loanRepository.save(loan);
    }

    @Override
    @Transactional
    public Loan approveLoan(String loanId) {
        Loan loan = findLoan(loanId);
        Account borrowerAccount = findAccount(loan.getBorrowerAccountId().value());

        loan.approve();
        borrowerAccount.deposit(loan.getPrincipal());

        accountRepository.save(borrowerAccount);
        return loanRepository.save(loan);
    }

    @Override
    @Transactional
    public Loan rejectLoan(String loanId) {
        Loan loan = findLoan(loanId);

        loan.reject();

        return loanRepository.save(loan);
    }

    @Override
    @Transactional
    public Loan recordPayment(String loanId, Money amount) {
        Loan loan = findLoan(loanId);
        Account borrowerAccount = findAccount(loan.getBorrowerAccountId().value());

        borrowerAccount.withdraw(amount);
        loan.recordPayment(amount);

        accountRepository.save(borrowerAccount);
        return loanRepository.save(loan);
    }

    @Override
    @Transactional(readOnly = true)
    public Loan getLoan(String loanId) {
        return findLoan(loanId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Loan> getLoansForAccount(String accountId) {
        return loanRepository.findAllByBorrowerAccountId(accountId);
    }

    private Loan findLoan(String loanId) {
        return loanRepository.findById(new LoanId(loanId))
                .orElseThrow(() -> new LoanNotFoundException(loanId));
    }

    private Account findAccount(String accountId) {
        return accountRepository.findById(new AccountId(accountId))
                .orElseThrow(() -> new AccountNotFoundException(accountId));
    }
}