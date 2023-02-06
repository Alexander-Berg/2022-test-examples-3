package ru.yandex.market.logistic.api.model.fulfillment.response.entities;

import ru.yandex.market.logistic.api.utils.ParsingTest;

public class ExpirationParsingTest extends ParsingTest<Expiration> {

    public ExpirationParsingTest() {
        super(Expiration.class, "fixture/response/entities/expiration.xml");
    }
}
