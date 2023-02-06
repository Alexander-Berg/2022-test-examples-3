package ru.yandex.autotests.innerpochta.imap.requests;

import ru.yandex.autotests.innerpochta.imap.consts.base.ImapCmd;
import ru.yandex.autotests.innerpochta.imap.responses.IdleResponse;

/**
 * Created with IntelliJ IDEA.
 * User: vicdev
 * Date: 29.04.14
 * Time: 16:00
 */
public class IdleRequest implements ImapRequestBuilder<IdleResponse> {
    private IdleRequest() {
    }

    public static IdleRequest idle() {
        return new IdleRequest();
    }

    @Override
    public ImapRequest<IdleResponse> build(String tag) {
        return new ImapRequest(IdleResponse.class, tag).complex().add(ImapCmd.IDLE);
    }
}
