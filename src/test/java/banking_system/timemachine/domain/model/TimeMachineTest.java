package banking_system.timemachine.domain.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TimeMachineTest {

    private TimeMachine timeMachine;

    @BeforeEach
    void setUp() {
        timeMachine = new TimeMachine();
    }

    @Test
    @DisplayName("a new time machine starts at the real system time")
    void startsAtSystemTime() {
        Instant before = Instant.now();
        Instant simulated = timeMachine.now();
        Instant after = Instant.now();

        assertThat(simulated).isBetween(before, after);
    }

    @Test
    @DisplayName("skipDays() moves the clock forward by the given number of days")
    void skipDaysMovesClockForward() {
        Instant before = Instant.now();

        timeMachine.skipDays(30);

        Instant simulated = timeMachine.now();
        Instant after = Instant.now();
        assertThat(simulated).isBetween(before.plus(30, ChronoUnit.DAYS), after.plus(30, ChronoUnit.DAYS));
    }

    @Test
    @DisplayName("consecutive skips accumulate")
    void skipDaysAccumulates() {
        Instant before = Instant.now();

        timeMachine.skipDays(10);
        timeMachine.skipDays(5);

        Instant simulated = timeMachine.now();
        Instant after = Instant.now();
        assertThat(simulated).isBetween(before.plus(15, ChronoUnit.DAYS), after.plus(15, ChronoUnit.DAYS));
    }

    @Test
    @DisplayName("skipping zero days is allowed and changes nothing")
    void skipZeroDaysIsAllowed() {
        Instant before = Instant.now();

        timeMachine.skipDays(0);

        Instant simulated = timeMachine.now();
        Instant after = Instant.now();
        assertThat(simulated).isBetween(before, after);
    }

    @Test
    @DisplayName("skipping a negative number of days is rejected")
    void skipNegativeDaysIsRejected() {
        assertThatThrownBy(() -> timeMachine.skipDays(-1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("backwards");
    }

    @Test
    @DisplayName("reset() returns the clock to the real system time")
    void resetReturnsToSystemTime() {
        timeMachine.skipDays(100);
        Instant before = Instant.now();

        timeMachine.reset();

        Instant simulated = timeMachine.now();
        Instant after = Instant.now();
        assertThat(simulated).isBetween(before, after);
    }

    @Test
    @DisplayName("the clock works in UTC")
    void usesUtcZone() {
        assertThat(timeMachine.getZone()).isEqualTo(ZoneOffset.UTC);
        assertThat(timeMachine.getClock()).isNotNull();
    }
}