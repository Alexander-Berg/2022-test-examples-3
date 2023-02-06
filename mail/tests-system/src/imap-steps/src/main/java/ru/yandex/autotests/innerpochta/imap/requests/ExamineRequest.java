package ru.yandex.autotests.innerpochta.imap.requests;

import ru.yandex.autotests.innerpochta.imap.consts.base.ImapCmd;
import ru.yandex.autotests.innerpochta.imap.responses.ExamineResponse;

public class ExamineRequest implements ImapRequestBuilder<ExamineResponse> {
    private final String folderName;

    private ExamineRequest(String folderName) {
        this.folderName = folderName;
    }

    public static ExamineRequest examine(String folderName) {
        return new ExamineRequest(folderName);
    }

    @Override
    public ImapRequest<ExamineResponse> build(String tag) {
        return new ImapRequest(ExamineResponse.class, tag).add(ImapCmd.EXAMINE, folderName);
    }
}
