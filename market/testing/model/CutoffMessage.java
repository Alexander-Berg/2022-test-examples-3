package ru.yandex.market.partner.testing.model;

import ru.yandex.market.core.cutoff.model.CutoffInfo;

/**
 * @author mkasumov
 */
public class CutoffMessage {

    private CutoffInfo cutoff;
    private MessageDetails message;

    public CutoffMessage(CutoffInfo cutoff, MessageDetails message) {
        this.cutoff = cutoff;
        this.message = message;
    }

    public CutoffInfo getCutoff() {
        return cutoff;
    }

    public MessageDetails getMessage() {
        return message;
    }

}
