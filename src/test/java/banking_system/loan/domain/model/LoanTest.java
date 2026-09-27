package banking_system.loan.domain.model;

import banking_system.account.domain.model.AccountId;
import banking_system.loan.domain.exception.InvalidLoanStateTransitionException;
import banking_system.shared.domain.model.Money;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LoanTest {

    private Loan loan;
    private Instant originationDate;

    @BeforeEach
    void setUp() {
        originationDate = Instant.parse("2026-01-01T00:00:00Z");
        // 300.00 sek, 0% interest, 3/100.00 simple math easy to verify.
        loan = LoanFactory.originate(AccountId.generate(), Money.sek("300.00"), BigDecimal.ZERO, 3, originationDate);
    }

    @Test
    @DisplayName("approve() moves a pending loan to ACTIVE")
    void approveMovesToActive() {
        loan.approve();

        assertThat(loan.getStatus()).isEqualTo(LoanStatus.ACTIVE);
    }

    @Test
    @DisplayName("reject() moves a pending loan to REJECTED")
    void rejectMovesToRejected() {
        loan.reject();

        assertThat(loan.getStatus()).isEqualTo(LoanStatus.REJECTED);
    }

    @Test
    @DisplayName("approve() cannot be called twice")
    void cannotApproveTwice() {
        loan.approve();

        assertThatThrownBy(() -> loan.approve())
                .isInstanceOf(InvalidLoanStateTransitionException.class);
    }

    @Test
    @DisplayName("a rejected loan cannot receive payments")
    void rejectedLoanCannotReceivePayments() {
        loan.reject();

        assertThatThrownBy(() -> loan.recordPayment(Money.sek("100.00")))
                .isInstanceOf(InvalidLoanStateTransitionException.class);
    }

    @Test
    @DisplayName("recordPayment() settles the next due installment and stays ACTIVE if others remain")
    void recordPaymentSettlesNextInstallment() {
        loan.approve();

        loan.recordPayment(Money.sek("100.00"));

        assertThat(loan.getStatus()).isEqualTo(LoanStatus.ACTIVE);
        assertThat(loan.getInstallments().get(0).getStatus()).isEqualTo(InstallmentStatus.PAID);
        assertThat(loan.getInstallments().get(1).getStatus()).isEqualTo(InstallmentStatus.PENDING);
    }

    @Test
    @DisplayName("recordPayment() rejects an amount that does not match the due installment")
    void recordPaymentRejectsWrongAmount() {
        loan.approve();

        assertThatThrownBy(() -> loan.recordPayment(Money.sek("50.00")))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("paying off the final installment moves the loan to PAID_OFF")
    void finalPaymentMovesToPaidOff() {
        loan.approve();

        loan.recordPayment(Money.sek("100.00"));
        loan.recordPayment(Money.sek("100.00"));
        loan.recordPayment(Money.sek("100.00"));

        assertThat(loan.getStatus()).isEqualTo(LoanStatus.PAID_OFF);
        assertThat(loan.getInstallments()).allMatch(Installment::isSettled);
    }

    @Test
    @DisplayName("a paid-off loan cannot receive further payments")
    void paidOffLoanRejectsFurtherPayments() {
        loan.approve();
        loan.recordPayment(Money.sek("100.00"));
        loan.recordPayment(Money.sek("100.00"));
        loan.recordPayment(Money.sek("100.00"));

        assertThatThrownBy(() -> loan.recordPayment(Money.sek("100.00")))
                .isInstanceOf(InvalidLoanStateTransitionException.class);
    }

    @Test
    @DisplayName("markOverdue() moves an active loan to OVERDUE")
    void markOverdueMovesToOverdue() {
        loan.approve();

        loan.markOverdue();

        assertThat(loan.getStatus()).isEqualTo(LoanStatus.OVERDUE);
    }

    @Test
    @DisplayName("a payment on an overdue loan cures it back to ACTIVE")
    void paymentOnOverdueLoanCuresToActive() {
        loan.approve();
        loan.markOverdue();

        loan.recordPayment(Money.sek("100.00"));

        assertThat(loan.getStatus()).isEqualTo(LoanStatus.ACTIVE);
    }

    @Test
    @DisplayName("markDefaulted() moves an overdue loan to DEFAULTED")
    void markDefaultedMovesToDefaulted() {
        loan.approve();
        loan.markOverdue();

        loan.markDefaulted();

        assertThat(loan.getStatus()).isEqualTo(LoanStatus.DEFAULTED);
    }

    @Test
    @DisplayName("a defaulted loan cannot receive payments")
    void defaultedLoanRejectsPayments() {
        loan.approve();
        loan.markOverdue();
        loan.markDefaulted();

        assertThatThrownBy(() -> loan.recordPayment(Money.sek("100.00")))
                .isInstanceOf(InvalidLoanStateTransitionException.class);
    }

    @Test
    @DisplayName("markOverdue() cannot be called on a pending loan")
    void cannotMarkOverduePendingLoan() {
        assertThatThrownBy(() -> loan.markOverdue())
                .isInstanceOf(InvalidLoanStateTransitionException.class);
    }

    @Test
    @DisplayName("processing a day before the first due date leaves an active loan untouched")
    void endOfDayBeforeDueDateDoesNothing() {
        loan.approve();

        loan.applyEndOfDayProcessing(originationDate.plus(10, ChronoUnit.DAYS));

        assertThat(loan.getStatus()).isEqualTo(LoanStatus.ACTIVE);
        assertThat(loan.getInstallments().get(0).getStatus()).isEqualTo(InstallmentStatus.PENDING);
    }

    @Test
    @DisplayName("processing a day after the first due date marks that installment overdue and moves the loan to OVERDUE")
    void endOfDayAfterDueDateMarksOverdue() {
        loan.approve();

        loan.applyEndOfDayProcessing(originationDate.plus(31, ChronoUnit.DAYS));

        assertThat(loan.getStatus()).isEqualTo(LoanStatus.OVERDUE);
        assertThat(loan.getInstallments().get(0).getStatus()).isEqualTo(InstallmentStatus.OVERDUE);
    }

    @Test
    @DisplayName("penalty accrues for each additional day an installment stays overdue")
    void overduePenaltyAccruesPerDay() {
        loan.approve();
        loan.applyEndOfDayProcessing(originationDate.plus(31, ChronoUnit.DAYS)); // ACTIVE -> OVERDUE; brak kary jeszcze na tym wywołaniu
        assertThat(loan.getAccruedPenalty().amount()).isEqualByComparingTo(BigDecimal.ZERO);

        loan.applyEndOfDayProcessing(originationDate.plus(32, ChronoUnit.DAYS)); // pełny dzień zaległości -> pierwsza kara
        Money penaltyAfterOneDay = loan.getAccruedPenalty();
        assertThat(penaltyAfterOneDay.amount()).isGreaterThan(BigDecimal.ZERO);

        loan.applyEndOfDayProcessing(originationDate.plus(33, ChronoUnit.DAYS)); // drugi dzień zaległości -> kara rośnie dalej
        assertThat(loan.getAccruedPenalty().amount()).isGreaterThan(penaltyAfterOneDay.amount());
    }

    @Test
    @DisplayName("paying the exact original installment amount still settles it even while penalty has accrued")
    void paymentSettlesInstallmentDespiteAccruedPenalty() {
        loan.approve();
        loan.applyEndOfDayProcessing(originationDate.plus(32, ChronoUnit.DAYS)); // OVERDUE, jeden dzień kary naliczony

        loan.recordPayment(Money.sek("100.00"));

        assertThat(loan.getInstallments().get(0).isSettled()).isTrue();
        assertThat(loan.getStatus()).isEqualTo(LoanStatus.ACTIVE);
    }

    @Test
    @DisplayName("staying overdue beyond the default threshold moves the loan to DEFAULTED")
    void exceedingDefaultThresholdMovesToDefaulted() {
        loan.approve();
        loan.applyEndOfDayProcessing(originationDate.plus(31, ChronoUnit.DAYS)); // ACTIVE -> OVERDUE

        loan.applyEndOfDayProcessing(originationDate.plus(47, ChronoUnit.DAYS)); // 17 dni zaległości, powyżej progu 15 dni

        assertThat(loan.getStatus()).isEqualTo(LoanStatus.DEFAULTED);
    }

    @Test
    @DisplayName("processEndOfDay on a pending loan is a no-op")
    void endOfDayOnPendingLoanIsNoOp() {
        loan.applyEndOfDayProcessing(originationDate.plus(100, ChronoUnit.DAYS));

        assertThat(loan.getStatus()).isEqualTo(LoanStatus.PENDING_APPROVAL);
        assertThat(loan.getInstallments()).allMatch(installment -> installment.getStatus() == InstallmentStatus.PENDING);
    }

    @Test
    @DisplayName("processEndOfDay on a paid-off loan is a no-op")
    void endOfDayOnPaidOffLoanIsNoOp() {
        loan.approve();
        loan.recordPayment(Money.sek("100.00"));
        loan.recordPayment(Money.sek("100.00"));
        loan.recordPayment(Money.sek("100.00"));

        loan.applyEndOfDayProcessing(originationDate.plus(200, ChronoUnit.DAYS));

        assertThat(loan.getStatus()).isEqualTo(LoanStatus.PAID_OFF);
        assertThat(loan.getAccruedPenalty().amount()).isEqualByComparingTo(BigDecimal.ZERO);
    }
}