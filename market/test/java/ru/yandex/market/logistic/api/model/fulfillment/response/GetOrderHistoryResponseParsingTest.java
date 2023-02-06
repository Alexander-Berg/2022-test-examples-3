package ru.yandex.market.logistic.api.model.fulfillment.response;

import ru.yandex.market.logistic.api.model.common.response.ResponseWrapper;
import ru.yandex.market.logistic.api.model.fulfillment.ResourceId;
import ru.yandex.market.logistic.api.model.fulfillment.response.entities.OrderStatusHistory;
import ru.yandex.market.logistic.api.utils.ParsingXmlWrapperTest;

public class GetOrderHistoryResponseParsingTest extends ParsingXmlWrapperTest<ResponseWrapper,
    GetOrderHistoryResponse> {

    public GetOrderHistoryResponseParsingTest() {
        super(ResponseWrapper.class, GetOrderHistoryResponse.class, "fixture/response/ff_get_order_history.xml");
        addNodeNameToSkip("orderId");
    }

    @Override
    protected void performAdditionalAssertions(ResponseWrapper responseWrapper) {
        OrderStatusHistory orderStatusHistory =
            ((GetOrderHistoryResponse) responseWrapper.getResponse()).getOrderStatusHistory();
        ResourceId resourceId = orderStatusHistory.getOrderId();

        assertions().assertThat(resourceId.getYandexId())
            .as("Asserting yandex id")
            .isEqualTo("25");

        assertions().assertThat(resourceId.getPartnerId())
            .as("Asserting fulfillment id")
            .isEqualTo("45");

        assertions().assertThat(orderStatusHistory.getHistory())
            .as("Asserting history size")
            .hasSize(3);
    }
}
