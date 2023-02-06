package ru.yandex.autotests.innerpochta.imap.requests;

import ru.yandex.autotests.innerpochta.imap.consts.base.ImapCmd;
import ru.yandex.autotests.innerpochta.imap.responses.GenericResponse;

public class AuthenticateRequest implements ImapRequestBuilder<GenericResponse> {

    public static final String XOAUTH = "XOAUTH";
    public static final String XOAUTH2 = "XOAUTH2";

    private final String mechanismName;
    private final String token;


    private AuthenticateRequest(String mechanismName, String token) {
        this.mechanismName = mechanismName;
        this.token = token;
    }

    public static AuthenticateRequest authenticate(String mechanismName) {
        return new AuthenticateRequest(mechanismName, "");
    }

    public static AuthenticateRequest authenticate(String mechanismName, String token) {
        return new AuthenticateRequest(mechanismName, token);
    }

    @Override
    public ImapRequest<GenericResponse> build(String tag) {
        return new ImapRequest(GenericResponse.class, tag).add(ImapCmd.AUTHENTICATE, mechanismName, token);
    }
}
