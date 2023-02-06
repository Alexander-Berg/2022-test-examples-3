package ru.yandex.autotests.innerpochta.imap.requests;

import ru.yandex.autotests.innerpochta.imap.consts.base.ImapCmd;
import ru.yandex.autotests.innerpochta.imap.responses.GenericResponse;

/**
 * Created with IntelliJ IDEA.
 * User: vicdev
 * Date: 29.04.14
 * Time: 16:31
 * <p/>
 * Комманда должна выполняться без тага, после комманды IDLE
 */
public class DoneRequest implements ImapRequestBuilder<GenericResponse> {
    private DoneRequest() {
    }

    public static DoneRequest done() {
        return new DoneRequest();
    }

    @Override
    public ImapRequest<GenericResponse> build(String tag) {
        return new ImapRequest(GenericResponse.class).complex().add(ImapCmd.DONE);
    }
}
