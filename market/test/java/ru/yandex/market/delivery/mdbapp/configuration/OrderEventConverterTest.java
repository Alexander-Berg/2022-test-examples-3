package ru.yandex.market.delivery.mdbapp.configuration;

import java.nio.charset.StandardCharsets;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.io.IOUtils;
import org.json.JSONObject;
import org.junit.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import steps.orderSteps.OrderEventSteps;

import ru.yandex.market.checkout.checkouter.event.OrderHistoryEvent;


public class OrderEventConverterTest {
    private AppConfiguration appConfiguration = new AppConfiguration();
    private MappingJackson2HttpMessageConverter converter = appConfiguration.dsmClientMessageConverter();

    @Test
    public void orderEventTest() throws Exception {
        final String expectedJson =
            IOUtils.toString(OrderEventConverterTest.class.getResourceAsStream("/order.json"), StandardCharsets.UTF_8);
        OrderHistoryEvent orderHistoryEvent = OrderEventSteps.getOrderHistoryEvent();

        ObjectMapper mapper = converter.getObjectMapper();
        JSONObject actualOrderEventJsonObject = new JSONObject(
            mapper.writeValueAsString(orderHistoryEvent)
        );
        JSONObject actualOrderJsonObject = (JSONObject) (actualOrderEventJsonObject.get("orderAfter"));

        JSONAssert.assertEquals(expectedJson, actualOrderJsonObject.toString(), JSONCompareMode.STRICT_ORDER);
    }
}
