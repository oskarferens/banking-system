package banking_system.loan.domain.port;

import banking_system.loan.domain.model.Loan;
import banking_system.loan.domain.model.LoanId;

import java.util.List;
import java.util.Optional;

public interface LoanRepositoryPort {
    Loan save(Loan loan);
    Optional<Loan> findById(LoanId id);
    List<Loan> findAllByBorrowerAccountId(String accountId);
    List<Loan> findAll();
}