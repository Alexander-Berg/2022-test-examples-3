package ru.yandex.market.logistic.api.model.fulfillment.response;

import ru.yandex.market.logistic.api.model.common.response.ResponseWrapper;
import ru.yandex.market.logistic.api.utils.ParsingWrapperTest;

public class GetReferenceItemsResponseParsingTest
    extends ParsingWrapperTest<ResponseWrapper, GetReferenceItemsResponse> {

    public GetReferenceItemsResponseParsingTest() {
        super(
            ResponseWrapper.class,
            GetReferenceItemsResponse.class,
            "fixture/response/get_reference_items_response.xml"
        );
    }
}
