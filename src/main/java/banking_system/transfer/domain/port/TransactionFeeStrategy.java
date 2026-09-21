package banking_system.transfer.domain.port;

import banking_system.shared.domain.model.Money;
import banking_system.transfer.domain.model.TransferType;

public interface TransactionFeeStrategy {
    boolean supports(TransferType transferType);
    Money calculateFee(Money transferAmount);
}