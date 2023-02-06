package ru.yandex.market.logistic.api.model.fulfillment.request;

import java.util.List;

import ru.yandex.market.logistic.api.model.common.request.RequestWrapper;
import ru.yandex.market.logistic.api.model.fulfillment.ResourceId;
import ru.yandex.market.logistic.api.utils.ParsingXmlWrapperTest;

public class GetOrdersStatusRequestParsingTest extends ParsingXmlWrapperTest<RequestWrapper, GetOrdersStatusRequest> {

    public GetOrdersStatusRequestParsingTest() {
        super(RequestWrapper.class, GetOrdersStatusRequest.class, "fixture/request/get_orders_status_request.xml");
        addNodeNameToSkip("orderId");
    }

    @Override
    protected void performAdditionalAssertions(RequestWrapper requestWrapper) {
        assertions().assertThat(requestWrapper.getRequest().getType())
            .as("Asserting type")
            .isEqualTo("getOrdersStatus");

        List<ResourceId> ordersId = ((GetOrdersStatusRequest) requestWrapper.getRequest()).getOrdersId();

        assertions().assertThat(ordersId.size())
            .as("Asserting lenght")
            .isEqualTo(3);

        assertions().assertThat(ordersId.get(0).getYandexId())
            .as("Asserting yandex id")
            .isEqualTo("35");

        assertions().assertThat(ordersId.get(1).getYandexId())
            .as("Asserting yandex id")
            .isEqualTo("36");

        assertions().assertThat(ordersId.get(2).getYandexId())
            .as("Asserting yandex id")
            .isEqualTo("37");

        assertions().assertThat(ordersId.get(0).getPartnerId())
            .as("Asserting fulfillment id")
            .isEqualTo("45");
        assertions().assertThat(ordersId.get(1).getPartnerId())
            .as("Asserting fulfillment id")
            .isEqualTo("46");
        assertions().assertThat(ordersId.get(2).getPartnerId())
            .as("Asserting fulfillment id")
            .isEqualTo("47");
    }
}
