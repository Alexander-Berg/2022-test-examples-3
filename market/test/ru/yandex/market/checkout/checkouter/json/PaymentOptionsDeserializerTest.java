package ru.yandex.market.checkout.checkouter.json;

import java.io.IOException;
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

import static java.util.Collections.emptySet;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

@ExtendWith(SpringExtension.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@ContextConfiguration(locations = {"classpath:WEB-INF/checkouter-serialization.xml"})
public class PaymentOptionsDeserializerTest {

    public static final String EMPTY_ARRAY = "{\"paymentOptions\":[]}";
    public static final String NULL_ARRAY = "{}";
    @Autowired
    HttpMessageConverter converter;

    @Test
    public void shouldDeserializeEmptyPaymentOptions() throws IOException {
        HttpInputMessage inputMessage = new MockHttpInputMessage(EMPTY_ARRAY.getBytes());
        Delivery delivery = (Delivery) converter.read(Delivery.class, inputMessage);
        assertThat("", delivery.getPaymentOptions(), equalTo((Set) emptySet()));
    }

    @Test
    public void shouldDeserializeNullPaymentOptions() throws IOException {
        HttpInputMessage inputMessage = new MockHttpInputMessage(NULL_ARRAY.getBytes());
        Delivery delivery = (Delivery) converter.read(Delivery.class, inputMessage);
        assertThat("", delivery.getPaymentOptions(), equalTo((Set) emptySet()));
    }
}
