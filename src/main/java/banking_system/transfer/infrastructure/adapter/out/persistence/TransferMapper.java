package banking_system.transfer.infrastructure.adapter.out.persistence;

import banking_system.account.domain.model.AccountId;
import banking_system.shared.domain.model.Money;
import banking_system.transfer.domain.model.Transfer;
import banking_system.transfer.domain.model.TransferId;
import banking_system.transfer.domain.model.TransferStatus;
import org.springframework.stereotype.Component;

import java.util.Currency;

@Component
public class TransferMapper {

    public TransferEntity toEntity(Transfer domain) {
        if (domain == null) return null;
        return TransferEntity.builder()
                .id(domain.getId().value())
                .sourceAccountId(domain.getSourceAccountId().value())
                .targetAccountId(domain.getTargetAccountId().value())
                .amount(domain.getAmount().amount())
                .feeAmount(domain.getFee().amount())
                .currency(domain.getAmount().currency().getCurrencyCode())
                .status(domain.getStatus().name())
                .timestamp(domain.getTimestamp())
                .title(domain.getTitle())
                .build();
    }

    public Transfer toDomain(TransferEntity entity) {
        if (entity == null) return null;
        Currency currency = Currency.getInstance(entity.getCurrency());
        return new Transfer(
                new TransferId(entity.getId()),
                new AccountId(entity.getSourceAccountId()),
                new AccountId(entity.getTargetAccountId()),
                new Money(entity.getAmount(), currency),
                new Money(entity.getFeeAmount(), currency),
                TransferStatus.valueOf(entity.getStatus()),
                entity.getTimestamp(),
                entity.getTitle()
        );
    }
}