package ru.yandex.market.abo.core.premod.model;

/**
 * needed to workaround {@link PremodTicket#stateChanged} in tests.
 */
public class PremodTicketMock extends PremodTicket {
    public PremodTicketMock(PremodTicketStatus status) {
        super();
        this.statusId = status.getId();
    }
}
