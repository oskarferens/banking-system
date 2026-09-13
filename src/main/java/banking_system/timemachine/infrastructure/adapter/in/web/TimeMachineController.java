package banking_system.timemachine.infrastructure.adapter.in.web;

import banking_system.timemachine.domain.port.TimeMachineUseCase;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/time-machine")
public class TimeMachineController {

    private final TimeMachineUseCase timeMachineUseCase;

    public TimeMachineController(TimeMachineUseCase timeMachineUseCase) {
        this.timeMachineUseCase = timeMachineUseCase;
    }

    @GetMapping("/current")
    public ResponseEntity<Map<String, Object>> getCurrentTime() {
        return ResponseEntity.ok(Map.of("currentTime", timeMachineUseCase.getCurrentTime()));
    }

    @PostMapping("/skip")
    public ResponseEntity<Map<String, Object>> skipDays(@RequestParam long days) {
        Instant newTime = timeMachineUseCase.skipDays(days);
        return ResponseEntity.ok(Map.of(
                "message", "Advanced time by " + days + " days",
                "currentTime", newTime
        ));
    }

    @PostMapping("/reset")
    public ResponseEntity<Map<String, String>> resetTime() {
        timeMachineUseCase.resetTime();
        return ResponseEntity.ok(Map.of("message", "Time Machine reset to system UTC"));
    }
}