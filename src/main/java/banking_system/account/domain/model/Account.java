package banking_system.account.domain.model;

import banking_system.account.domain.exception.InsufficientFundsException;
import banking_system.shared.domain.model.Money;

import java.util.Objects;

public class Account {

    private final AccountId id;
    private final AccountNumber accountNumber;
    private final String ownerId;
    private Money balance;
    private final Money overdraftLimit; // Default - 500.00 SEK

    public Account(AccountId id, AccountNumber accountNumber, String ownerId, Money balance, Money overdraftLimit) {
        this.id = Objects.requireNonNull(id);
        this.accountNumber = Objects.requireNonNull(accountNumber);
        this.ownerId = Objects.requireNonNull(ownerId);
        this.balance = Objects.requireNonNull(balance);
        this.overdraftLimit = Objects.requireNonNull(overdraftLimit);
    }

    public void deposit(Money amount) {
        if (amount.isNegative() || amount.amount().signum() == 0) {
            throw new IllegalArgumentException("Deposit amount must be positive");
        }
        this.balance = this.balance.add(amount);
    }

    public void withdraw(Money amount) {
        if (amount.isNegative() || amount.amount().signum() == 0) {
            throw new IllegalArgumentException("Withdrawal amount must be positive");
        }

        Money minimumAllowedBalance = overdraftLimit.isNegative()
                ? overdraftLimit
                : overdraftLimit.subtract(overdraftLimit).subtract(overdraftLimit); // -overdraftLimit

        Money balanceAfterWithdrawal = this.balance.subtract(amount);

        if (balanceAfterWithdrawal.isLessThan(minimumAllowedBalance)) {
            throw new InsufficientFundsException(
                    "Transaction rejected. Maximum allowed overdraft limit of " + overdraftLimit + " exceeded."
            );
        }

        this.balance = balanceAfterWithdrawal;
    }

    public AccountId getId() { return id; }
    public AccountNumber getAccountNumber() { return accountNumber; }
    public String getOwnerId() { return ownerId; }
    public Money getBalance() { return balance; }
    public Money getOverdraftLimit() { return overdraftLimit; }
}
