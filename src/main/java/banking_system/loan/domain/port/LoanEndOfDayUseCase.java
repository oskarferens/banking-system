package banking_system.loan.domain.port;

import java.time.Instant;

public interface LoanEndOfDayUseCase {
    void processDay(Instant currentVirtualTime);
}