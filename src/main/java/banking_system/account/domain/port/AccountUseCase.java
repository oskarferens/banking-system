package banking_system.account.domain.port;

import banking_system.account.domain.model.Account;
import banking_system.shared.domain.model.Money;

public interface AccountUseCase {
    Account createAccount(String ownerId);
    Account getAccount(String accountId);
    Account deposit(String accountId, Money amount);
    Account withdraw(String accountId, Money amount);
}
