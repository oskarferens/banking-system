package banking_system.loan.infrastructure.adapter.out.scoring;

import banking_system.account.domain.model.Account;
import banking_system.loan.domain.port.CreditScoringPolicy;
import banking_system.shared.domain.model.Money;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class BalanceMultipleScoringPolicy implements CreditScoringPolicy {

    private static final BigDecimal MAX_PRINCIPAL_TO_BALANCE_MULTIPLE = new BigDecimal("3");

    @Override
    public boolean isEligible(Account borrowerAccount, Money requestedPrincipal) {
        Money currentBalance = borrowerAccount.getBalance();

        if (currentBalance.isNegative() || currentBalance.amount().signum() == 0) {
            return false;
        }

        BigDecimal maxEligiblePrincipal = currentBalance.amount().multiply(MAX_PRINCIPAL_TO_BALANCE_MULTIPLE);
        return requestedPrincipal.amount().compareTo(maxEligiblePrincipal) <= 0;
    }
}