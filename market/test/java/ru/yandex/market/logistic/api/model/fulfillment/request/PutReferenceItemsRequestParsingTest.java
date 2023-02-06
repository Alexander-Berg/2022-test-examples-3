package ru.yandex.market.logistic.api.model.fulfillment.request;

import ru.yandex.market.logistic.api.model.common.request.RequestWrapper;
import ru.yandex.market.logistic.api.utils.ParsingXmlWrapperTest;

public class PutReferenceItemsRequestParsingTest extends ParsingXmlWrapperTest<RequestWrapper,
        PutReferenceItemsRequest> {

    public PutReferenceItemsRequestParsingTest() {
        super(RequestWrapper.class, PutReferenceItemsRequest.class, "fixture/request/put_reference_items.xml");
    }
}
