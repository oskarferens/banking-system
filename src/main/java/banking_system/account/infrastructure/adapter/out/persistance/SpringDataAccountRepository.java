package banking_system.account.infrastructure.adapter.out.persistance;

import org.springframework.data.jpa.repository.JpaRepository;

public interface SpringDataAccountRepository extends JpaRepository<AccountEntity, String> {
}
