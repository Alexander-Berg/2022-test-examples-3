package ru.yandex.market.logistic.api.model.fulfillment.request;

import ru.yandex.market.logistic.api.model.common.request.RequestWrapper;
import ru.yandex.market.logistic.api.model.fulfillment.ResourceId;
import ru.yandex.market.logistic.api.utils.ParsingXmlWrapperTest;

public class GetOrderRequestParsingTest extends ParsingXmlWrapperTest<RequestWrapper, GetOrderRequest> {

    public GetOrderRequestParsingTest() {
        super(RequestWrapper.class, GetOrderRequest.class, "fixture/request/get_order_request.xml");
        addNodeNameToSkip("orderId");
    }

    @Override
    protected void performAdditionalAssertions(RequestWrapper requestWrapper) {
        ResourceId resourceId = ((GetOrderRequest) requestWrapper.getRequest()).getOrderId();

        assertions().assertThat(resourceId.getYandexId())
            .as("Asserting yandex id")
            .isEqualTo("25");

        assertions().assertThat(resourceId.getPartnerId())
            .as("Asserting fulfillment id")
            .isEqualTo("45");
    }
}
