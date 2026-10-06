package banking_system.timemachine.application.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TimeMachineServiceTest {

    private TimeMachineService service;

    @BeforeEach
    void setUp() {
        service = new TimeMachineService();
    }

    @Test
    @DisplayName("getCurrentTime() starts at the real system time")
    void getCurrentTimeIsSystemTime() {
        Instant before = Instant.now();
        Instant current = service.getCurrentTime();
        Instant after = Instant.now();

        assertThat(current).isBetween(before, after);
    }

    @Test
    @DisplayName("skipDays() returns the new simulated time")
    void skipDaysReturnsNewTime() {
        Instant before = Instant.now();

        Instant returned = service.skipDays(7);

        Instant after = Instant.now();
        assertThat(returned).isBetween(before.plus(7, ChronoUnit.DAYS), after.plus(7, ChronoUnit.DAYS));
    }

    @Test
    @DisplayName("resetTime() brings the clock back to the real system time")
    void resetTimeReturnsToSystemTime() {
        service.skipDays(50);
        Instant before = Instant.now();

        service.resetTime();

        Instant current = service.getCurrentTime();
        Instant after = Instant.now();
        assertThat(current).isBetween(before, after);
    }

    @Test
    @DisplayName("a negative skip is rejected")
    void negativeSkipIsRejected() {
        assertThatThrownBy(() -> service.skipDays(-1))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("the exposed TimeMachine shares state with the service")
    void exposedTimeMachineSharesState() {
        service.skipDays(3);

        assertThat(service.getTimeMachine().now()).isAfter(Instant.now().plus(2, ChronoUnit.DAYS));
    }
}