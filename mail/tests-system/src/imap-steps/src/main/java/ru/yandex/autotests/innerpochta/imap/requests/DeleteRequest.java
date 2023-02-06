package ru.yandex.autotests.innerpochta.imap.requests;

import ru.yandex.autotests.innerpochta.imap.consts.base.ImapCmd;
import ru.yandex.autotests.innerpochta.imap.responses.GenericResponse;

public class DeleteRequest implements ImapRequestBuilder<GenericResponse> {
    private final String folderName;

    private DeleteRequest(String folderName) {
        this.folderName = folderName;
    }

    public static DeleteRequest delete(String folderName) {
        return new DeleteRequest(folderName);
    }

    @Override
    public ImapRequest<GenericResponse> build(String tag) {
        return new ImapRequest(GenericResponse.class, tag).add(ImapCmd.DELETE, folderName);
    }
}
