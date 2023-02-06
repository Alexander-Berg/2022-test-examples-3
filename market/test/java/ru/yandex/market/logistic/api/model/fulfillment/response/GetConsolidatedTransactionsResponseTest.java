package ru.yandex.market.logistic.api.model.fulfillment.response;

import ru.yandex.market.logistic.api.model.common.response.ResponseWrapper;
import ru.yandex.market.logistic.api.utils.ParsingXmlWrapperTest;

public class GetConsolidatedTransactionsResponseTest
    extends ParsingXmlWrapperTest<ResponseWrapper, GetConsolidatedTransactionsResponse> {

    public GetConsolidatedTransactionsResponseTest() {
        super(
            ResponseWrapper.class,
            GetConsolidatedTransactionsResponse.class,
            "fixture/response/ff_get_consolidated_transactions_response.xml"
        );
    }
}
