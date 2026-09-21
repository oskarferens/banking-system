package banking_system.transfer.infrastructure.adapter.out.fee;

import banking_system.shared.domain.model.Money;
import banking_system.transfer.domain.model.TransferType;
import banking_system.transfer.domain.port.TransactionFeeStrategy;
import org.springframework.stereotype.Component;

@Component
public class DomesticTransferFeeStrategy implements TransactionFeeStrategy {

    @Override
    public boolean supports(TransferType transferType) {
        return transferType == TransferType.DOMESTIC;
    }

    @Override
    public Money calculateFee(Money transferAmount) {
        return Money.zero(transferAmount.currency());
    }
}