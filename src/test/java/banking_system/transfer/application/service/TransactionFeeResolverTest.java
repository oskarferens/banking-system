package banking_system.transfer.application.service;

import banking_system.transfer.domain.model.TransferType;
import banking_system.transfer.infrastructure.adapter.out.fee.DomesticTransferFeeStrategy;
import banking_system.transfer.infrastructure.adapter.out.fee.ForeignExchangeFeeStrategy;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TransactionFeeResolverTest {

    @Test
    @DisplayName("resolves the domestic strategy for DOMESTIC transfers")
    void resolvesDomesticStrategy() {
        DomesticTransferFeeStrategy domestic = new DomesticTransferFeeStrategy();
        TransactionFeeResolver resolver = new TransactionFeeResolver(
                List.of(domestic, new ForeignExchangeFeeStrategy()));

        assertThat(resolver.resolve(TransferType.DOMESTIC)).isSameAs(domestic);
    }

    @Test
    @DisplayName("resolves the foreign exchange strategy for FOREIGN_EXCHANGE transfers")
    void resolvesForeignExchangeStrategy() {
        ForeignExchangeFeeStrategy foreignExchange = new ForeignExchangeFeeStrategy();
        TransactionFeeResolver resolver = new TransactionFeeResolver(
                List.of(new DomesticTransferFeeStrategy(), foreignExchange));

        assertThat(resolver.resolve(TransferType.FOREIGN_EXCHANGE)).isSameAs(foreignExchange);
    }

    @Test
    @DisplayName("throws IllegalStateException when no registered strategy supports the transfer type")
    void throwsWhenNoStrategySupportsType() {
        TransactionFeeResolver resolver = new TransactionFeeResolver(List.of(new DomesticTransferFeeStrategy()));

        assertThatThrownBy(() -> resolver.resolve(TransferType.FOREIGN_EXCHANGE))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("FOREIGN_EXCHANGE");
    }
}