package banking_system.account.domain.port;

import banking_system.account.domain.model.Account;
import banking_system.account.domain.model.AccountId;
import banking_system.account.domain.model.AccountNumber;

import java.util.Optional;

public interface AccountRepositoryPort {
    Account save(Account account);
    Optional<Account> findById(AccountId id);
    Optional<Account> findByAccountNumber(AccountNumber accountNumber); // New method required for transfers
}