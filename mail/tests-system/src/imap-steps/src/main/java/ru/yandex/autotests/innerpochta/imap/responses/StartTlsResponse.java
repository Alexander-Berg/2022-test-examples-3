package ru.yandex.autotests.innerpochta.imap.responses;

/**
 * Created with IntelliJ IDEA.
 * User: vicdev
 * Date: 26.09.14
 * Time: 20:09
 */
public class StartTlsResponse extends ImapResponse<GenericResponse> {
    //MAILPROTO-2360
    public static final String CLIENT_BUG_STARTTLS_ACTIVE = "STARTTLS SSL/TLS already active";

    @Override
    protected void parse(String line) {
    }

    @Override
    protected void validate() {
    }
}

