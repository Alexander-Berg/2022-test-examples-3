package ru.yandex.autotests.innerpochta.imap.requests;

import ru.yandex.autotests.innerpochta.imap.consts.base.ImapCmd;
import ru.yandex.autotests.innerpochta.imap.responses.GenericResponse;

/**
 * Created with IntelliJ IDEA.
 * User: vicdev
 * Date: 30.04.14
 * Time: 15:26
 */
public class UnselectRequest implements ImapRequestBuilder<GenericResponse> {
    private UnselectRequest() {
    }

    public static UnselectRequest unselect() {
        return new UnselectRequest();
    }

    @Override
    public ImapRequest<GenericResponse> build(String tag) {
        return new ImapRequest(GenericResponse.class, tag).add(ImapCmd.UNSELECT);
    }
}
