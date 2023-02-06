package ru.yandex.market.logistic.api.model.fulfillment.request;

import ru.yandex.market.logistic.api.model.common.request.RequestWrapper;
import ru.yandex.market.logistic.api.utils.ParsingWrapperTest;

public class CreateOrderRequestParsingTest extends ParsingWrapperTest<RequestWrapper, CreateOrderRequest> {
    public CreateOrderRequestParsingTest() {
        super(RequestWrapper.class, CreateOrderRequest.class, "fixture/request/create_order_request.xml");
    }
}
