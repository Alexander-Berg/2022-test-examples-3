package ru.yandex.market.logistic.api.model.fulfillment.request;

import ru.yandex.market.logistic.api.model.common.request.RequestWrapper;
import ru.yandex.market.logistic.api.model.fulfillment.ResourceId;
import ru.yandex.market.logistic.api.utils.ParsingXmlWrapperTest;

public class GetOrderHistoryRequestParsingTest extends ParsingXmlWrapperTest<RequestWrapper, GetOrderHistoryRequest> {

    public GetOrderHistoryRequestParsingTest() {
        super(RequestWrapper.class, GetOrderHistoryRequest.class, "fixture/request/ff_get_order_history.xml");
        addNodeNameToSkip("orderId");
    }

    @Override
    protected void performAdditionalAssertions(RequestWrapper requestWrapper) {
        ResourceId resourceId = ((GetOrderHistoryRequest) requestWrapper.getRequest()).getOrderId();

        assertions().assertThat(resourceId.getYandexId())
            .as("Asserting yandex id")
            .isEqualTo("25");

        assertions().assertThat(resourceId.getPartnerId())
            .as("Asserting fulfillment id")
            .isEqualTo("45");
    }
}
