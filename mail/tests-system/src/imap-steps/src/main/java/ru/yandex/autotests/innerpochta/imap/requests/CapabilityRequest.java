package ru.yandex.autotests.innerpochta.imap.requests;

import ru.yandex.autotests.innerpochta.imap.consts.base.ImapCmd;
import ru.yandex.autotests.innerpochta.imap.responses.CapabilityResponse;

public class CapabilityRequest implements ImapRequestBuilder<CapabilityResponse> {
    private CapabilityRequest() {
    }

    public static CapabilityRequest capability() {
        return new CapabilityRequest();
    }

    @Override
    public ImapRequest<CapabilityResponse> build(String tag) {
        return new ImapRequest(CapabilityResponse.class, tag).add(ImapCmd.CAPABILITY);
    }
}
