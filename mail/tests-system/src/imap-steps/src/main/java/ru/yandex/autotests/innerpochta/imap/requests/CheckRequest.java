package ru.yandex.autotests.innerpochta.imap.requests;

import ru.yandex.autotests.innerpochta.imap.consts.base.ImapCmd;
import ru.yandex.autotests.innerpochta.imap.responses.GenericResponse;

public class CheckRequest implements ImapRequestBuilder<GenericResponse> {
    private CheckRequest() {
    }

    public static CheckRequest check() {
        return new CheckRequest();
    }

    @Override
    public ImapRequest<GenericResponse> build(String tag) {
        return new ImapRequest(GenericResponse.class, tag).add(ImapCmd.CHECK);
    }
}
