package ru.yandex.autotests.innerpochta.imap.requests;

import ru.yandex.autotests.innerpochta.imap.consts.base.ImapCmd;
import ru.yandex.autotests.innerpochta.imap.responses.GenericResponse;

public class StartTlsRequest implements ImapRequestBuilder<GenericResponse> {
    private StartTlsRequest() {
    }

    public static StartTlsRequest startTls() {
        return new StartTlsRequest();
    }

    @Override
    public ImapRequest<GenericResponse> build(String tag) {
        return new ImapRequest(GenericResponse.class, tag).add(ImapCmd.STARTTLS);
    }
}
