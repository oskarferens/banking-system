package banking_system.shared.domain.port;

import java.time.Instant;

public interface EndOfDaySimulationUseCase {
    Instant advanceDays(long days);
}