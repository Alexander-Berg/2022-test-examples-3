package toolkit.exceptions;


public class BreakRetryError extends AssertionError {
    public BreakRetryError(Throwable cause) {
        super(cause);
    }
}
