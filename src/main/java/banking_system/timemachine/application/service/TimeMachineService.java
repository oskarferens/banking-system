package banking_system.timemachine.application.service;

import banking_system.timemachine.domain.port.TimeMachineUseCase;
import banking_system.timemachine.domain.model.TimeMachine;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
public class TimeMachineService implements TimeMachineUseCase {

    private final TimeMachine timeMachine = new TimeMachine();

    @Override
    public Instant getCurrentTime() {
        return timeMachine.now();
    }

    @Override
    public Instant skipDays(long days) {
        timeMachine.skipDays(days);
        return timeMachine.now();
    }

    @Override
    public void resetTime() {
        timeMachine.reset();
    }

    public TimeMachine getTimeMachine() {
        return timeMachine;
    }
}