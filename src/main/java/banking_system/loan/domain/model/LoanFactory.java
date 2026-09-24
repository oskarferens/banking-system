package banking_system.loan.domain.model;

import banking_system.account.domain.model.AccountId;
import banking_system.loan.domain.model.state.PendingApprovalState;
import banking_system.shared.domain.model.Money;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

public class LoanFactory {

    static final BigDecimal DEFAULT_ANNUAL_INTEREST_RATE = new BigDecimal("0.0500"); // 5% p.a., bank-set, not client-chosen
    private static final int DAYS_PER_INSTALLMENT = 30;

    public static Loan originate(AccountId borrowerAccountId, Money principal, int termInMonths, Instant originationDate) {
        return originate(borrowerAccountId, principal, DEFAULT_ANNUAL_INTEREST_RATE, termInMonths, originationDate);
    }

    // Package private lets domain tests exercise the schedule algorithm at arbitrary rates.
    // 0% so state transition tests get round numbers without exposing rate selection.
    // outside the domain application code can only reach the public overload above and it always applies the bank's own rate.
    static Loan originate(AccountId borrowerAccountId, Money principal, BigDecimal annualInterestRate,
                          int termInMonths, Instant originationDate) {

        List<Installment> installments = buildFlatRateSchedule(principal, annualInterestRate, termInMonths, originationDate);

        return new Loan(
                LoanId.generate(),
                borrowerAccountId,
                principal,
                annualInterestRate,
                termInMonths,
                originationDate,
                installments,
                PendingApprovalState.INSTANCE
        );
    }

    private static List<Installment> buildFlatRateSchedule(Money principal, BigDecimal annualInterestRate,
                                                           int termInMonths, Instant originationDate) {

        BigDecimal totalInterest = principal.amount()
                .multiply(annualInterestRate)
                .multiply(BigDecimal.valueOf(termInMonths))
                .divide(BigDecimal.valueOf(12), 10, RoundingMode.HALF_UP);

        BigDecimal totalRepayable = principal.amount().add(totalInterest);
        BigDecimal baseInstallmentAmount = totalRepayable.divide(BigDecimal.valueOf(termInMonths), 2, RoundingMode.HALF_UP);

        List<Installment> installments = new ArrayList<>();
        BigDecimal runningTotal = BigDecimal.ZERO;

        for (int i = 1; i <= termInMonths; i++) {
            BigDecimal installmentAmount;
            if (i < termInMonths) {
                installmentAmount = baseInstallmentAmount;
                runningTotal = runningTotal.add(installmentAmount);
            } else {
                installmentAmount = totalRepayable.subtract(runningTotal);
            }

            Instant dueDate = originationDate.plus((long) DAYS_PER_INSTALLMENT * i, ChronoUnit.DAYS);
            installments.add(new Installment(i, new Money(installmentAmount, principal.currency()), dueDate, InstallmentStatus.PENDING));
        }

        return installments;
    }
}