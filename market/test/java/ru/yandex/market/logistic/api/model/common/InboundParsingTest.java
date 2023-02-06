package ru.yandex.market.logistic.api.model.common;

import ru.yandex.market.logistic.api.utils.ParsingTest;

public class InboundParsingTest extends ParsingTest<Inbound> {

    public InboundParsingTest() {
        super(Inbound.class, "fixture/request/common_inbound.xml");
    }
}
