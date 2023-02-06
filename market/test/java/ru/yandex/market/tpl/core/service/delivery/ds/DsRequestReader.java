package ru.yandex.market.tpl.core.service.delivery.ds;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.apache.commons.io.IOUtils;
import org.springframework.stereotype.Component;

import ru.yandex.market.logistic.api.model.common.request.AbstractRequest;
import ru.yandex.market.logistic.api.model.common.request.RequestWrapper;
import ru.yandex.market.logistic.api.model.delivery.ResourceId;
import ru.yandex.market.logistic.api.model.delivery.request.CreateOrderRequest;
import ru.yandex.market.logistic.api.model.delivery.request.GetOrderHistoryRequest;
import ru.yandex.market.logistic.api.model.delivery.response.CreateOrderResponse;
import ru.yandex.market.logistic.api.model.delivery.response.entities.OrderStatus;
import ru.yandex.market.tpl.common.util.TestUtil;
import ru.yandex.market.tpl.core.domain.ds.DsOrderManager;
import ru.yandex.market.tpl.core.domain.partner.Partner;
import ru.yandex.market.tpl.core.service.delivery.LogisticApiRequestProcessingConfiguration;

import static org.assertj.core.api.Assertions.assertThat;

@Component
@RequiredArgsConstructor
public class DsRequestReader {

    private static final ObjectMapper MAPPER = LogisticApiRequestProcessingConfiguration.DEFAULT_MAPPER;
    private static final String YANDEX_ORDER_ID = "12981801";

    private final DsOrderManager dsOrderManager;

    public <T extends AbstractRequest> T readRequest(String fileName, Class<T> clazz,
                                                     Object... args) throws IOException {
        String rawInput = IOUtils.toString(this.getClass().getResourceAsStream(fileName), StandardCharsets.UTF_8);
        if (args.length > 0) {
            rawInput = String.format(rawInput, args);
        }

        RequestWrapper<T> requestWrapper = MAPPER.readValue(rawInput,
                MAPPER.getTypeFactory().constructParametricType(RequestWrapper.class, clazz));

        assertThat(requestWrapper.getRequest()).isNotNull();
        return requestWrapper.getRequest();
    }

    public CreateOrderResponse sendCreateOrder(Partner partner) throws IOException {
        return sendCreateOrder(null, partner);
    }

    public CreateOrderResponse sendCreateOrder(String externalId, Partner partner) throws IOException {
        return sendCreateOrder("/ds/create_order.xml", externalId, partner);
    }

    public CreateOrderResponse sendCreateOrder(String fileName,
                                               String externalId,
                                               Partner partner) throws IOException {
        CreateOrderRequest request = readRequest(fileName, CreateOrderRequest.class, YANDEX_ORDER_ID);
        return dsOrderManager.createOrder(withExternalId(request.getOrder(), externalId), partner);
    }

    public List<OrderStatus> sendGetOrderHistory(long orderId, Partner partner) throws IOException {
        GetOrderHistoryRequest getOrderHistoryRequest = readRequest("/ds/get_order_history.xml",
                GetOrderHistoryRequest.class, YANDEX_ORDER_ID, orderId);
        return dsOrderManager.getOrderHistory(getOrderHistoryRequest.getOrderId(),
                partner).getOrderStatusHistory().getHistory();
    }

    @SneakyThrows
    private ru.yandex.market.logistic.api.model.delivery.Order withExternalId(
            ru.yandex.market.logistic.api.model.delivery.Order order, String externalOrderId
    ) {
        if (externalOrderId != null) {
            TestUtil.setPrivateFinalField(order, "orderId",
                    new ResourceId(externalOrderId, null, null));
        }
        return order;
    }

}
