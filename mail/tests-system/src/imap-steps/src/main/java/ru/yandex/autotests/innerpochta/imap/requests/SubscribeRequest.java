package ru.yandex.autotests.innerpochta.imap.requests;

import ru.yandex.autotests.innerpochta.imap.consts.base.ImapCmd;
import ru.yandex.autotests.innerpochta.imap.responses.GenericResponse;

public class SubscribeRequest implements ImapRequestBuilder<GenericResponse> {
    private final String folderName;

    private SubscribeRequest(String folderName) {
        this.folderName = folderName;
    }

    public static SubscribeRequest subscribe(String folderName) {
        return new SubscribeRequest(folderName);
    }

    @Override
    public ImapRequest<GenericResponse> build(String tag) {
        return new ImapRequest(GenericResponse.class, tag).add(ImapCmd.SUBSCRIBE, folderName);
    }
}
