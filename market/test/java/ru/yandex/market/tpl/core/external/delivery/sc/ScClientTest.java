package ru.yandex.market.tpl.core.external.delivery.sc;

import java.nio.charset.StandardCharsets;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Test;

import ru.yandex.market.logistic.api.model.common.request.RequestWrapper;
import ru.yandex.market.logistic.api.model.fulfillment.request.CreateOrderRequest;
import ru.yandex.market.tpl.core.config.external.DeliveryConfiguration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author kukabara
 */
class ScClientTest {

    private static final ObjectMapper OBJECT_MAPPER = DeliveryConfiguration.SimpleClientConfiguration.SC_OBJECT_MAPPER;

    @Test
    void parseCreateOrder() throws Exception {
        String rawInput = IOUtils.toString(this.getClass().getResourceAsStream("/sc/create_order.xml"),
                StandardCharsets.UTF_8);

        RequestWrapper<CreateOrderRequest> createOrderRequest = OBJECT_MAPPER.readValue(rawInput,
                new TypeReference<RequestWrapper<CreateOrderRequest>>() {
                });

        assertThat(createOrderRequest.getRequest()).isNotNull();
    }

}
