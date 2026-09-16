package banking_system.account.infrastructure.adapter.out.persistence;

import banking_system.account.domain.model.Account;
import banking_system.account.domain.model.AccountId;
import banking_system.account.domain.model.AccountNumber;
import banking_system.account.domain.port.AccountRepositoryPort;
import org.springframework.stereotype.Repository;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class InMemoryAccountRepository implements AccountRepositoryPort {

    private final Map<String, Account> storage = new ConcurrentHashMap<>();

    @Override
    public Account save(Account account) {
        storage.put(account.getId().value(), account);
        return account;
    }

    @Override
    public Optional<Account> findById(AccountId id) {
        return Optional.ofNullable(storage.get(id.value()));
    }

    @Override
    public Optional<Account> findByAccountNumber(AccountNumber accountNumber) {
        return storage.values().stream()
                .filter(account -> account.getAccountNumber().equals(accountNumber))
                .findFirst();
    }
}