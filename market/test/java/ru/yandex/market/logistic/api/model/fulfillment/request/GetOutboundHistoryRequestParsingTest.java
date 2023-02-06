package ru.yandex.market.logistic.api.model.fulfillment.request;

import ru.yandex.market.logistic.api.model.common.request.RequestWrapper;
import ru.yandex.market.logistic.api.utils.ParsingWrapperTest;

public class GetOutboundHistoryRequestParsingTest
    extends ParsingWrapperTest<RequestWrapper, GetOutboundHistoryRequest> {

    public GetOutboundHistoryRequestParsingTest() {
        super(
            RequestWrapper.class,
            GetOutboundHistoryRequest.class,
            "fixture/request/get_outbound_history_request.xml"
        );
    }
}
