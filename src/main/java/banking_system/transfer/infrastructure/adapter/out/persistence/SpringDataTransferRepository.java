package banking_system.transfer.infrastructure.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface SpringDataTransferRepository extends JpaRepository<banking_system.transfer.infrastructure.adapter.out.persistence.TransferEntity, String> {

    // Query that finds transfers where the specified account is either the sender or the recipient.
    @Query("SELECT t FROM TransferEntity t WHERE t.sourceAccountId = :accountId OR t.targetAccountId = :accountId ORDER BY t.timestamp DESC")
    List<banking_system.transfer.infrastructure.adapter.out.persistence.TransferEntity> findAllBySourceAccountIdOrTargetAccountId(@Param("accountId") String accountId);
}