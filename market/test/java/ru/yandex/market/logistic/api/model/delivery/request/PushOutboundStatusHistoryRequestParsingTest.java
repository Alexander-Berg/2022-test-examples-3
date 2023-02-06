package ru.yandex.market.logistic.api.model.delivery.request;

import ru.yandex.market.logistic.api.model.common.request.RequestWrapper;
import ru.yandex.market.logistic.api.utils.ParsingWrapperTest;

public class PushOutboundStatusHistoryRequestParsingTest extends
    ParsingWrapperTest<RequestWrapper, PushOutboundStatusHistoryRequest> {

    PushOutboundStatusHistoryRequestParsingTest() {
        super(RequestWrapper.class, PushOutboundStatusHistoryRequest.class,
            "fixture/request/ds_push_outbound_status_history.xml");
    }
}
