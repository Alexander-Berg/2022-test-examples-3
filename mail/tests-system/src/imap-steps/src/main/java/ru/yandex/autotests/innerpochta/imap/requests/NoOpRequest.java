package ru.yandex.autotests.innerpochta.imap.requests;

import ru.yandex.autotests.innerpochta.imap.consts.base.ImapCmd;
import ru.yandex.autotests.innerpochta.imap.responses.NoopExpungeResponse;

public class NoOpRequest implements ImapRequestBuilder<NoopExpungeResponse> {
    private NoOpRequest() {
    }

    public static NoOpRequest noOp() {
        return new NoOpRequest();
    }

    @Override
    public ImapRequest<NoopExpungeResponse> build(String tag) {
        return new ImapRequest(NoopExpungeResponse.class, tag).add(ImapCmd.NOOP);
    }
}
