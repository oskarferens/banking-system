package banking_system.account.infrastructure.adapter.out.persistance;

import banking_system.account.domain.model.Account;
import banking_system.account.domain.model.AccountId;
import banking_system.account.domain.model.AccountNumber;
import banking_system.shared.domain.model.Money;

import java.util.Currency;

public class AccountMapper {

    public static AccountEntity toEntity(Account account) {
        return new AccountEntity(
                account.getId().value(),
                account.getAccountNumber().value(),
                account.getOwnerId(),
                account.getBalance().amount(),
                account.getBalance().currency().getCurrencyCode(),
                account.getOverdraftLimit().amount()
        );
    }

    public static Account toDomain(AccountEntity entity) {
        Currency currency = Currency.getInstance(entity.getCurrency());
        return new Account(
                new AccountId(entity.getId()),
                new AccountNumber(entity.getAccountNumber()),
                entity.getOwnerId(),
                new Money(entity.getBalanceAmount(), currency),
                new Money(entity.getOverdraftLimit(), currency)
        );
    }
}
