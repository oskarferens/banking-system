package banking_system.transfer.application.service;

import banking_system.account.domain.model.Account;
import banking_system.account.domain.model.AccountNumber;
import banking_system.account.domain.port.AccountRepositoryPort;
import banking_system.shared.domain.model.Money;
import banking_system.timemachine.domain.port.TimeMachineUseCase;
import banking_system.transfer.domain.model.Transfer;
import banking_system.transfer.domain.model.TransferId;
import banking_system.transfer.domain.model.TransferStatus;
import banking_system.transfer.domain.port.TransferRepositoryPort;
import banking_system.transfer.domain.port.TransferUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Currency;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TransferService implements TransferUseCase {

    private final AccountRepositoryPort accountRepository;
    private final TransferRepositoryPort transferRepository;
    private final TimeMachineUseCase timeMachineUseCase;

    @Override
    @Transactional
    public Transfer executeTransfer(String sourceAccountNumber, String targetAccountNumber,
                                    BigDecimal amount, String currencyCode, String title) {

        Currency currency = Currency.getInstance(currencyCode);
        Money transferMoney = new Money(amount, currency);

        // Retrieve account aggregates
        Account sourceAccount = accountRepository.findByAccountNumber(new AccountNumber(sourceAccountNumber))
                .orElseThrow(() -> new IllegalArgumentException("Source account not found: " + sourceAccountNumber));

        Account targetAccount = accountRepository.findByAccountNumber(new AccountNumber(targetAccountNumber))
                .orElseThrow(() -> new IllegalArgumentException("Target account not found: " + targetAccountNumber));

        // Perform domain operations on the aggregates
        sourceAccount.withdraw(transferMoney);
        targetAccount.deposit(transferMoney);

        // Save the updated account state
        accountRepository.save(sourceAccount);
        accountRepository.save(targetAccount);

        // Create the transfer aggregate and assign the virtual time
        Transfer transfer = new Transfer(
                new TransferId(UUID.randomUUID().toString()),
                sourceAccount.getId(),
                targetAccount.getId(),
                transferMoney,
                TransferStatus.COMPLETED,
                timeMachineUseCase.getCurrentTime(),
                title
        );

        // Save the transfer to the transaction history
        return transferRepository.save(transfer);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Transfer> getAccountHistory(String accountNumber) {
        Account account = accountRepository.findByAccountNumber(new AccountNumber(accountNumber))
                .orElseThrow(() -> new IllegalArgumentException("Account not found: " + accountNumber));

        return transferRepository.findAllByAccountId(account.getId().value());
    }
}