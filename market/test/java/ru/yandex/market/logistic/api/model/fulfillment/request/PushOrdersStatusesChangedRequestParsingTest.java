package ru.yandex.market.logistic.api.model.fulfillment.request;

import ru.yandex.market.logistic.api.model.common.request.RequestWrapper;
import ru.yandex.market.logistic.api.utils.ParsingWrapperTest;

public class PushOrdersStatusesChangedRequestParsingTest
    extends ParsingWrapperTest<RequestWrapper, PushOrdersStatusesChangedRequest> {

    public PushOrdersStatusesChangedRequestParsingTest() {
        super(RequestWrapper.class, PushOrdersStatusesChangedRequest.class,
            "fixture/request/ff_push_orders_statuses_changed.xml");
    }
}
