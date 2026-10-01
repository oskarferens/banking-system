package banking_system.shared.application.service;

import banking_system.loan.domain.port.LoanEndOfDayUseCase;
import banking_system.timemachine.domain.port.TimeMachineUseCase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EndOfDaySimulationServiceTest {

    @Mock
    private TimeMachineUseCase timeMachineUseCase;

    @Mock
    private LoanEndOfDayUseCase loanEndOfDayUseCase;

    @InjectMocks
    private EndOfDaySimulationService endOfDaySimulationService;

    @Test
    @DisplayName("advanceDays(3) skips the clock one day at a time and processes each resulting day")
    void advancesOneDayAtATimeAndProcessesEach() {
        Instant day1 = Instant.parse("2026-01-02T00:00:00Z");
        Instant day2 = Instant.parse("2026-01-03T00:00:00Z");
        Instant day3 = Instant.parse("2026-01-04T00:00:00Z");
        when(timeMachineUseCase.skipDays(1)).thenReturn(day1, day2, day3);

        Instant result = endOfDaySimulationService.advanceDays(3);

        verify(timeMachineUseCase, times(3)).skipDays(1);
        verify(loanEndOfDayUseCase).processDay(day1);
        verify(loanEndOfDayUseCase).processDay(day2);
        verify(loanEndOfDayUseCase).processDay(day3);
        assertThat(result).isEqualTo(day3);
    }

    @Test
    @DisplayName("advanceDays() rejects zero days")
    void rejectsZeroDays() {
        assertThatThrownBy(() -> endOfDaySimulationService.advanceDays(0))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("advanceDays() rejects a negative number of days")
    void rejectsNegativeDays() {
        assertThatThrownBy(() -> endOfDaySimulationService.advanceDays(-5))
                .isInstanceOf(IllegalArgumentException.class);
    }
}