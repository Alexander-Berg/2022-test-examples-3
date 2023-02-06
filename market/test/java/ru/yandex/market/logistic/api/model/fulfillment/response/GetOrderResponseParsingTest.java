package ru.yandex.market.logistic.api.model.fulfillment.response;

import ru.yandex.market.logistic.api.model.common.response.ResponseWrapper;
import ru.yandex.market.logistic.api.utils.ParsingXmlWrapperTest;

public class GetOrderResponseParsingTest extends ParsingXmlWrapperTest<ResponseWrapper, GetOrderResponse> {

    public GetOrderResponseParsingTest() {
        super(ResponseWrapper.class, GetOrderResponse.class, "fixture/response/get_order_response.xml");
    }
}
