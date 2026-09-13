package banking_system.transfer.domain.exception;

public class SameAccountTransferException extends RuntimeException {
    public SameAccountTransferException() {
        super("Cannot perform transfer between the same account.");
    }
}