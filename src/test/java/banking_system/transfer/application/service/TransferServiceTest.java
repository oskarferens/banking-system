package banking_system.transfer.application.service;

import banking_system.account.domain.exception.InsufficientFundsException;
import banking_system.account.domain.model.Account;
import banking_system.account.domain.model.AccountFactory;
import banking_system.account.domain.model.AccountNumber;
import banking_system.account.domain.port.AccountRepositoryPort;
import banking_system.shared.domain.model.Money;
import banking_system.timemachine.domain.port.TimeMachineUseCase;
import banking_system.transfer.domain.exception.SameAccountTransferException;
import banking_system.transfer.domain.model.Transfer;
import banking_system.transfer.domain.model.TransferId;
import banking_system.transfer.domain.model.TransferStatus;
import banking_system.transfer.domain.port.ExchangeRateProviderPort;
import banking_system.transfer.domain.port.TransferRepositoryPort;
import banking_system.transfer.infrastructure.adapter.out.fee.DomesticTransferFeeStrategy;
import banking_system.transfer.infrastructure.adapter.out.fee.ForeignExchangeFeeStrategy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Currency;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TransferServiceTest {

    private static final Instant NOW = Instant.parse("2026-01-01T12:00:00Z");
    private static final Currency USD = Currency.getInstance("USD");

    @Mock
    private AccountRepositoryPort accountRepository;

    @Mock
    private TransferRepositoryPort transferRepository;

    @Mock
    private TimeMachineUseCase timeMachineUseCase;

    @Mock
    private ExchangeRateProviderPort exchangeRateProvider;

    private TransferService transferService;
    private Account source;
    private Account target;

    @BeforeEach
    void setUp() {
        TransactionFeeResolver feeResolver = new TransactionFeeResolver(
                List.of(new DomesticTransferFeeStrategy(), new ForeignExchangeFeeStrategy()));

        transferService = new TransferService(
                accountRepository, transferRepository, timeMachineUseCase, exchangeRateProvider, feeResolver);

        source = AccountFactory.createStandardAccount("owner-1");
        target = AccountFactory.createStandardAccount("owner-2");
    }

    private void stubBothAccounts() {
        when(accountRepository.findByAccountNumber(source.getAccountNumber())).thenReturn(Optional.of(source));
        when(accountRepository.findByAccountNumber(target.getAccountNumber())).thenReturn(Optional.of(target));
    }

    private String sourceNumber() {
        return source.getAccountNumber().value();
    }

    private String targetNumber() {
        return target.getAccountNumber().value();
    }

    @Test
    @DisplayName("a domestic SEK to SEK transfer moves the exact amount, charges no fee and never asks for an exchange rate")
    void domesticTransferMovesMoneyWithoutFee() {
        source.deposit(Money.sek("1000.00"));
        stubBothAccounts();
        when(timeMachineUseCase.getCurrentTime()).thenReturn(NOW);
        when(transferRepository.save(any(Transfer.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Transfer result = transferService.executeTransfer(
                sourceNumber(), targetNumber(), new BigDecimal("250.00"), "SEK", "Rent");

        assertThat(result.getAmount().amount()).isEqualByComparingTo("250.00");
        assertThat(result.getFee().amount()).isEqualByComparingTo("0.00");
        assertThat(result.getStatus()).isEqualTo(TransferStatus.COMPLETED);
        assertThat(result.getTimestamp()).isEqualTo(NOW);
        assertThat(source.getBalance().amount()).isEqualByComparingTo("750.00");
        assertThat(target.getBalance().amount()).isEqualByComparingTo("250.00");
        verifyNoInteractions(exchangeRateProvider);
        verify(accountRepository).save(source);
        verify(accountRepository).save(target);
    }

    @Test
    @DisplayName("a foreign currency transfer is converted at the provider's rate, with the 1.5% fee charged on top to the sender only")
    void foreignExchangeTransferConvertsAndChargesFee() {
        source.deposit(Money.sek("2000.00"));
        stubBothAccounts();
        when(exchangeRateProvider.getExchangeRate(USD, Money.SEK)).thenReturn(new BigDecimal("10.00"));
        when(timeMachineUseCase.getCurrentTime()).thenReturn(NOW);
        when(transferRepository.save(any(Transfer.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Transfer result = transferService.executeTransfer(
                sourceNumber(), targetNumber(), new BigDecimal("100"), "USD", "Invoice");

        // 100 USD * 10.00 = 1000.00 SEK, a fee of 1.5% = 15.00 SEK
        assertThat(result.getAmount().amount()).isEqualByComparingTo("1000.00");
        assertThat(result.getAmount().currency()).isEqualTo(Money.SEK);
        assertThat(result.getFee().amount()).isEqualByComparingTo("15.00");
        assertThat(source.getBalance().amount()).isEqualByComparingTo("985.00");
        assertThat(target.getBalance().amount()).isEqualByComparingTo("1000.00");
    }

    @Test
    @DisplayName("a transfer exceeding the overdraft limit is rejected and nothing is saved")
    void transferBeyondOverdraftIsRejected() {
        stubBothAccounts();

        assertThatThrownBy(() -> transferService.executeTransfer(
                sourceNumber(), targetNumber(), new BigDecimal("600.00"), "SEK", "Too much"))
                .isInstanceOf(InsufficientFundsException.class);

        verify(accountRepository, never()).save(any(Account.class));
        verify(transferRepository, never()).save(any(Transfer.class));
    }

    @Test
    @DisplayName("the fee counts towards the overdraft limit: 495.00 SEK alone would fit, 495.00 + the fee does not")
    void feeCountsTowardsOverdraftLimit() {
        stubBothAccounts();
        when(exchangeRateProvider.getExchangeRate(USD, Money.SEK)).thenReturn(new BigDecimal("4.95"));

        assertThatThrownBy(() -> transferService.executeTransfer(
                sourceNumber(), targetNumber(), new BigDecimal("100"), "USD", "Edge case"))
                .isInstanceOf(InsufficientFundsException.class);

        assertThat(source.getBalance().amount()).isEqualByComparingTo("0.00");
        verify(transferRepository, never()).save(any(Transfer.class));
    }

    @Test
    @DisplayName("an unknown source account is reported as IllegalArgumentException")
    void unknownSourceAccountIsRejected() {
        when(accountRepository.findByAccountNumber(new AccountNumber("SE1234567890123456789012")))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> transferService.executeTransfer(
                "SE1234567890123456789012", targetNumber(), new BigDecimal("10.00"), "SEK", "Ghost"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Source account not found");
    }

    @Test
    @DisplayName("an unknown target account is reported as IllegalArgumentException")
    void unknownTargetAccountIsRejected() {
        when(accountRepository.findByAccountNumber(source.getAccountNumber())).thenReturn(Optional.of(source));
        when(accountRepository.findByAccountNumber(new AccountNumber("SE2109876543210987654321")))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> transferService.executeTransfer(
                sourceNumber(), "SE2109876543210987654321", new BigDecimal("10.00"), "SEK", "Ghost"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Target account not found");
    }

    @Test
    @DisplayName("a currency code that is not ISO 4217 is rejected before any account lookup")
    void invalidCurrencyCodeIsRejectedUpFront() {
        assertThatThrownBy(() -> transferService.executeTransfer(
                sourceNumber(), targetNumber(), new BigDecimal("10.00"), "ZZZ", "Bad currency"))
                .isInstanceOf(IllegalArgumentException.class);

        verifyNoInteractions(accountRepository);
    }

    @Test
    @DisplayName("transferring to the same account is rejected by the Transfer domain rule")
    void sameAccountTransferIsRejected() {
        source.deposit(Money.sek("100.00"));
        when(accountRepository.findByAccountNumber(source.getAccountNumber())).thenReturn(Optional.of(source));
        when(timeMachineUseCase.getCurrentTime()).thenReturn(NOW);

        assertThatThrownBy(() -> transferService.executeTransfer(
                sourceNumber(), sourceNumber(), new BigDecimal("10.00"), "SEK", "Self"))
                .isInstanceOf(SameAccountTransferException.class);

        verify(transferRepository, never()).save(any(Transfer.class));
    }

    @Test
    @DisplayName("getAccountHistory returns what the repository holds for the resolved account id")
    void historyDelegatesToRepository() {
        Transfer transfer = new Transfer(TransferId.generate(), source.getId(), target.getId(),
                Money.sek("10.00"), Money.zero(Money.SEK), TransferStatus.COMPLETED, NOW, "Coffee");
        when(accountRepository.findByAccountNumber(source.getAccountNumber())).thenReturn(Optional.of(source));
        when(transferRepository.findAllByAccountId(source.getId().value())).thenReturn(List.of(transfer));

        List<Transfer> history = transferService.getAccountHistory(sourceNumber());

        assertThat(history).containsExactly(transfer);
    }

    @Test
    @DisplayName("getAccountHistory for an unknown account number is rejected")
    void historyForUnknownAccountIsRejected() {
        when(accountRepository.findByAccountNumber(new AccountNumber("SE1234567890123456789012")))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> transferService.getAccountHistory("SE1234567890123456789012"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Account not found");
    }
}