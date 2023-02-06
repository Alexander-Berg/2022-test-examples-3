package ru.yandex.market.logistic.api.model.fulfillment.response.entities;

import ru.yandex.market.logistic.api.utils.ParsingTest;

public class OutboundStatusParsingTest extends ParsingTest<OutboundStatus> {

    public OutboundStatusParsingTest() {
        super(OutboundStatus.class, "fixture/response/entities/outbound_status.xml");
    }
}
