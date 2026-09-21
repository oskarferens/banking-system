package banking_system.transfer.infrastructure.adapter.out.exchange;

import banking_system.transfer.domain.exception.ExchangeRateUnavailableException;
import banking_system.transfer.domain.port.ExchangeRateProviderPort;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.math.BigDecimal;
import java.util.Currency;

@Component
public class FrankfurterExchangeRateAdapter implements ExchangeRateProviderPort {

    private static final String BASE_URL = "https://api.frankfurter.dev/v1/latest";

    private final RestClient restClient;

    public FrankfurterExchangeRateAdapter() {
        this.restClient = RestClient.builder().baseUrl(BASE_URL).build();
    }

    @Override
    public BigDecimal getExchangeRate(Currency from, Currency to) {
        if (from.equals(to)) {
            return BigDecimal.ONE;
        }

        try {
            FrankfurterRatesResponse response = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .queryParam("base", from.getCurrencyCode())
                            .queryParam("symbols", to.getCurrencyCode())
                            .build())
                    .retrieve()
                    .body(FrankfurterRatesResponse.class);

            if (response == null || response.rates() == null) {
                throw new ExchangeRateUnavailableException(
                        "Exchange rate provider returned an empty response for "
                                + from.getCurrencyCode() + " -> " + to.getCurrencyCode());
            }

            BigDecimal rate = response.rates().get(to.getCurrencyCode());
            if (rate == null) {
                throw new ExchangeRateUnavailableException(
                        "No exchange rate available for " + from.getCurrencyCode() + " -> " + to.getCurrencyCode());
            }

            return rate;
        } catch (RestClientException e) {
            throw new ExchangeRateUnavailableException(
                    "Failed to fetch exchange rate for " + from.getCurrencyCode() + " -> " + to.getCurrencyCode(), e);
        }
    }
}