package ru.yandex.market.checkout.checkouter.json;

import java.io.IOException;
import java.util.EnumSet;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.mock.http.MockHttpInputMessage;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import ru.yandex.market.checkout.checkouter.delivery.Delivery;
import ru.yandex.market.checkout.checkouter.pay.PaymentMethod;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

@ExtendWith(SpringExtension.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@ContextConfiguration(locations = {"classpath:WEB-INF/checkouter-serialization.xml"})
public class DeliveryJsonDeserializerTest {

    public static final String JSON_STR =
            "{\"paymentOptions\":[" +
                    "{\"paymentType\":\"POSTPAID\",\"paymentMethod\":\"CARD_ON_DELIVERY\"}" +
                    ",{\"paymentType\":\"PREPAID\",\"paymentMethod\":\"BANK_CARD\"}" +
                    "]}";
    @Autowired
    HttpMessageConverter converter;

    @Test
    public void shouldDeserializePaymentOptions() throws IOException {
        HttpInputMessage inputMessage = new MockHttpInputMessage(JSON_STR.getBytes());
        Delivery delivery = (Delivery) converter.read(Delivery.class, inputMessage);
        assertThat("", delivery.getPaymentOptions(), equalTo((Set) EnumSet.of(PaymentMethod.BANK_CARD,
                PaymentMethod.CARD_ON_DELIVERY)));
    }
}
