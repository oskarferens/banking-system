package banking_system.account.domain.model;

import banking_system.account.domain.exception.InsufficientFundsException;
import banking_system.shared.domain.model.Money;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AccountTest {

    private Account account;

    @BeforeEach
    void setUp() {
        account = AccountFactory.createStandardAccount("owner-1");
    }

    @Test
    @DisplayName("deposit increases the balance")
    void depositIncreasesBalance() {
        account.deposit(Money.sek("100.00"));

        assertThat(account.getBalance().amount()).isEqualByComparingTo(new BigDecimal("100.00"));
    }

    @Test
    @DisplayName("deposit rejects a zero amount")
    void depositRejectsZero() {
        assertThatThrownBy(() -> account.deposit(Money.sek("0.00")))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("deposit rejects a negative amount")
    void depositRejectsNegative() {
        assertThatThrownBy(() -> account.deposit(Money.sek("-10.00")))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("withdraw decreases the balance")
    void withdrawDecreasesBalance() {
        account.deposit(Money.sek("200.00"));

        account.withdraw(Money.sek("50.00"));

        assertThat(account.getBalance().amount()).isEqualByComparingTo(new BigDecimal("150.00"));
    }

    @Test
    @DisplayName("withdraw is allowed exactly down to the overdraft limit")
    void withdrawAllowedExactlyAtOverdraftLimit() {
        account.withdraw(Money.sek("500.00"));

        assertThat(account.getBalance().amount()).isEqualByComparingTo(new BigDecimal("-500.00"));
    }

    @Test
    @DisplayName("withdraw beyond the overdraft limit is rejected")
    void withdrawBeyondOverdraftLimitRejected() {
        assertThatThrownBy(() -> account.withdraw(Money.sek("500.01")))
                .isInstanceOf(InsufficientFundsException.class);
    }

    @Test
    @DisplayName("withdraw rejects a zero amount")
    void withdrawRejectsZero() {
        assertThatThrownBy(() -> account.withdraw(Money.sek("0.00")))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("withdraw rejects a negative amount")
    void withdrawRejectsNegative() {
        assertThatThrownBy(() -> account.withdraw(Money.sek("-10.00")))
                .isInstanceOf(IllegalArgumentException.class);
    }
}