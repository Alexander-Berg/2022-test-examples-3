package ru.yandex.market.checkout.referee.entity;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;

import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.mock.http.MockHttpInputMessage;
import org.springframework.mock.http.MockHttpOutputMessage;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

/**
 * @author kukabara
 */

@ExtendWith(SpringExtension.class)
@ContextConfiguration({
        "classpath:checkout-referee-test.xml"
})
public abstract class AbstractJsonHandlerTest {
    @Autowired
    protected HttpMessageConverter checkoutRefereeAnnotationJsonMessageConverter;

    protected <T> T read(Class<T> clazz, InputStream json) throws IOException {
        return (T) checkoutRefereeAnnotationJsonMessageConverter.read(clazz, new MockHttpInputMessage(json));
    }

    protected <T> T read(Class<T> clazz, String json) throws IOException {
        return (T) checkoutRefereeAnnotationJsonMessageConverter.read(clazz, new MockHttpInputMessage(json.getBytes(StandardCharsets.UTF_8)));
    }

    protected String write(Object object) throws IOException {
        MockHttpOutputMessage mockHttpOutputMessage = new MockHttpOutputMessage();
        checkoutRefereeAnnotationJsonMessageConverter.write(object, MediaType.APPLICATION_JSON_UTF8, mockHttpOutputMessage);
        return mockHttpOutputMessage.getBodyAsString();
    }

    protected static void checkJson(String json, String path, Object value) throws ParseException {
        JsonTest.checkJson(json, path, value);
    }
}
