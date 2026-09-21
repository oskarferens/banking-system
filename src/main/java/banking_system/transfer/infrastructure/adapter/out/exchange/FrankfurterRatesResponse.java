package banking_system.transfer.infrastructure.adapter.out.exchange;

import java.math.BigDecimal;
import java.util.Map;

record FrankfurterRatesResponse(String base, String date, Map<String, BigDecimal> rates) {
}