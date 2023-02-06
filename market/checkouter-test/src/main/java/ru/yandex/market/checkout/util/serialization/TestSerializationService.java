package ru.yandex.market.checkout.util.serialization;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.function.Supplier;

import javax.annotation.PostConstruct;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.io.input.CharSequenceInputStream;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.TestComponent;
import org.springframework.http.converter.GenericHttpMessageConverter;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.mock.http.MockHttpInputMessage;
import org.springframework.mock.http.MockHttpOutputMessage;

/**
 * @author Nikolai Iusiumbeli
 * date: 14/07/2017
 */
@TestComponent
public class TestSerializationService {

    @Autowired
    @Qualifier("checkouterJsonMessageConverter")
    private GenericHttpMessageConverter checkouterJsonMessageConverter;
    @Autowired
    @Qualifier("pushApiTestMappingXmlMessageConverter")
    private HttpMessageConverter pushApiTestMappingXmlMessageConverter;
    @Autowired
    @Qualifier("objectMapperHolder")
    private Supplier<ObjectMapper> marketLoyaltyObjectMapper;
    @Autowired
    @Qualifier("objectMapperHolder")
    private Supplier<ObjectMapper> antifraudObjectMapper;
    @Autowired
    @Qualifier("yaUslugiMessageConverter")
    private MappingJackson2HttpMessageConverter yaUslugiMessageConverter;

    private MappingJackson2HttpMessageConverter loyaltyMessageConverter;

    private MappingJackson2HttpMessageConverter antifraudMessageConverter;

    @PostConstruct
    private void init() {
        loyaltyMessageConverter = new MappingJackson2HttpMessageConverter(marketLoyaltyObjectMapper.get());
        antifraudMessageConverter = new MappingJackson2HttpMessageConverter(antifraudObjectMapper.get());

        loyaltyMessageConverter.setDefaultCharset(StandardCharsets.UTF_8);
        antifraudMessageConverter.setDefaultCharset(StandardCharsets.UTF_8);
    }

    public <T> String serializeCheckouterObject(T obj) {
        return serialize(obj, checkouterJsonMessageConverter);
    }

    public <T> String serializePushApiObject(T obj) {
        return serialize(obj, pushApiTestMappingXmlMessageConverter);
    }

    public <T> String serializeLoyaltyObject(T obj) {
        return serialize(obj, loyaltyMessageConverter);
    }

    public <T> String serializeAntifraudObject(T obj) {
        return serialize(obj, antifraudMessageConverter);
    }

    public <T> String serializeYaUslugiObject(T obj) {
        return serialize(obj, yaUslugiMessageConverter);
    }

    public <T> T deserializeCheckouterObject(String body, Class<T> clazz) {
        return deserialize(clazz, checkouterJsonMessageConverter, body);
    }

    public <T> T deserializeCheckouterObject(String body, Type type, Class<?> contextClass) {
        return deserialize(type, contextClass, checkouterJsonMessageConverter, body);
    }

    public <T> T deserializePushApiObject(String body, Class<T> clazz) {
        return deserialize(clazz, pushApiTestMappingXmlMessageConverter, body);
    }

    public <T> T deserializeLoyaltyObject(String body, Class<T> clazz) {
        return deserialize(clazz, loyaltyMessageConverter, body);
    }

    public <T> T deserializeAntifraudObject(String body, Class<T> clazz) {
        return deserialize(clazz, antifraudMessageConverter, body);
    }

    public <T> T deserializeYaUslugiObject(String body, Class<T> clazz) {
        return deserialize(clazz, yaUslugiMessageConverter, body);
    }

    private <T> String serialize(T obj, HttpMessageConverter converter) {
        try {
            MockHttpOutputMessage outputMessage = new MockHttpOutputMessage();
            converter.write(obj, null, outputMessage);
            return outputMessage.getBodyAsString();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


    private <T> T deserialize(Class<T> clazz, HttpMessageConverter converter, String body) {
        try {
            return (T) converter.read(clazz, new MockHttpInputMessage(new CharSequenceInputStream(body,
                    StandardCharsets.UTF_8)));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private <T> T deserialize(Type type, Class contextClass, GenericHttpMessageConverter converter, String body) {
        try {
            return (T) converter.read(type, contextClass, new MockHttpInputMessage(new CharSequenceInputStream(body,
                    StandardCharsets.UTF_8)));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
