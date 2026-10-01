package banking_system.loan.application.service;

import banking_system.account.domain.model.Account;
import banking_system.account.domain.model.AccountFactory;
import banking_system.account.domain.port.AccountRepositoryPort;
import banking_system.loan.domain.model.Loan;
import banking_system.loan.domain.model.LoanFactory;
import banking_system.loan.domain.port.LoanRepositoryPort;
import banking_system.notification.application.service.NotificationService;
import banking_system.shared.domain.model.Money;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LoanEndOfDayServiceTest {

    @Mock
    private LoanRepositoryPort loanRepository;

    @Mock
    private AccountRepositoryPort accountRepository;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private LoanEndOfDayService loanEndOfDayService;

    private Account borrowerAccount;
    private Instant originationDate;

    @BeforeEach
    void setUp() {
        borrowerAccount = AccountFactory.createStandardAccount("owner-1");
        originationDate = Instant.parse("2026-01-01T00:00:00Z");
    }

    @Test
    @DisplayName("processDay() sends a notification when a loan's status changes")
    void sendsNotificationOnStatusChange() {
        Loan loan = LoanFactory.originate(borrowerAccount.getId(), Money.sek("300.00"), 3, originationDate);
        loan.approve();
        when(loanRepository.findAll()).thenReturn(List.of(loan));
        when(accountRepository.findById(borrowerAccount.getId())).thenReturn(Optional.of(borrowerAccount));

        loanEndOfDayService.processDay(originationDate.plus(31, ChronoUnit.DAYS));

        verify(notificationService).sendNotification(eq("owner-1"), anyString(), anyString());
        verify(loanRepository).save(loan);
    }

    @Test
    @DisplayName("processDay() sends no notification when a loan's status does not change")
    void sendsNoNotificationWhenStatusUnchanged() {
        Loan loan = LoanFactory.originate(borrowerAccount.getId(), Money.sek("300.00"), 3, originationDate);
        loan.approve();
        when(loanRepository.findAll()).thenReturn(List.of(loan));

        loanEndOfDayService.processDay(originationDate.plus(5, ChronoUnit.DAYS));

        verify(notificationService, never()).sendNotification(any(), any(), any());
        verify(loanRepository).save(loan);
    }

    @Test
    @DisplayName("processDay() processes every loan returned by the repository")
    void processesEveryLoan() {
        Loan loanOne = LoanFactory.originate(borrowerAccount.getId(), Money.sek("300.00"), 3, originationDate);
        Loan loanTwo = LoanFactory.originate(borrowerAccount.getId(), Money.sek("300.00"), 3, originationDate);
        when(loanRepository.findAll()).thenReturn(List.of(loanOne, loanTwo));

        loanEndOfDayService.processDay(originationDate.plus(1, ChronoUnit.DAYS));

        verify(loanRepository, times(2)).save(any(Loan.class));
    }
}