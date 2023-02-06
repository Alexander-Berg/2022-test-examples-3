package ru.yandex.market.common.test.spring;

import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.mock.http.MockHttpInputMessage;
import org.springframework.mock.http.MockHttpOutputMessage;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * @author Georgiy Klimov gaklimov@yandex-team.ru
 */
public class MVCSerializationTest {

    protected String out(HttpMessageConverter conv, Object obj) {
        try {
            HttpOutputMessage msg = new MockHttpOutputMessage();
            //noinspection unchecked
            conv.write(obj, (MediaType) conv.getSupportedMediaTypes().get(0), msg);
            ByteArrayOutputStream baos = (ByteArrayOutputStream) msg.getBody();
            return new String(baos.toByteArray(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    protected <T> T in(HttpMessageConverter conv, String s, Class<T> type) {
        try {
            //noinspection unchecked
            return (T) conv.read(type, new MockHttpInputMessage(s.getBytes(StandardCharsets.UTF_8)));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
