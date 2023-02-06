package ru.yandex.autotests.innerpochta.imap.requests;

import ru.yandex.autotests.innerpochta.imap.consts.base.ImapCmd;
import ru.yandex.autotests.innerpochta.imap.responses.GenericResponse;

public class CreateRequest implements ImapRequestBuilder<GenericResponse> {
    private final String folderName;

    private CreateRequest(String folderName) {
        this.folderName = folderName;
    }

    public static CreateRequest create(String folderName) {
        return new CreateRequest(folderName);
    }

    @Override
    public ImapRequest<GenericResponse> build(String tag) {
        return new ImapRequest(GenericResponse.class, tag).add(ImapCmd.CREATE, folderName);
    }
}
