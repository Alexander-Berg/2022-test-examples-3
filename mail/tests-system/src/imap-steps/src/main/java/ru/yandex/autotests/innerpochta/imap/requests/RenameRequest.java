package ru.yandex.autotests.innerpochta.imap.requests;

import ru.yandex.autotests.innerpochta.imap.consts.base.ImapCmd;
import ru.yandex.autotests.innerpochta.imap.responses.GenericResponse;

public class RenameRequest implements ImapRequestBuilder<GenericResponse> {
    private final String oldFolderName;
    private final String newFolderName;

    private RenameRequest(String oldFolderName, String newFolderName) {
        this.oldFolderName = oldFolderName;
        this.newFolderName = newFolderName;
    }

    public static RenameRequest rename(String oldFolderName, String newFolderName) {
        return new RenameRequest(oldFolderName, newFolderName);
    }

    @Override
    public ImapRequest<GenericResponse> build(String tag) {
        return new ImapRequest(GenericResponse.class, tag).add(ImapCmd.RENAME, oldFolderName, newFolderName);
    }
}
