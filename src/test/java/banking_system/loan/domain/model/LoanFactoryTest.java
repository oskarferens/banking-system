package banking_system.loan.domain.model;

import banking_system.account.domain.model.AccountId;
import banking_system.shared.domain.model.Money;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;

class LoanFactoryTest {

    private final AccountId borrowerAccountId = AccountId.generate();
    private final Instant originationDate = Instant.parse("2026-01-01T00:00:00Z");

    @Test
    @DisplayName("originate() creates a loan with the requested number of installments")
    void createsCorrectNumberOfInstallments() {
        Loan loan = LoanFactory.originate(borrowerAccountId, Money.sek("1200.00"), new BigDecimal("0.10"), 12, originationDate);

        assertThat(loan.getInstallments()).hasSize(12);
    }

    @Test
    @DisplayName("originate() starts the loan in PENDING_APPROVAL")
    void startsInPendingApproval() {
        Loan loan = LoanFactory.originate(borrowerAccountId, Money.sek("1200.00"), new BigDecimal("0.10"), 12, originationDate);

        assertThat(loan.getStatus()).isEqualTo(LoanStatus.PENDING_APPROVAL);
    }

    @Test
    @DisplayName("flat-rate schedule sums exactly to principal plus total interest")
    void scheduleSumsToPrincipalPlusInterest() {
        Loan loan = LoanFactory.originate(borrowerAccountId, Money.sek("1200.00"), new BigDecimal("0.10"), 12, originationDate);

        BigDecimal sumOfInstallments = loan.getInstallments().stream()
                .map(installment -> installment.getAmount().amount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // principal 1200.00 + (1200.00 * 10% * 12 months / 12) = 1320.00
        assertThat(sumOfInstallments).isEqualByComparingTo(new BigDecimal("1320.00"));
    }

    @Test
    @DisplayName("rounding remainder across installments is absorbed by the last installment")
    void lastInstallmentAbsorbsRoundingRemainder() {
        // 1000.00 / 3 does not divide evenly. Forces a rounding remainder.
        Loan loan = LoanFactory.originate(borrowerAccountId, Money.sek("1000.00"), BigDecimal.ZERO, 3, originationDate);

        BigDecimal sumOfInstallments = loan.getInstallments().stream()
                .map(installment -> installment.getAmount().amount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        assertThat(sumOfInstallments).isEqualByComparingTo(new BigDecimal("1000.00"));
        assertThat(loan.getInstallments().get(0).getAmount().amount()).isEqualByComparingTo(new BigDecimal("333.33"));
        assertThat(loan.getInstallments().get(2).getAmount().amount()).isEqualByComparingTo(new BigDecimal("333.34"));
    }

    @Test
    @DisplayName("installments are due 30 days apart, starting 30 days after origination")
    void installmentsAreSpacedThirtyDaysApart() {
        Loan loan = LoanFactory.originate(borrowerAccountId, Money.sek("300.00"), BigDecimal.ZERO, 3, originationDate);

        assertThat(loan.getInstallments().get(0).getDueDate()).isEqualTo(originationDate.plus(30, ChronoUnit.DAYS));
        assertThat(loan.getInstallments().get(1).getDueDate()).isEqualTo(originationDate.plus(60, ChronoUnit.DAYS));
        assertThat(loan.getInstallments().get(2).getDueDate()).isEqualTo(originationDate.plus(90, ChronoUnit.DAYS));
    }

    @Test
    @DisplayName("the public originate() overload applies the bank's default interest rate")
    void publicOverloadAppliesDefaultRate() {
        Loan viaDefaultRate = LoanFactory.originate(borrowerAccountId, Money.sek("1200.00"), 12, originationDate);
        Loan viaExplicitRate = LoanFactory.originate(borrowerAccountId, Money.sek("1200.00"), LoanFactory.DEFAULT_ANNUAL_INTEREST_RATE, 12, originationDate);

        BigDecimal sumViaDefault = viaDefaultRate.getInstallments().stream()
                .map(installment -> installment.getAmount().amount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal sumViaExplicit = viaExplicitRate.getInstallments().stream()
                .map(installment -> installment.getAmount().amount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        assertThat(sumViaDefault).isEqualByComparingTo(sumViaExplicit);
        assertThat(viaDefaultRate.getAnnualInterestRate()).isEqualByComparingTo(LoanFactory.DEFAULT_ANNUAL_INTEREST_RATE);
    }

}