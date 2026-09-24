package banking_system.loan.application.service;

import banking_system.account.domain.exception.AccountNotFoundException;
import banking_system.account.domain.exception.InsufficientFundsException;
import banking_system.account.domain.model.Account;
import banking_system.account.domain.model.AccountFactory;
import banking_system.account.domain.model.AccountId;
import banking_system.account.domain.port.AccountRepositoryPort;
import banking_system.loan.domain.exception.LoanApplicationRejectedException;
import banking_system.loan.domain.exception.LoanNotFoundException;
import banking_system.loan.domain.model.InstallmentStatus;
import banking_system.loan.domain.model.Loan;
import banking_system.loan.domain.model.LoanFactory;
import banking_system.loan.domain.model.LoanId;
import banking_system.loan.domain.model.LoanStatus;
import banking_system.loan.domain.port.CreditScoringPolicy;
import banking_system.loan.domain.port.LoanRepositoryPort;
import banking_system.shared.domain.model.Money;
import banking_system.timemachine.domain.port.TimeMachineUseCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LoanServiceTest {

    @Mock
    private LoanRepositoryPort loanRepository;

    @Mock
    private AccountRepositoryPort accountRepository;

    @Mock
    private CreditScoringPolicy creditScoringPolicy;

    @Mock
    private TimeMachineUseCase timeMachineUseCase;

    @InjectMocks
    private LoanService loanService;

    private Account borrowerAccount;

    @BeforeEach
    void setUp() {
        borrowerAccount = AccountFactory.createStandardAccount("owner-1");
        borrowerAccount.deposit(Money.sek("1000.00"));
    }

    @Test
    @DisplayName("applyForLoan() saves a PENDING_APPROVAL loan when scoring passes")
    void applyForLoanSavesLoanWhenScoringPasses() {
        when(accountRepository.findById(borrowerAccount.getId())).thenReturn(Optional.of(borrowerAccount));
        when(creditScoringPolicy.isEligible(borrowerAccount, Money.sek("500.00"))).thenReturn(true);
        when(timeMachineUseCase.getCurrentTime()).thenReturn(Instant.parse("2026-01-01T00:00:00Z"));
        when(loanRepository.save(any(Loan.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Loan result = loanService.applyForLoan(borrowerAccount.getId().value(), Money.sek("500.00"), 12);

        assertThat(result.getStatus()).isEqualTo(LoanStatus.PENDING_APPROVAL);
        assertThat(result.getPrincipal().amount()).isEqualByComparingTo(Money.sek("500.00").amount());
        verify(loanRepository).save(any(Loan.class));
    }

    @Test
    @DisplayName("applyForLoan() rejects the application without saving anything when scoring fails")
    void applyForLoanRejectsWhenScoringFails() {
        when(accountRepository.findById(borrowerAccount.getId())).thenReturn(Optional.of(borrowerAccount));
        when(creditScoringPolicy.isEligible(borrowerAccount, Money.sek("5000.00"))).thenReturn(false);

        assertThatThrownBy(() -> loanService.applyForLoan(borrowerAccount.getId().value(), Money.sek("5000.00"), 12))
                .isInstanceOf(LoanApplicationRejectedException.class);

        verify(loanRepository, never()).save(any());
    }

    @Test
    @DisplayName("applyForLoan() throws when the borrower account does not exist")
    void applyForLoanThrowsWhenAccountNotFound() {
        AccountId missingId = AccountId.generate();
        when(accountRepository.findById(missingId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> loanService.applyForLoan(missingId.value(), Money.sek("500.00"), 12))
                .isInstanceOf(AccountNotFoundException.class);

        verify(creditScoringPolicy, never()).isEligible(any(), any());
    }

    @Test
    @DisplayName("approveLoan() activates the loan and credits the principal to the borrower's account")
    void approveLoanCreditsAccount() {
        Loan loan = LoanFactory.originate(borrowerAccount.getId(), Money.sek("500.00"), 12, Instant.parse("2026-01-01T00:00:00Z"));
        when(loanRepository.findById(loan.getId())).thenReturn(Optional.of(loan));
        when(accountRepository.findById(borrowerAccount.getId())).thenReturn(Optional.of(borrowerAccount));
        when(loanRepository.save(any(Loan.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Loan result = loanService.approveLoan(loan.getId().value());

        assertThat(result.getStatus()).isEqualTo(LoanStatus.ACTIVE);
        assertThat(borrowerAccount.getBalance().amount()).isEqualByComparingTo(Money.sek("1500.00").amount());
        verify(accountRepository).save(borrowerAccount);
    }

    @Test
    @DisplayName("approveLoan() throws when the loan does not exist")
    void approveLoanThrowsWhenLoanNotFound() {
        when(loanRepository.findById(any(LoanId.class))).thenReturn(Optional.empty());

        assertThatThrownBy(() -> loanService.approveLoan(LoanId.generate().value()))
                .isInstanceOf(LoanNotFoundException.class);
    }

    @Test
    @DisplayName("recordPayment() withdraws the installment amount from the borrower's account")
    void recordPaymentWithdrawsFromAccount() {
        Loan loan = LoanFactory.originate(borrowerAccount.getId(), Money.sek("300.00"), 3, Instant.parse("2026-01-01T00:00:00Z"));
        loan.approve();
        Money firstInstallment = loan.getInstallments().get(0).getAmount();
        Money balanceBeforePayment = borrowerAccount.getBalance();

        when(loanRepository.findById(loan.getId())).thenReturn(Optional.of(loan));
        when(accountRepository.findById(borrowerAccount.getId())).thenReturn(Optional.of(borrowerAccount));
        when(loanRepository.save(any(Loan.class))).thenAnswer(invocation -> invocation.getArgument(0));

        loanService.recordPayment(loan.getId().value(), firstInstallment);

        assertThat(borrowerAccount.getBalance().amount())
                .isEqualByComparingTo(balanceBeforePayment.subtract(firstInstallment).amount());
        assertThat(loan.getInstallments().get(0).isSettled()).isTrue();
    }

    @Test
    @DisplayName("recordPayment() leaves the loan untouched when the account has insufficient funds")
    void recordPaymentLeavesLoanUntouchedOnInsufficientFunds() {
        // Constructed directly bypassing applyForLoan/scoring to explicitly test
        // the order of operations in recordPayment()
        //- the account is charged BEFORE the loan is updated.
        Account poorAccount = AccountFactory.createStandardAccount("owner-2"); // balance 0.00, overdraft limit -500.00
        Loan loan = LoanFactory.originate(poorAccount.getId(), Money.sek("5000.00"), 3, Instant.parse("2026-01-01T00:00:00Z"));
        loan.approve();
        Money firstInstallment = loan.getInstallments().get(0).getAmount(); // Way above the limit

        when(loanRepository.findById(loan.getId())).thenReturn(Optional.of(loan));
        when(accountRepository.findById(poorAccount.getId())).thenReturn(Optional.of(poorAccount));

        assertThatThrownBy(() -> loanService.recordPayment(loan.getId().value(), firstInstallment))
                .isInstanceOf(InsufficientFundsException.class);

        assertThat(loan.getInstallments().get(0).getStatus()).isEqualTo(InstallmentStatus.PENDING);
        verify(loanRepository, never()).save(any());
    }

    @Test
    @DisplayName("getLoansForAccount() delegates directly to the repository")
    void getLoansForAccountDelegatesToRepository() {
        String accountId = borrowerAccount.getId().value();
        Loan loan = LoanFactory.originate(borrowerAccount.getId(), Money.sek("500.00"), 12, Instant.parse("2026-01-01T00:00:00Z"));
        when(loanRepository.findAllByBorrowerAccountId(accountId)).thenReturn(List.of(loan));

        List<Loan> result = loanService.getLoansForAccount(accountId);

        assertThat(result).containsExactly(loan);
    }
}