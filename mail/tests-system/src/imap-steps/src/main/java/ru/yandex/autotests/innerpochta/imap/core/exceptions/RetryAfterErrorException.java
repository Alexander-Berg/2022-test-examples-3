package ru.yandex.autotests.innerpochta.imap.core.exceptions;

/**
 * Created with IntelliJ IDEA.
 * User: vicdev
 * Date: 16.06.14
 * Time: 19:01
 */
public class RetryAfterErrorException extends AssertionError {

    public RetryAfterErrorException(AssertionError cause) {
        super(cause);
    }

    @Override
    public String getMessage() {
        return "Попытка ретрая для ошибки не увенчалась успехом:\n" + getCause().getMessage();
    }
}
