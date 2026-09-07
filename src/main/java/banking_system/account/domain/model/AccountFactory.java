package banking_system.account.domain.model;

import banking_system.shared.domain.model.Money;

import java.math.BigDecimal;

public class AccountFactory {

    private static final Money DEFAULT_OVERDRAFT_LIMIT = Money.sek(new BigDecimal("-500.00"));

    public static Account createStandardAccount(String ownerId) {
        AccountId id = AccountId.generate();
        AccountNumber accountNumber = generateSwedishAccountNumber();
        Money initialBalance = Money.zero(Money.SEK);

        return new Account(id, accountNumber, ownerId, initialBalance, DEFAULT_OVERDRAFT_LIMIT);
    }

    private static AccountNumber generateSwedishAccountNumber() {
        StringBuilder sb = new StringBuilder("SE");
        for (int i = 0; i < 22; i++) {
            sb.append((int) (Math.random() * 10));
        }
        return new AccountNumber(sb.toString());
    }
}