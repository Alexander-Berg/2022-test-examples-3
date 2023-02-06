package ru.yandex.market.logistic.api.model.fulfillment.response;

import java.util.List;

import ru.yandex.market.logistic.api.model.common.response.ResponseWrapper;
import ru.yandex.market.logistic.api.model.fulfillment.response.entities.OrderStatusHistory;
import ru.yandex.market.logistic.api.utils.ParsingXmlWrapperTest;

public class GetOrdersStatusResponseParsingTest extends ParsingXmlWrapperTest<ResponseWrapper,
    GetOrdersStatusResponse> {

    public GetOrdersStatusResponseParsingTest() {
        super(ResponseWrapper.class, GetOrdersStatusResponse.class, "fixture/response/get_orders_status_response.xml");
        addNodeNameToSkip("orderId");
    }

    @Override
    protected void performAdditionalAssertions(ResponseWrapper responseWrapper) {
        GetOrdersStatusResponse responseContent = (GetOrdersStatusResponse) responseWrapper.getResponse();

        assertions().assertThat(responseContent.getType())
            .as("Asserting type")
            .isEqualTo("getOrdersStatus");

        List<OrderStatusHistory> histories = responseContent.getOrderStatusHistories();
        assertions().assertThat(histories.size())
            .as("Asserting histories count")
            .isEqualTo(2);

        assertions().assertThat(histories.get(0).getHistory().size())
            .as("Asserting checkpoints count")
            .isEqualTo(2);

        assertions().assertThat(histories.get(1).getHistory().size())
            .as("Asserting checkpoints count")
            .isEqualTo(3);
    }
}
