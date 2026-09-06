package banking_system.timemachine.application.port.in;

import java.time.Instant;

public interface TimeMachineUseCase {
    Instant getCurrentTime();
    Instant skipDays(long days);
    void resetTime();
}