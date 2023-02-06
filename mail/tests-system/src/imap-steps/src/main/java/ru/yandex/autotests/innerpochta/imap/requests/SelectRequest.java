package ru.yandex.autotests.innerpochta.imap.requests;

import ru.yandex.autotests.innerpochta.imap.consts.base.ImapCmd;
import ru.yandex.autotests.innerpochta.imap.responses.SelectResponse;

public class SelectRequest implements ImapRequestBuilder<SelectResponse> {
    private final String folderName;

    private SelectRequest(String folderName) {
        this.folderName = folderName;
    }

    public static SelectRequest select(String folderName) {
        return new SelectRequest(folderName);
    }

    @Override
    public ImapRequest<SelectResponse> build(String tag) {
        return new ImapRequest(SelectResponse.class, tag).add(ImapCmd.SELECT, folderName);
    }
}
