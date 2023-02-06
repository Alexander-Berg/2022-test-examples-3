package ru.yandex.market.logistic.api.model.fulfillment.request;

import ru.yandex.market.logistic.api.model.common.request.RequestWrapper;
import ru.yandex.market.logistic.api.utils.ParsingWrapperTest;

public class GetExpirationItemsRequestParsingTest extends ParsingWrapperTest<RequestWrapper,
    GetExpirationItemsRequest> {

    public GetExpirationItemsRequestParsingTest() {
        super(
            RequestWrapper.class,
            GetExpirationItemsRequest.class,
            "fixture/request/get_expiration_items_request.xml"
        );
    }
}
