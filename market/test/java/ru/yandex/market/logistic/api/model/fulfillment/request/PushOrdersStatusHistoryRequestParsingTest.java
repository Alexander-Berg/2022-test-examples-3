package ru.yandex.market.logistic.api.model.fulfillment.request;

import ru.yandex.market.logistic.api.model.common.request.RequestWrapper;
import ru.yandex.market.logistic.api.utils.ParsingWrapperTest;

class PushOrdersStatusHistoryRequestParsingTest extends
        ParsingWrapperTest<RequestWrapper, PushOrdersStatusHistoryRequest> {

    PushOrdersStatusHistoryRequestParsingTest() {
        super(RequestWrapper.class, PushOrdersStatusHistoryRequest.class,
                "fixture/request/ff_push_orders_status_history.xml");
    }
}
