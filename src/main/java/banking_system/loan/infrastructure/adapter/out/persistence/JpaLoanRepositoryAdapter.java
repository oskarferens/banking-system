package banking_system.loan.infrastructure.adapter.out.persistence;

import banking_system.loan.domain.model.Loan;
import banking_system.loan.domain.model.LoanId;
import banking_system.loan.domain.port.LoanRepositoryPort;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
public class JpaLoanRepositoryAdapter implements LoanRepositoryPort {

    private final SpringDataLoanRepository repository;
    private final LoanMapper mapper;

    public JpaLoanRepositoryAdapter(SpringDataLoanRepository repository, LoanMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public Loan save(Loan loan) {
        LoanEntity entity = mapper.toEntity(loan);
        LoanEntity saved = repository.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    public Optional<Loan> findById(LoanId id) {
        return repository.findById(id.value())
                .map(mapper::toDomain);
    }

    @Override
    public List<Loan> findAllByBorrowerAccountId(String accountId) {
        return repository.findByBorrowerAccountId(accountId).stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<Loan> findAll() {
        return repository.findAll().stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }
}