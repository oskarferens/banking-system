package banking_system.transfer.application.service;

import banking_system.account.domain.model.Account;
import banking_system.account.domain.model.AccountNumber;
import banking_system.account.domain.port.AccountRepositoryPort;
import banking_system.shared.domain.model.Money;
import banking_system.timemachine.domain.port.TimeMachineUseCase;
import banking_system.transfer.domain.model.Transfer;
import banking_system.transfer.domain.model.TransferId;
import banking_system.transfer.domain.model.TransferStatus;
import banking_system.transfer.domain.model.TransferType;
import banking_system.transfer.domain.port.ExchangeRateProviderPort;
import banking_system.transfer.domain.port.TransactionFeeStrategy;
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
    private final ExchangeRateProviderPort exchangeRateProviderPort;
    private final TransactionFeeResolver transactionFeeResolver;

    @Override
    @Transactional
    public Transfer executeTransfer(String sourceAccountNumber, String targetAccountNumber,
                                    BigDecimal amount, String currencyCode, String title) {

        Currency requestedCurrency = Currency.getInstance(currencyCode);

        Account sourceAccount = accountRepository.findByAccountNumber(new AccountNumber(sourceAccountNumber))
                .orElseThrow(() -> new IllegalArgumentException("Source account not found: " + sourceAccountNumber));

        Account targetAccount = accountRepository.findByAccountNumber(new AccountNumber(targetAccountNumber))
                .orElseThrow(() -> new IllegalArgumentException("Target account not found: " + targetAccountNumber));

        Currency accountCurrency = sourceAccount.getBalance().currency();
        TransferType transferType = requestedCurrency.equals(accountCurrency)
                ? TransferType.DOMESTIC
                : TransferType.FOREIGN_EXCHANGE;

        Money convertedAmount;
        if (transferType == TransferType.FOREIGN_EXCHANGE) {
            BigDecimal exchangeRate = exchangeRateProviderPort.getExchangeRate(requestedCurrency, accountCurrency);
            convertedAmount = new Money(amount.multiply(exchangeRate), accountCurrency);
        } else {
            convertedAmount = new Money(amount, accountCurrency);
        }

        TransactionFeeStrategy feeStrategy = transactionFeeResolver.resolve(transferType);
        Money fee = feeStrategy.calculateFee(convertedAmount);
        Money totalDebit = convertedAmount.add(fee);


        sourceAccount.withdraw(totalDebit);
        targetAccount.deposit(convertedAmount);

        //Save the updated account state (This time works)
        accountRepository.save(sourceAccount);
        accountRepository.save(targetAccount);

        //Create the transfer aggregate and assign the virtual time (virtual time fixed from Feature08)
        Transfer transfer = new Transfer(
                new TransferId(UUID.randomUUID().toString()),
                sourceAccount.getId(),
                targetAccount.getId(),
                convertedAmount,
                fee,
                TransferStatus.COMPLETED,
                timeMachineUseCase.getCurrentTime(),
                title
        );

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