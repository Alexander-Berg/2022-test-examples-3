package ru.yandex.autotests.innerpochta.imap.core.exceptions;

/**
 * Created with IntelliJ IDEA.
 * User: vicdev
 * Date: 16.06.14
 * Time: 19:01
 */
public class RetryException extends RuntimeException {

    public RetryException(Throwable cause) {
        super(cause);
    }

    @Override
    public String getMessage() {
        return "[Retry, exception message: " + getCause().getMessage() + "]";
    }
}
