package banking_system.transfer.infrastructure.adapter.out.persistence;

import banking_system.transfer.domain.model.Transfer;
import banking_system.transfer.domain.model.TransferId;
import banking_system.transfer.domain.port.TransferRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class JpaTransferRepositoryAdapter implements TransferRepositoryPort {

    private final SpringDataTransferRepository repository;
    private final TransferMapper mapper;

    @Override
    public Transfer save(Transfer transfer) {
        TransferEntity entity = mapper.toEntity(transfer);
        TransferEntity savedEntity = repository.save(entity);
        return mapper.toDomain(savedEntity);
    }

    @Override
    public Optional<Transfer> findById(TransferId id) {
        return repository.findById(id.value())
                .map(mapper::toDomain);
    }

    @Override
    public List<Transfer> findAllByAccountId(String accountId) {
        return repository.findAllBySourceAccountIdOrTargetAccountId(accountId).stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }
}