package ru.yandex.market.logistic.api.model.delivery.request;

import ru.yandex.market.logistic.api.model.common.request.RequestWrapper;
import ru.yandex.market.logistic.api.utils.ParsingWrapperTest;

public class GetTransactionsOrdersRequestTest extends ParsingWrapperTest<RequestWrapper, GetTransactionsOrdersRequest> {

    public GetTransactionsOrdersRequestTest() {
        super(RequestWrapper.class, GetTransactionsOrdersRequest.class,
                "fixture/request/ds_get_transactions_orders_request.xml");
    }
}
