package banking_system.loan.domain.exception;

public class InvalidLoanStateTransitionException extends RuntimeException {
    public InvalidLoanStateTransitionException(String message) {
        super(message);
    }
}