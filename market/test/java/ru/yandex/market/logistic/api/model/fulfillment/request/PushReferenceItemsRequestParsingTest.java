package ru.yandex.market.logistic.api.model.fulfillment.request;

import ru.yandex.market.logistic.api.model.common.request.RequestWrapper;
import ru.yandex.market.logistic.api.utils.ParsingXmlWrapperTest;

public class PushReferenceItemsRequestParsingTest extends ParsingXmlWrapperTest<RequestWrapper,
        PushReferenceItemsRequest> {

    public PushReferenceItemsRequestParsingTest() {
        super(RequestWrapper.class, PushReferenceItemsRequest.class, "fixture/request/push_reference_items.xml");
    }
}
