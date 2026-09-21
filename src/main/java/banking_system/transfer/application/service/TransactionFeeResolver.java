package banking_system.transfer.application.service;

import banking_system.transfer.domain.model.TransferType;
import banking_system.transfer.domain.port.TransactionFeeStrategy;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class TransactionFeeResolver {

    private final List<TransactionFeeStrategy> strategies;

    public TransactionFeeResolver(List<TransactionFeeStrategy> strategies) {
        this.strategies = strategies;
    }

    public TransactionFeeStrategy resolve(TransferType transferType) {
        return strategies.stream()
                .filter(strategy -> strategy.supports(transferType))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "No fee strategy registered for transfer type: " + transferType));
    }
}