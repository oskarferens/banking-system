package banking_system.loan.application.service;

import banking_system.account.domain.model.Account;
import banking_system.account.domain.port.AccountRepositoryPort;
import banking_system.loan.domain.model.Loan;
import banking_system.loan.domain.model.LoanStatus;
import banking_system.loan.domain.port.LoanEndOfDayUseCase;
import banking_system.loan.domain.port.LoanRepositoryPort;
import banking_system.notification.application.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class LoanEndOfDayService implements LoanEndOfDayUseCase {

    private final LoanRepositoryPort loanRepository;
    private final AccountRepositoryPort accountRepository;
    private final NotificationService notificationService;

    @Override
    @Transactional
    public void processDay(Instant currentVirtualTime) {
        List<Loan> allLoans = loanRepository.findAll();

        for (Loan loan : allLoans) {
            LoanStatus statusBefore = loan.getStatus();

            loan.applyEndOfDayProcessing(currentVirtualTime);
            loanRepository.save(loan);

            if (loan.getStatus() != statusBefore) {
                notifyStatusChange(loan, statusBefore);
            }
        }
    }

    private void notifyStatusChange(Loan loan, LoanStatus previousStatus) {
        Optional<Account> borrowerAccount = accountRepository.findById(loan.getBorrowerAccountId());
        if (borrowerAccount.isEmpty()) {
            return;
        }

        String title = "Loan status update";
        String message = "Your loan " + loan.getId().value() + " moved from " + previousStatus + " to " + loan.getStatus() + ".";

        notificationService.sendNotification(borrowerAccount.get().getOwnerId(), title, message);
    }
}