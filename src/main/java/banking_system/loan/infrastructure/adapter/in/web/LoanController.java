package banking_system.loan.infrastructure.adapter.in.web;

import banking_system.loan.domain.model.Installment;
import banking_system.loan.domain.model.Loan;
import banking_system.loan.domain.port.LoanUseCase;
import banking_system.shared.domain.model.Money;
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
@RequestMapping("/api/v1/loans")
public class LoanController {

    private final LoanUseCase loanUseCase;

    public LoanController(LoanUseCase loanUseCase) {
        this.loanUseCase = loanUseCase;
    }

    @PostMapping
    public ResponseEntity<LoanResponseDto> applyForLoan(@Valid @RequestBody LoanApplicationRequestDto request) {
        Loan loan = loanUseCase.applyForLoan(request.borrowerAccountId(), Money.sek(request.principal()), request.termInMonths());
        return ResponseEntity.status(HttpStatus.CREATED).body(LoanResponseDto.fromDomain(loan));
    }

    @GetMapping("/{id}")
    public ResponseEntity<LoanResponseDto> getLoan(@PathVariable String id) {
        Loan loan = loanUseCase.getLoan(id);
        return ResponseEntity.ok(LoanResponseDto.fromDomain(loan));
    }

    @GetMapping("/account/{accountId}")
    public ResponseEntity<List<LoanResponseDto>> getLoansForAccount(@PathVariable String accountId) {
        List<LoanResponseDto> response = loanUseCase.getLoansForAccount(accountId).stream()
                .map(LoanResponseDto::fromDomain)
                .toList();
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/approve")
    public ResponseEntity<LoanResponseDto> approveLoan(@PathVariable String id) {
        Loan loan = loanUseCase.approveLoan(id);
        return ResponseEntity.ok(LoanResponseDto.fromDomain(loan));
    }

    @PostMapping("/{id}/reject")
    public ResponseEntity<LoanResponseDto> rejectLoan(@PathVariable String id) {
        Loan loan = loanUseCase.rejectLoan(id);
        return ResponseEntity.ok(LoanResponseDto.fromDomain(loan));
    }

    @PostMapping("/{id}/payments")
    public ResponseEntity<LoanResponseDto> recordPayment(@PathVariable String id, @RequestParam BigDecimal amount) {
        Loan loan = loanUseCase.recordPayment(id, Money.sek(amount));
        return ResponseEntity.ok(LoanResponseDto.fromDomain(loan));
    }

    public record LoanApplicationRequestDto(
            @NotBlank(message = "Borrower account id cannot be blank")
            String borrowerAccountId,

            @NotNull(message = "Principal cannot be null")
            @Positive(message = "Principal must be strictly positive")
            BigDecimal principal,

            @Positive(message = "Term must be at least 1 month")
            int termInMonths
    ) {}

    public record LoanResponseDto(
            String id,
            String borrowerAccountId,
            BigDecimal principal,
            BigDecimal accruedPenalty,
            String currency,
            BigDecimal annualInterestRate,
            int termInMonths,
            String status,
            Instant createdAt,
            List<InstallmentDto> installments
    ) {
        public static LoanResponseDto fromDomain(Loan loan) {
            List<InstallmentDto> installments = loan.getInstallments().stream()
                    .map(InstallmentDto::fromDomain)
                    .toList();

            return new LoanResponseDto(
                    loan.getId().value(),
                    loan.getBorrowerAccountId().value(),
                    loan.getPrincipal().amount(),
                    loan.getAccruedPenalty().amount(),
                    loan.getPrincipal().currency().getCurrencyCode(),
                    loan.getAnnualInterestRate(),
                    loan.getTermInMonths(),
                    loan.getStatus().name(),
                    loan.getCreatedAt(),
                    installments
            );
        }

        public record InstallmentDto(int number, BigDecimal amount, Instant dueDate, String status) {
            public static InstallmentDto fromDomain(Installment installment) {
                return new InstallmentDto(
                        installment.getNumber(),
                        installment.getAmount().amount(),
                        installment.getDueDate(),
                        installment.getStatus().name()
                );
            }
        }
    }
}