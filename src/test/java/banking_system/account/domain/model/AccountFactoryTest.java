package banking_system.account.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class AccountFactoryTest {

    @Test
    @DisplayName("creates a standard account with zero balance and the default overdraft limit")
    void createsStandardAccountWithDefaults() {
        Account account = AccountFactory.createStandardAccount("owner-1");

        assertThat(account.getOwnerId()).isEqualTo("owner-1");
        assertThat(account.getBalance().amount()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(account.getOverdraftLimit().amount()).isEqualByComparingTo(new BigDecimal("-500.00"));
    }

    @Test
    @DisplayName("generates a valid Swedish account number")
    void generatesValidAccountNumber() {
        Account account = AccountFactory.createStandardAccount("owner-1");

        assertThat(account.getAccountNumber().value()).matches("^SE\\d{22}$");
    }

    @Test
    @DisplayName("generates a unique account number and id for each account")
    void generatesUniqueIdentifiers() {
        Account first = AccountFactory.createStandardAccount("owner-1");
        Account second = AccountFactory.createStandardAccount("owner-1");

        assertThat(first.getId()).isNotEqualTo(second.getId());
        assertThat(first.getAccountNumber()).isNotEqualTo(second.getAccountNumber());
    }
}