package ru.yandex.market.logistic.api.model.fulfillment.request;

import ru.yandex.market.logistic.api.model.common.request.RequestWrapper;
import ru.yandex.market.logistic.api.utils.ParsingWrapperTest;

public class GetOutboundDetailsRequestParsingTest extends ParsingWrapperTest<RequestWrapper,
    GetOutboundDetailsRequest> {

    public GetOutboundDetailsRequestParsingTest() {
        super(
            RequestWrapper.class,
            GetOutboundDetailsRequest.class,
            "fixture/request/get_outbound_details_request.xml"
        );
    }
}
