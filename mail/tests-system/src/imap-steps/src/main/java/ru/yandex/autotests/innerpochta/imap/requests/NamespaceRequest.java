package ru.yandex.autotests.innerpochta.imap.requests;

import ru.yandex.autotests.innerpochta.imap.consts.base.ImapCmd;
import ru.yandex.autotests.innerpochta.imap.responses.NamespaceResponse;

/**
 * Created with IntelliJ IDEA.
 * User: vicdev
 * Date: 17.04.14
 * Time: 16:06
 */
public class NamespaceRequest implements ImapRequestBuilder<NamespaceResponse> {

    public static NamespaceRequest namespace() {
        return new NamespaceRequest();
    }

    @Override
    public ImapRequest<NamespaceResponse> build(String tag) {
        return new ImapRequest(NamespaceResponse.class, tag).add(ImapCmd.NAMESPACE);
    }
}
