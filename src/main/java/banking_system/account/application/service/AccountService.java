package banking_system.account.application.service;

import banking_system.account.application.port.in.AccountUseCase;
import banking_system.account.application.port.out.AccountRepositoryPort;
import banking_system.account.domain.exception.AccountNotFoundException;
import banking_system.account.domain.model.Account;
import banking_system.account.domain.model.AccountFactory;
import banking_system.account.domain.model.AccountId;
import banking_system.shared.domain.model.Money;
import org.springframework.stereotype.Service;

@Service
public class AccountService implements AccountUseCase {

    private final AccountRepositoryPort accountRepositoryPort;

    public AccountService(AccountRepositoryPort accountRepositoryPort) {
        this.accountRepositoryPort = accountRepositoryPort;
    }

    @Override
    public Account createAccount(String ownerId) {
        Account account = AccountFactory.createStandardAccount(ownerId);
        return accountRepositoryPort.save(account);
    }

    @Override
    public Account getAccount(String accountId) {
        return accountRepositoryPort.findById(new AccountId(accountId))
                .orElseThrow(() -> new AccountNotFoundException(accountId));
    }

    @Override
    public Account deposit(String accountId, Money amount) {
        Account account = getAccount(accountId);
        account.deposit(amount);
        return accountRepositoryPort.save(account);
    }

    @Override
    public Account withdraw(String accountId, Money amount) {
        Account account = getAccount(accountId);
        account.withdraw(amount);
        return accountRepositoryPort.save(account);
    }
}
