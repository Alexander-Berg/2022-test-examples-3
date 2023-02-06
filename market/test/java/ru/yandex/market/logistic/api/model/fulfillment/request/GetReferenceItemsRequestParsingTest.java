package ru.yandex.market.logistic.api.model.fulfillment.request;

import ru.yandex.market.logistic.api.model.common.request.RequestWrapper;
import ru.yandex.market.logistic.api.utils.ParsingWrapperTest;

public class GetReferenceItemsRequestParsingTest extends ParsingWrapperTest<RequestWrapper, GetReferenceItemsRequest> {

    public GetReferenceItemsRequestParsingTest() {
        super(RequestWrapper.class, GetReferenceItemsRequest.class, "fixture/request/get_reference_items_request.xml");
    }
}
