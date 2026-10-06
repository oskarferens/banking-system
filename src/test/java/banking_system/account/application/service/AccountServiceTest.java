package banking_system.account.application.service;

import banking_system.account.domain.exception.AccountNotFoundException;
import banking_system.account.domain.exception.InsufficientFundsException;
import banking_system.account.domain.model.Account;
import banking_system.account.domain.model.AccountFactory;
import banking_system.account.domain.model.AccountId;
import banking_system.account.domain.port.AccountRepositoryPort;
import banking_system.shared.domain.model.Money;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccountServiceTest {

    @Mock
    private AccountRepositoryPort accountRepository;

    @InjectMocks
    private AccountService accountService;

    private Account account;

    @BeforeEach
    void setUp() {
        account = AccountFactory.createStandardAccount("owner-1");
    }

    @Test
    @DisplayName("createAccount() saves a fresh standard account for the given owner")
    void createAccountSavesStandardAccount() {
        when(accountRepository.save(any(Account.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Account created = accountService.createAccount("owner-42");

        assertThat(created.getOwnerId()).isEqualTo("owner-42");
        assertThat(created.getBalance().amount()).isEqualByComparingTo("0.00");
        verify(accountRepository).save(created);
    }

    @Test
    @DisplayName("getAccount() returns the account the repository finds")
    void getAccountReturnsExistingAccount() {
        when(accountRepository.findById(account.getId())).thenReturn(Optional.of(account));

        Account found = accountService.getAccount(account.getId().value());

        assertThat(found).isSameAs(account);
    }

    @Test
    @DisplayName("getAccount() throws AccountNotFoundException for an unknown id")
    void getAccountThrowsWhenMissing() {
        AccountId missing = AccountId.generate();
        when(accountRepository.findById(missing)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> accountService.getAccount(missing.value()))
                .isInstanceOf(AccountNotFoundException.class)
                .hasMessageContaining(missing.value());
    }

    @Test
    @DisplayName("deposit() adds to the balance and saves the account")
    void depositIncreasesBalanceAndSaves() {
        when(accountRepository.findById(account.getId())).thenReturn(Optional.of(account));
        when(accountRepository.save(account)).thenReturn(account);

        Account result = accountService.deposit(account.getId().value(), Money.sek("150.00"));

        assertThat(result.getBalance().amount()).isEqualByComparingTo("150.00");
        verify(accountRepository).save(account);
    }

    @Test
    @DisplayName("deposit() on an unknown account throws and saves nothing")
    void depositThrowsWhenAccountMissing() {
        AccountId missing = AccountId.generate();
        when(accountRepository.findById(missing)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> accountService.deposit(missing.value(), Money.sek("10.00")))
                .isInstanceOf(AccountNotFoundException.class);

        verify(accountRepository, never()).save(any(Account.class));
    }

    @Test
    @DisplayName("withdraw() subtracts from the balance and saves the account")
    void withdrawDecreasesBalanceAndSaves() {
        account.deposit(Money.sek("300.00"));
        when(accountRepository.findById(account.getId())).thenReturn(Optional.of(account));
        when(accountRepository.save(account)).thenReturn(account);

        Account result = accountService.withdraw(account.getId().value(), Money.sek("100.00"));

        assertThat(result.getBalance().amount()).isEqualByComparingTo("200.00");
        verify(accountRepository).save(account);
    }

    @Test
    @DisplayName("withdraw() beyond the overdraft limit throws and saves nothing")
    void withdrawBeyondOverdraftIsRejected() {
        when(accountRepository.findById(account.getId())).thenReturn(Optional.of(account));

        assertThatThrownBy(() -> accountService.withdraw(account.getId().value(), Money.sek("500.01")))
                .isInstanceOf(InsufficientFundsException.class);

        verify(accountRepository, never()).save(any(Account.class));
    }

    @Test
    @DisplayName("withdraw() on an unknown account throws AccountNotFoundException")
    void withdrawThrowsWhenAccountMissing() {
        AccountId missing = AccountId.generate();
        when(accountRepository.findById(missing)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> accountService.withdraw(missing.value(), Money.sek("10.00")))
                .isInstanceOf(AccountNotFoundException.class);
    }
}