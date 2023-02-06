package ru.yandex.market.checkout.checkouter.json;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;

import org.hamcrest.Matcher;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.mock.http.MockHttpInputMessage;
import org.springframework.mock.http.MockHttpOutputMessage;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import ru.yandex.market.checkout.util.json.JsonTest;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = {
        "classpath:WEB-INF/checkouter-serialization.xml"}
)
public abstract class AbstractJsonHandlerTestBase {

    @Autowired
    protected HttpMessageConverter<Object> converter;

    @SuppressWarnings("unchecked")
    protected <T> T read(Class<T> clazz, InputStream json) throws IOException {
        return (T) converter.read(clazz, new MockHttpInputMessage(json));
    }

    @SuppressWarnings("unchecked")
    protected <T> T read(Class<T> clazz, String json) throws IOException {
        return (T) converter.read(clazz, new MockHttpInputMessage(json.getBytes(StandardCharsets.UTF_8)));
    }

    protected String write(Object object) throws IOException {
        MockHttpOutputMessage mockHttpOutputMessage = new MockHttpOutputMessage();
        converter.write(object, MediaType.APPLICATION_JSON_UTF8, mockHttpOutputMessage);
        return mockHttpOutputMessage.getBodyAsString();
    }

    protected void checkJson(String json, String path, Object value) throws ParseException {
        JsonTest.checkJson(json, path, value);
    }

    protected <T> void checkJsonMatcher(String json, String path, Matcher<T> matcher) throws ParseException {
        JsonTest.checkJsonMatcher(json, path, matcher);
    }

    protected void checkJson(String json, String path, JsonTest.JsonConsumer handler) throws ParseException {
        JsonTest.checkJson(json, path, handler);
    }

    protected void checkJsonNotExist(String json, String path) {
        JsonTest.checkJsonNotExist(json, path);
    }
}
