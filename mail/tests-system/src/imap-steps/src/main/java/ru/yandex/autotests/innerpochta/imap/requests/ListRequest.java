package ru.yandex.autotests.innerpochta.imap.requests;

import ru.yandex.autotests.innerpochta.imap.consts.base.ImapCmd;
import ru.yandex.autotests.innerpochta.imap.responses.ListResponse;

public class ListRequest implements ImapRequestBuilder<ListResponse> {
    private final String referenceName;
    private final String folderName;

    private ListRequest(String referenceName, String folderName) {
        this.referenceName = referenceName;
        this.folderName = folderName;
    }

    public static ListRequest list(String oldFolderName, String newFolderName) {
        return new ListRequest(oldFolderName, newFolderName);
    }

    @Override
    public ImapRequest<ListResponse> build(String tag) {
        return new ImapRequest(ListResponse.class, tag).add(ImapCmd.LIST, referenceName, folderName);
    }
}
