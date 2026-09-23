package banking_system.shared.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Currency;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MoneyTest {

    private static final Currency SEK = Currency.getInstance("SEK");
    private static final Currency USD = Currency.getInstance("USD");

    @Test
    @DisplayName("rounds amount to 2 decimal places using HALF_UP")
    void roundsAmountToTwoDecimalPlaces() {
        Money money = Money.sek("1.005");

        assertThat(money.amount()).isEqualByComparingTo(new BigDecimal("1.01"));
    }

    @Test
    @DisplayName("zero() creates a Money with amount 0.00 in the given currency")
    void zeroCreatesZeroAmount() {
        Money money = Money.zero(SEK);

        assertThat(money.amount()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(money.currency()).isEqualTo(SEK);
    }

    @Test
    @DisplayName("rejects a null amount")
    void rejectsNullAmount() {
        assertThatNullPointerException()
                .isThrownBy(() -> new Money(null, SEK));
    }

    @Test
    @DisplayName("rejects a null currency")
    void rejectsNullCurrency() {
        assertThatNullPointerException()
                .isThrownBy(() -> new Money(BigDecimal.TEN, null));
    }

    @Test
    @DisplayName("adds two amounts in the same currency")
    void addsAmountsInSameCurrency() {
        Money result = Money.sek("100.00").add(Money.sek("50.50"));

        assertThat(result.amount()).isEqualByComparingTo(new BigDecimal("150.50"));
    }

    @Test
    @DisplayName("subtracts two amounts in the same currency")
    void subtractsAmountsInSameCurrency() {
        Money result = Money.sek("100.00").subtract(Money.sek("30.00"));

        assertThat(result.amount()).isEqualByComparingTo(new BigDecimal("70.00"));
    }

    @Test
    @DisplayName("rejects adding amounts in different currencies")
    void rejectsAddingDifferentCurrencies() {
        Money sek = Money.sek("100.00");
        Money usd = new Money(new BigDecimal("100.00"), USD);

        assertThatThrownBy(() -> sek.add(usd))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("different currencies");
    }

    @Test
    @DisplayName("rejects subtracting amounts in different currencies")
    void rejectsSubtractingDifferentCurrencies() {
        Money sek = Money.sek("100.00");
        Money usd = new Money(new BigDecimal("100.00"), USD);

        assertThatThrownBy(() -> sek.subtract(usd))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("different currencies");
    }

    @Test
    @DisplayName("isGreaterThanOrEqual is true when amounts are equal")
    void isGreaterThanOrEqualTrueWhenEqual() {
        assertThat(Money.sek("100.00").isGreaterThanOrEqual(Money.sek("100.00"))).isTrue();
    }

    @Test
    @DisplayName("isGreaterThanOrEqual is false when amount is smaller")
    void isGreaterThanOrEqualFalseWhenSmaller() {
        assertThat(Money.sek("99.99").isGreaterThanOrEqual(Money.sek("100.00"))).isFalse();
    }

    @Test
    @DisplayName("isLessThan is strict - false when equal")
    void isLessThanStrict() {
        Money a = Money.sek("99.99");
        Money b = Money.sek("100.00");

        assertThat(a.isLessThan(b)).isTrue();
        assertThat(b.isLessThan(a)).isFalse();
        assertThat(a.isLessThan(a)).isFalse();
    }

    @Test
    @DisplayName("isNegative reflects the sign of the amount")
    void isNegativeReflectsSign() {
        assertThat(Money.sek("-1.00").isNegative()).isTrue();
        assertThat(Money.sek("0.00").isNegative()).isFalse();
        assertThat(Money.sek("1.00").isNegative()).isFalse();
    }
}