package banking_system.loan.infrastructure.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SpringDataLoanRepository extends JpaRepository<LoanEntity, String> {
    List<LoanEntity> findByBorrowerAccountId(String borrowerAccountId);
}