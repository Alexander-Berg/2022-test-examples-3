package ru.yandex.autotests.innerpochta.imap.requests;

import ru.yandex.autotests.innerpochta.imap.consts.base.ImapCmd;
import ru.yandex.autotests.innerpochta.imap.responses.NoopExpungeResponse;

public class ExpungeRequest implements ImapRequestBuilder<NoopExpungeResponse> {

    private String sequenceSet;
    private boolean isUid = false;

    private ExpungeRequest() {
    }

    private ExpungeRequest(String sequenceSet) {
        this.sequenceSet = sequenceSet;
    }

    public static ExpungeRequest expunge() {
        return new ExpungeRequest();
    }

    public static ExpungeRequest uidExpunge(String sequenceSet) {
        return new ExpungeRequest(sequenceSet).uid(true);
    }

    @Override
    public ImapRequest<NoopExpungeResponse> build(String tag) {
        ImapRequest<NoopExpungeResponse> request = new ImapRequest(NoopExpungeResponse.class, tag);
        if (isUid) {
            request.add(ImapCmd.UID);
            return request.add(ImapCmd.EXPUNGE, sequenceSet);
        } else {
            return request.add(ImapCmd.EXPUNGE);
        }
    }

    private ExpungeRequest uid(boolean value) {
        isUid = value;
        return this;
    }
}
