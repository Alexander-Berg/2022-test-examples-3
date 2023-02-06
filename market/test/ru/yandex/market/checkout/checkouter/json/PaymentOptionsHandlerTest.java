package ru.yandex.market.checkout.checkouter.json;

import java.io.IOException;
import java.util.Collections;

import org.json.JSONException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.skyscreamer.jsonassert.JSONAssert;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.mock.http.MockHttpOutputMessage;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import ru.yandex.market.checkout.checkouter.delivery.Delivery;

@ExtendWith(SpringExtension.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@ContextConfiguration(locations = {"classpath:WEB-INF/checkouter-serialization.xml"})
public class PaymentOptionsHandlerTest {

    public static final String EXPECTED_STR = "{\"paymentOptions\":[]}";

    @Autowired
    HttpMessageConverter converter;

    @Test
    public void shouldSerializeNullPaymentOptions() throws IOException, JSONException {
        Delivery delivery = new Delivery();
        delivery.setPaymentOptions(null);
        MockHttpOutputMessage outputMessage = new MockHttpOutputMessage();
        converter.write(delivery, MediaType.parseMediaType("json/application"), outputMessage);
        JSONAssert.assertEquals(EXPECTED_STR, outputMessage.getBodyAsString(), false);
    }

    @Test
    public void shouldSerializeEmptyPaymentOptions() throws IOException, JSONException {
        Delivery delivery = new Delivery();
        delivery.setPaymentOptions(Collections.emptySet());
        MockHttpOutputMessage outputMessage = new MockHttpOutputMessage();
        converter.write(delivery, MediaType.parseMediaType("json/application"), outputMessage);
        JSONAssert.assertEquals(EXPECTED_STR, outputMessage.getBodyAsString(), false);
    }
}
