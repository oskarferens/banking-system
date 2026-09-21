package banking_system.transfer.infrastructure.adapter.in.web;

import banking_system.transfer.domain.model.Transfer;
import banking_system.transfer.domain.port.TransferUseCase;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/v1/transfers")
public class TransferController {

    private final TransferUseCase transferUseCase;

    public TransferController(TransferUseCase transferUseCase) {
        this.transferUseCase = transferUseCase;
    }

    @PostMapping
    public ResponseEntity<TransferResponseDto> executeTransfer(@Valid @RequestBody TransferRequestDto request) {
        Transfer transfer = transferUseCase.executeTransfer(
                request.sourceAccountNumber(),
                request.targetAccountNumber(),
                request.amount(),
                request.currency(),
                request.title()
        );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(TransferResponseDto.fromDomain(transfer));
    }

    @GetMapping("/history/{accountNumber}")
    public ResponseEntity<List<TransferResponseDto>> getAccountHistory(@PathVariable String accountNumber) {
        List<Transfer> transfers = transferUseCase.getAccountHistory(accountNumber);
        List<TransferResponseDto> response = transfers.stream()
                .map(TransferResponseDto::fromDomain)
                .toList();

        return ResponseEntity.ok(response);
    }

    // DTO request
    public record TransferRequestDto(
            @NotBlank(message = "Source account number cannot be blank")
            String sourceAccountNumber,

            @NotBlank(message = "Target account number cannot be blank")
            String targetAccountNumber,

            @NotNull(message = "Amount cannot be null")
            @Positive(message = "Amount must be strictly positive")
            BigDecimal amount,

            @NotBlank(message = "Currency cannot be blank")
            String currency,

            @NotBlank(message = "Title cannot be blank")
            String title
    ) {}


    public record TransferResponseDto(
            String id,
            String sourceAccountId,
            String targetAccountId,
            BigDecimal amount,
            BigDecimal fee,
            String currency,
            String status,
            Instant timestamp,
            String title
    ) {
        public static TransferResponseDto fromDomain(Transfer transfer) {
            return new TransferResponseDto(
                    transfer.getId().value(),
                    transfer.getSourceAccountId().value(),
                    transfer.getTargetAccountId().value(),
                    transfer.getAmount().amount(),
                    transfer.getFee().amount(),
                    transfer.getAmount().currency().getCurrencyCode(),
                    transfer.getStatus().name(),
                    transfer.getTimestamp(),
                    transfer.getTitle()
            );
        }
    }
}