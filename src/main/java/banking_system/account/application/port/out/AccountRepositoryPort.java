package banking_system.account.application.port.out;

import banking_system.account.domain.model.Account;
import banking_system.account.domain.model.AccountId;

import java.util.Optional;

public interface AccountRepositoryPort {
    Account save(Account account);
    Optional<Account> findById(AccountId id);
}