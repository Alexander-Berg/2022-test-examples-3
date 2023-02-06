package ru.yandex.market.logistic.api.model.fulfillment.response;

import ru.yandex.market.logistic.api.model.common.response.ResponseWrapper;
import ru.yandex.market.logistic.api.utils.ParsingWrapperTest;

public class GetExpirationItemsResponseParsingTest extends ParsingWrapperTest<ResponseWrapper,
    GetExpirationItemsResponse> {

    public GetExpirationItemsResponseParsingTest() {
        super(ResponseWrapper.class, GetExpirationItemsResponse.class, "fixture/response" +
            "/get_expiration_items_response.xml");
    }
}
