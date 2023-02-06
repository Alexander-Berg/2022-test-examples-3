package ru.yandex.autotests.innerpochta.imap.responses;

/**
 * Created with IntelliJ IDEA.
 * User: vicdev
 * Date: 05.09.14
 * Time: 20:43
 */
public class AuthenticateResponse extends ImapResponse<GenericResponse> {

    public static final String AUTHENTICATION_FAILED = "[AUTHENTICATIONFAILED] AUTHENTICATE failure";
    public static final String AUTHENTICATE_WRONG_STATE = "[CLIENTBUG] AUTHENTICATE wrong state for this command";
    public static final String AUTHENTICATE_SYNTAX_ERROR = "AUTHENTICATE Command syntax error.";
    public static final String AUTHENTICATE_INVALID_CREDENTAILS = "[AUTHENTICATIONFAILED] AUTHENTICATE invalid credentials or IMAP is disabled";

    @Override
    protected void parse(String line) {
    }

    @Override
    protected void validate() {
    }
}
