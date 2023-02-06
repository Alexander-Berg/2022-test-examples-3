package ru.yandex.autotests.innerpochta.imap.requests;

import ru.yandex.autotests.innerpochta.imap.consts.base.ImapCmd;
import ru.yandex.autotests.innerpochta.imap.responses.GenericResponse;

public class UnsubscribeRequest implements ImapRequestBuilder<GenericResponse> {
    private final String folderName;

    private UnsubscribeRequest(String folderName) {
        this.folderName = folderName;
    }

    public static UnsubscribeRequest unsubscribe(String folderName) {
        return new UnsubscribeRequest(folderName);
    }

    @Override
    public ImapRequest<GenericResponse> build(String tag) {
        return new ImapRequest(GenericResponse.class, tag).add(ImapCmd.UNSUBSCRIBE, folderName);
    }
}
