package banking_system.loan.domain.exception;

public class LoanApplicationRejectedException extends RuntimeException {
    public LoanApplicationRejectedException(String message) {
        super(message);
    }
}