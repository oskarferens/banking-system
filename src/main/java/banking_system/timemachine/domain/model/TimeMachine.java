package banking_system.timemachine.domain.model;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;

public class TimeMachine {

    private Clock clock;

    public TimeMachine() {
        this.clock = Clock.systemUTC();
    }

    public Instant now() {
        return clock.instant();
    }

    public ZoneId getZone() {
        return clock.getZone();
    }

    public Clock getClock() {
        return clock;
    }

    public synchronized void skipDays(long days) {
        if (days < 0) {
            throw new IllegalArgumentException("Cannot travel backwards in time");
        }
        this.clock = Clock.offset(this.clock, Duration.ofDays(days));
    }

    public synchronized void reset() {
        this.clock = Clock.systemUTC();
    }
}