package ru.yandex.market.checkout.checkouter.json;

import java.io.IOException;
import java.math.BigDecimal;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.mock.http.MockHttpInputMessage;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import ru.yandex.market.checkout.checkouter.order.Order;
import ru.yandex.market.checkout.checkouter.order.OrderAcceptMethod;

@ExtendWith(SpringExtension.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@ContextConfiguration(locations = {"classpath:WEB-INF/checkouter-serialization.xml"})
public class OrderJsonDeserializerTest {

    public static final String JSON_STR = "{\"acceptMethod\":\"PUSH_API\"}";
    public static final String EMPTY_STR = "{}";
    @Autowired
    HttpMessageConverter converter;

    @Test
    public void shouldDeserializeAcceptMethod() throws IOException {
        HttpInputMessage inputMessage = new MockHttpInputMessage(JSON_STR.getBytes());
        Order order = (Order) converter.read(Order.class, inputMessage);
        Assertions.assertEquals(OrderAcceptMethod.PUSH_API, order.getAcceptMethod());
    }

    @Test
    public void shouldReturnDefaultAcceptMethodWhenNotSpecified() throws IOException {
        HttpInputMessage inputMessage = new MockHttpInputMessage(EMPTY_STR.getBytes());
        Order order = (Order) converter.read(Order.class, inputMessage);
        Assertions.assertEquals(OrderAcceptMethod.DEFAULT, order.getAcceptMethod());
    }

    @Test
    public void promoPricesDeserializeTest() throws IOException {

        String json = "{\"global\": true, \"fulfilment\":true, \"subsidyTotal\":100, \"buyerItemsTotalDiscount\":200," +
                "\"buyerItemsTotalBeforeDiscount\":300, \"buyerTotalDiscount\":400, \"buyerTotalBeforeDiscount\":500}";

        HttpInputMessage inputMessage = new MockHttpInputMessage(json.getBytes());
        Order order = (Order) converter.read(Order.class, inputMessage);
        Assertions.assertEquals(new BigDecimal(100), order.getPromoPrices().getSubsidyTotal());
        Assertions.assertEquals(new BigDecimal(200), order.getPromoPrices().getBuyerItemsTotalDiscount());
        Assertions.assertEquals(new BigDecimal(300), order.getPromoPrices().getBuyerItemsTotalBeforeDiscount());
        Assertions.assertEquals(new BigDecimal(400), order.getPromoPrices().getBuyerTotalDiscount());
        Assertions.assertEquals(new BigDecimal(500), order.getPromoPrices().getBuyerTotalBeforeDiscount());
    }
}
