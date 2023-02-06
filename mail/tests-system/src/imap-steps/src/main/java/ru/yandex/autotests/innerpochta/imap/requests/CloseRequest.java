package ru.yandex.autotests.innerpochta.imap.requests;


import ru.yandex.autotests.innerpochta.imap.consts.base.ImapCmd;
import ru.yandex.autotests.innerpochta.imap.responses.GenericResponse;

public class CloseRequest implements ImapRequestBuilder<GenericResponse> {
    private CloseRequest() {
    }

    public static CloseRequest close() {
        return new CloseRequest();
    }

    @Override
    public ImapRequest<GenericResponse> build(String tag) {
        return new ImapRequest(GenericResponse.class, tag).add(ImapCmd.CLOSE);
    }
}
