package banking_system.transfer.domain.port;

import java.math.BigDecimal;
import java.util.Currency;

public interface ExchangeRateProviderPort {
    BigDecimal getExchangeRate(Currency from, Currency to);
}