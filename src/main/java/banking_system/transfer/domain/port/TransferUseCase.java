package banking_system.transfer.domain.port;

import banking_system.transfer.domain.model.Transfer;

import java.math.BigDecimal;
import java.util.List;

public interface TransferUseCase {
    Transfer executeTransfer(String sourceAccountNumber, String targetAccountNumber,
                             BigDecimal amount, String currency, String title);
    List<Transfer> getAccountHistory(String accountNumber);
}