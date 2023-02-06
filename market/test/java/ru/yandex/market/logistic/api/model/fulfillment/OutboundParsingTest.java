package ru.yandex.market.logistic.api.model.fulfillment;

import ru.yandex.market.logistic.api.utils.ParsingTest;

public class OutboundParsingTest extends ParsingTest<Outbound> {

    public OutboundParsingTest() {
        super(Outbound.class, "fixture/entities/outbound.xml");
    }
}
