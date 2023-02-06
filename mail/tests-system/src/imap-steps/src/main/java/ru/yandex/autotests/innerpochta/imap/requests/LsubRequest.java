package ru.yandex.autotests.innerpochta.imap.requests;

import ru.yandex.autotests.innerpochta.imap.consts.base.ImapCmd;
import ru.yandex.autotests.innerpochta.imap.responses.LsubResponse;

public class LsubRequest implements ImapRequestBuilder<LsubResponse> {
    private final String referenceName;
    private final String folderName;

    private LsubRequest(String referenceName, String folderName) {
        this.referenceName = referenceName;
        this.folderName = folderName;
    }

    public static LsubRequest lsub(String oldFolderName, String newFolderName) {
        return new LsubRequest(oldFolderName, newFolderName);
    }

    @Override
    public ImapRequest<LsubResponse> build(String tag) {
        return new ImapRequest(LsubResponse.class, tag).add(ImapCmd.LSUB, referenceName, folderName);
    }
}
