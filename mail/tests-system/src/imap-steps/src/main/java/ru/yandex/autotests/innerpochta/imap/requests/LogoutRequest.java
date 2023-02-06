package ru.yandex.autotests.innerpochta.imap.requests;

import ru.yandex.autotests.innerpochta.imap.consts.base.ImapCmd;
import ru.yandex.autotests.innerpochta.imap.responses.LogoutResponse;

public class LogoutRequest implements ImapRequestBuilder<LogoutResponse> {
    private LogoutRequest() {
    }

    public static LogoutRequest logout() {
        return new LogoutRequest();
    }

    @Override
    public ImapRequest<LogoutResponse> build(String tag) {
        return new ImapRequest(LogoutResponse.class, tag).add(ImapCmd.LOGOUT);
    }
}
