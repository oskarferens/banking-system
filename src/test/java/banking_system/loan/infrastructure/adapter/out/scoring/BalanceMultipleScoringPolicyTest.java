package banking_system.loan.infrastructure.adapter.out.scoring;

import banking_system.account.domain.model.Account;
import banking_system.account.domain.model.AccountFactory;
import banking_system.shared.domain.model.Money;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class BalanceMultipleScoringPolicyTest {

    private final BalanceMultipleScoringPolicy policy = new BalanceMultipleScoringPolicy();
    private Account account;

    @BeforeEach
    void setUp() {
        account = AccountFactory.createStandardAccount("owner-1");
        account.deposit(Money.sek("1000.00"));
    }

    @Test
    @DisplayName("approves a request exactly at 3x the current balance")
    void approvesExactlyAtTheMultiple() {
        assertThat(policy.isEligible(account, Money.sek("3000.00"))).isTrue();
    }

    @Test
    @DisplayName("rejects a request just above 3x the current balance")
    void rejectsJustAboveTheMultiple() {
        assertThat(policy.isEligible(account, Money.sek("3000.01"))).isFalse();
    }

    @Test
    @DisplayName("approves a small request well under the multiple")
    void approvesSmallRequest() {
        assertThat(policy.isEligible(account, Money.sek("100.00"))).isTrue();
    }

    @Test
    @DisplayName("rejects any loan request when the account balance is zero")
    void rejectsWhenBalanceIsZero() {
        Account freshAccount = AccountFactory.createStandardAccount("owner-2");

        assertThat(policy.isEligible(freshAccount, Money.sek("1.00"))).isFalse();
    }

    @Test
    @DisplayName("rejects any loan request when the account balance is negative")
    void rejectsWhenBalanceIsNegative() {
        Account overdrawnAccount = AccountFactory.createStandardAccount("owner-3");
        overdrawnAccount.withdraw(Money.sek("200.00"));

        assertThat(policy.isEligible(overdrawnAccount, Money.sek("1.00"))).isFalse();
    }
}