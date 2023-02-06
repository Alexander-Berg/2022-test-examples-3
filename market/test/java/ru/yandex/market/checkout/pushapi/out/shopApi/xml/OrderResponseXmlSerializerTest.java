package ru.yandex.market.checkout.pushapi.out.shopApi.xml;

import java.sql.Date;
import java.time.LocalDate;
import java.time.ZoneId;

import io.github.benas.randombeans.api.EnhancedRandom;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.RepeatedTest;

import ru.yandex.market.checkout.pushapi.client.entity.OrderResponse;
import ru.yandex.market.checkout.pushapi.client.entity.order.DeclineReason;
import ru.yandex.market.checkout.pushapi.client.util.CheckoutDateFormat;
import ru.yandex.market.checkout.pushapi.client.util.test.XmlTestUtil;
import ru.yandex.market.checkout.pushapi.in.xml.OrderResponseXmlSerializer;
import ru.yandex.market.checkout.util.EnhancedRandomHelper;

//MARKETCHECKOUT-4009 run all randomized tests several times to prevent floating failures
public class OrderResponseXmlSerializerTest {

    private final EnhancedRandom enhancedRandom = EnhancedRandomHelper.createEnhancedRandom();

    private OrderResponseXmlSerializer serializer = new OrderResponseXmlSerializer();

    @BeforeEach
    public void setUp() throws Exception {
        serializer.setCheckoutDateFormat(new CheckoutDateFormat());
    }

    @RepeatedTest(10)
    public void testSerializeAccepted() throws Exception {
        XmlTestUtil.assertSerializeResultAndString(
                serializer,
                new OrderResponse() {{
                    setAccepted(true);
                    setShipmentDate(Date.from(LocalDate.of(2021, 2, 23)
                            .atStartOfDay(ZoneId.systemDefault())
                            .toInstant()));
                }},
                "<order accepted='true' shipment-date='23-02-2021'/>"
        );

    }

    @RepeatedTest(10)
    public void testSerializeDeclined() throws Exception {
        OrderResponse orderResponse = enhancedRandom.nextObject(OrderResponse.class);
        orderResponse.setAccepted(false);
        orderResponse.setReason(DeclineReason.OTHER);
        orderResponse.setId("1234");
        orderResponse.setShipmentDate(null);

        XmlTestUtil.assertSerializeResultAndString(
                serializer,
                orderResponse,
                "<order id='1234' accepted='false' reason='OTHER' />"
        );
    }

    @RepeatedTest(10)
    public void testSerializeEmpty() throws Exception {
        XmlTestUtil.assertSerializeResultAndString(
                serializer,
                new OrderResponse(),
                "<order />"
        );
    }
}
