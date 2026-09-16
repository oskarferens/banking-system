package banking_system.account.infrastructure.adapter.out.persistence;

import banking_system.account.domain.model.Account;
import banking_system.account.domain.model.AccountId;
import banking_system.account.domain.model.AccountNumber;
import banking_system.account.domain.port.AccountRepositoryPort;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@Primary
public class JpaAccountRepositoryAdapter implements AccountRepositoryPort {

    private final SpringDataAccountRepository repository;

    public JpaAccountRepositoryAdapter(SpringDataAccountRepository repository) {
        this.repository = repository;
    }

    @Override
    public Account save(Account account) {
        AccountEntity entity = AccountMapper.toEntity(account);
        AccountEntity saved = repository.save(entity);
        return AccountMapper.toDomain(saved);
    }

    @Override
    public Optional<Account> findById(AccountId id) {
        return repository.findById(id.value())
                .map(AccountMapper::toDomain);
    }

    @Override
    public Optional<Account> findByAccountNumber(AccountNumber accountNumber) {
        // Assume the account number is stored as a String in the entity (accountNumber.value()).
        return repository.findByAccountNumber(accountNumber.value())
                .map(AccountMapper::toDomain);
    }
}