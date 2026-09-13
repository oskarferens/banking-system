package banking_system.transfer.domain.port;

import banking_system.transfer.domain.model.Transfer;
import banking_system.transfer.domain.model.TransferId;

import java.util.List;
import java.util.Optional;

public interface TransferRepositoryPort {
    Transfer save(Transfer transfer);
    Optional<Transfer> findById(TransferId id);
    List<Transfer> findAllByAccountId(String accountId);
}