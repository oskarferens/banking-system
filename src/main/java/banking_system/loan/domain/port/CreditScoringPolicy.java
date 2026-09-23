package banking_system.loan.domain.port;

import banking_system.account.domain.model.Account;
import banking_system.shared.domain.model.Money;

public interface CreditScoringPolicy {
    boolean isEligible(Account borrowerAccount, Money requestedPrincipal);
}