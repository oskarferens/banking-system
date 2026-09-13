package banking_system.timemachine.domain.port;

import java.time.Instant;

public interface TimeMachineUseCase {
    Instant getCurrentTime();
    Instant skipDays(long days);
    void resetTime();
}