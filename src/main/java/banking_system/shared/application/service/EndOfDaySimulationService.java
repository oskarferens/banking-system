package banking_system.shared.application.service;

import banking_system.loan.domain.port.LoanEndOfDayUseCase;
import banking_system.shared.domain.port.EndOfDaySimulationUseCase;
import banking_system.timemachine.domain.port.TimeMachineUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class EndOfDaySimulationService implements EndOfDaySimulationUseCase {

    private final TimeMachineUseCase timeMachineUseCase;
    private final LoanEndOfDayUseCase loanEndOfDayUseCase;

    @Override
    public Instant advanceDays(long days) {
        if (days <= 0) {
            throw new IllegalArgumentException("Days to advance must be positive");
        }

        Instant currentTime = timeMachineUseCase.getCurrentTime();
        for (long i = 0; i < days; i++) {
            currentTime = timeMachineUseCase.skipDays(1);
            loanEndOfDayUseCase.processDay(currentTime);
        }

        return currentTime;
    }
}