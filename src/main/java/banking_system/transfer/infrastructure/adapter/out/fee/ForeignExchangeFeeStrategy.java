package banking_system.transfer.infrastructure.adapter.out.fee;

import banking_system.shared.domain.model.Money;
import banking_system.transfer.domain.model.TransferType;
import banking_system.transfer.domain.port.TransactionFeeStrategy;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class ForeignExchangeFeeStrategy implements TransactionFeeStrategy {

    private static final BigDecimal FEE_RATE = new BigDecimal("0.015"); // 1.5%

    @Override
    public boolean supports(TransferType transferType) {
        return transferType == TransferType.FOREIGN_EXCHANGE;
    }

    @Override
    public Money calculateFee(Money transferAmount) {
        BigDecimal feeAmount = transferAmount.amount().multiply(FEE_RATE);
        return new Money(feeAmount, transferAmount.currency());
    }
}