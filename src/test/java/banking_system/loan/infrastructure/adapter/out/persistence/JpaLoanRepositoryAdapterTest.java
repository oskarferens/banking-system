package banking_system.loan.infrastructure.adapter.out.persistence;

import banking_system.account.domain.model.Account;
import banking_system.account.domain.model.AccountFactory;
import banking_system.account.domain.port.AccountRepositoryPort;
import banking_system.loan.domain.model.Loan;
import banking_system.loan.domain.model.LoanFactory;
import banking_system.loan.domain.model.LoanId;
import banking_system.loan.domain.model.LoanStatus;
import banking_system.loan.domain.port.LoanRepositoryPort;
import banking_system.shared.domain.model.Money;
import banking_system.testsupport.MySQLTestContainer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class JpaLoanRepositoryAdapterTest {

    @DynamicPropertySource
    static void registerMySQLProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", MySQLTestContainer.INSTANCE::getJdbcUrl);
        registry.add("spring.datasource.username", MySQLTestContainer.INSTANCE::getUsername);
        registry.add("spring.datasource.password", MySQLTestContainer.INSTANCE::getPassword);
    }

    @Autowired
    private LoanRepositoryPort loanRepository;

    @Autowired
    private AccountRepositoryPort accountRepository;

    private Account borrowerAccount;

    @BeforeEach
    void setUp() {
        borrowerAccount = accountRepository.save(AccountFactory.createStandardAccount("integration-test-owner"));
    }

    @Test
    @DisplayName("saves a loan with several installments and reads it back unchanged")
    void savesAndReloadsLoanWithInstallments() {
        Loan loan = LoanFactory.originate(
                borrowerAccount.getId(), Money.sek("300.00"), 3, Instant.parse("2026-01-01T00:00:00Z"));

        Loan saved = loanRepository.save(loan);
        Optional<Loan> reloaded = loanRepository.findById(saved.getId());

        assertThat(reloaded).isPresent();
        assertThat(reloaded.get().getInstallments()).hasSize(3);
        assertThat(reloaded.get().getPrincipal().amount()).isEqualByComparingTo(new BigDecimal("300.00"));
        assertThat(reloaded.get().getStatus()).isEqualTo(LoanStatus.PENDING_APPROVAL);
    }

    @Test
    @DisplayName("preserves installment order and amounts across a save/reload round trip")
    void preservesInstallmentOrderAndAmounts() {
        Loan loan = LoanFactory.originate(
                borrowerAccount.getId(), Money.sek("1000.00"), 3, Instant.parse("2026-01-01T00:00:00Z"));

        Loan saved = loanRepository.save(loan);
        Loan reloaded = loanRepository.findById(saved.getId()).orElseThrow();

        assertThat(reloaded.getInstallments().get(0).getNumber()).isEqualTo(1);
        assertThat(reloaded.getInstallments().get(1).getNumber()).isEqualTo(2);
        assertThat(reloaded.getInstallments().get(2).getNumber()).isEqualTo(3);
        assertThat(reloaded.getInstallments().get(0).getAmount().amount())
                .isEqualByComparingTo(reloaded.getInstallments().get(1).getAmount().amount());
    }

    @Test
    @DisplayName("a loan's status round-trips correctly through the database, restoring the matching LoanState")
    void loanStatusRoundTripsToMatchingState() {
        Loan loan = LoanFactory.originate(
                borrowerAccount.getId(), Money.sek("300.00"), 3, Instant.parse("2026-01-01T00:00:00Z"));
        loan.approve();

        Loan saved = loanRepository.save(loan);
        Loan reloaded = loanRepository.findById(saved.getId()).orElseThrow();

        assertThat(reloaded.getStatus()).isEqualTo(LoanStatus.ACTIVE);
        // Confirms that this is not just a stored label - the reloaded loan actually
        // accepts a payment (which is only allowed in ACTIVE), rather than merely appearing as ACTIVE.
        reloaded.recordPayment(reloaded.getInstallments().get(0).getAmount());
        assertThat(reloaded.getInstallments().get(0).isSettled()).isTrue();
    }

    @Test
    @DisplayName("re-saving a loan after recording a payment updates the existing installment row instead of duplicating it")
    void resavingAfterPaymentUpdatesExistingInstallmentRow() {
        Loan loan = LoanFactory.originate(
                borrowerAccount.getId(), Money.sek("300.00"), 3, Instant.parse("2026-01-01T00:00:00Z"));
        loan.approve();
        Loan saved = loanRepository.save(loan);

        Loan reloaded = loanRepository.findById(saved.getId()).orElseThrow();
        reloaded.recordPayment(reloaded.getInstallments().get(0).getAmount());
        loanRepository.save(reloaded);

        Loan reloadedAgain = loanRepository.findById(saved.getId()).orElseThrow();

        assertThat(reloadedAgain.getInstallments()).hasSize(3);
        assertThat(reloadedAgain.getInstallments().get(0).isSettled()).isTrue();
        assertThat(reloadedAgain.getInstallments().get(1).isSettled()).isFalse();
    }

    @Test
    @DisplayName("findAllByBorrowerAccountId returns every loan for that account and none for another")
    void findAllByBorrowerAccountIdFiltersCorrectly() {
        Account otherAccount = accountRepository.save(AccountFactory.createStandardAccount("other-owner"));

        loanRepository.save(LoanFactory.originate(
                borrowerAccount.getId(), Money.sek("300.00"), 3, Instant.parse("2026-01-01T00:00:00Z")));
        loanRepository.save(LoanFactory.originate(
                borrowerAccount.getId(), Money.sek("500.00"), 6, Instant.parse("2026-01-01T00:00:00Z")));
        loanRepository.save(LoanFactory.originate(
                otherAccount.getId(), Money.sek("100.00"), 1, Instant.parse("2026-01-01T00:00:00Z")));

        List<Loan> loansForBorrower = loanRepository.findAllByBorrowerAccountId(borrowerAccount.getId().value());

        assertThat(loansForBorrower).hasSize(2);
    }

    @Test
    @DisplayName("findById returns empty for an id that does not exist")
    void findByIdReturnsEmptyForUnknownId() {
        Optional<Loan> result = loanRepository.findById(LoanId.generate());

        assertThat(result).isEmpty();
    }
}