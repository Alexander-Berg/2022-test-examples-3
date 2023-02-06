package ru.yandex.market.logistic.api.model.fulfillment.response;

import ru.yandex.market.logistic.api.model.common.response.ResponseWrapper;
import ru.yandex.market.logistic.api.utils.ParsingXmlWrapperTest;

public class GetTransactionsOrdersResponseTest
    extends ParsingXmlWrapperTest<ResponseWrapper, GetTransactionsOrdersResponse> {

    public GetTransactionsOrdersResponseTest() {
        super(
            ResponseWrapper.class,
            GetTransactionsOrdersResponse.class,
            "fixture/response/ff_get_transactions_orders_response.xml"
        );
    }
}
