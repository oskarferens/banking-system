package banking_system.account.infrastructure.adapter.in.web;

import banking_system.account.application.port.in.AccountUseCase;
import banking_system.account.domain.model.Account;
import banking_system.shared.domain.model.Money;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/v1/accounts")
public class AccountController {

    private final AccountUseCase accountUseCase;

    public AccountController(AccountUseCase accountUseCase) {
        this.accountUseCase = accountUseCase;
    }

    @PostMapping
    public ResponseEntity<AccountDto> createAccount(@RequestParam String ownerId) {
        Account account = accountUseCase.createAccount(ownerId);
        return ResponseEntity.ok(AccountDto.fromDomain(account));
    }

    @GetMapping("/{id}")
    public ResponseEntity<AccountDto> getAccount(@PathVariable String id) {
        Account account = accountUseCase.getAccount(id);
        return ResponseEntity.ok(AccountDto.fromDomain(account));
    }

    @PostMapping("/{id}/deposit")
    public ResponseEntity<AccountDto> deposit(@PathVariable String id, @RequestParam BigDecimal amount) {
        Account account = accountUseCase.deposit(id, Money.sek(amount));
        return ResponseEntity.ok(AccountDto.fromDomain(account));
    }

    @PostMapping("/{id}/withdraw")
    public ResponseEntity<AccountDto> withdraw(@PathVariable String id, @RequestParam BigDecimal amount) {
        Account account = accountUseCase.withdraw(id, Money.sek(amount));
        return ResponseEntity.ok(AccountDto.fromDomain(account));
    }

    public record AccountDto(String id, String accountNumber, String ownerId, BigDecimal balance, String currency, BigDecimal overdraftLimit) {
        public static AccountDto fromDomain(Account account) {
            return new AccountDto(
                    account.getId().value(),
                    account.getAccountNumber().value(),
                    account.getOwnerId(),
                    account.getBalance().amount(),
                    account.getBalance().currency().getCurrencyCode(),
                    account.getOverdraftLimit().amount()
            );
        }
    }
}
