package ru.yandex.market.logistic.api.model.fulfillment.request;

import ru.yandex.market.logistic.api.model.common.request.RequestWrapper;
import ru.yandex.market.logistic.api.utils.ParsingWrapperTest;

public class PushCargoUnitsRequestParsingTest extends
    ParsingWrapperTest<RequestWrapper, PushCargoUnitsRequest> {

    PushCargoUnitsRequestParsingTest() {
        super(RequestWrapper.class, PushCargoUnitsRequest.class,
            "fixture/request/ff_push_cargo_units.xml");
    }
}
