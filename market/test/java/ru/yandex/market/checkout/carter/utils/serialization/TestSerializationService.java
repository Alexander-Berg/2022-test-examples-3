package ru.yandex.market.checkout.carter.utils.serialization;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.io.input.CharSequenceInputStream;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.mock.http.MockHttpInputMessage;
import org.springframework.mock.http.MockHttpOutputMessage;

import ru.yandex.market.checkout.carter.web.CartListViewModel;
import ru.yandex.market.checkout.carter.web.CartViewModel;
import ru.yandex.market.checkout.carter.web.ResultViewModel;

/**
 * @author Kirill Khromov
 * date: 29/01/2018
 */

public class TestSerializationService {

    private final List<HttpMessageConverter> converters;
    private final ObjectMapper objectMapper;

    public TestSerializationService(List<HttpMessageConverter> converters, ObjectMapper objectMapper) {
        this.converters = converters;
        this.objectMapper = objectMapper;
    }

    public <T> String serializeCarterObject(T obj) {
        return serialize(obj);
    }

    public <T> T deserializeCarterObject(String body, Class<T> clazz) {
        return deserialize(clazz, body);
    }

    public ResultViewModel<CartViewModel> deserializeCarterObjectWithOm(String body) {
        try {
            ResultViewModel<List<CartListViewModel>> result = objectMapper.readValue(body,
                    new TypeReference<ResultViewModel<List<CartListViewModel>>>() {
                    });
            return new ResultViewModel<>(result.getTimestamp(), new CartViewModel(result.getResult()));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private <T> String serialize(T obj) {
        try {
            for (HttpMessageConverter<T> converter : converters) {
                if (converter.canWrite(obj.getClass(), MediaType.APPLICATION_JSON)) {

                    MockHttpOutputMessage outputMessage = new MockHttpOutputMessage();
                    converter.write(obj, null, outputMessage);
                    return outputMessage.getBodyAsString();
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        throw new RuntimeException("Can't find message converter for object " + obj);
    }


    private <T> T deserialize(Class<T> clazz, String body) {
        try {
            for (HttpMessageConverter<T> converter : converters) {
                if (converter.canRead(clazz, MediaType.APPLICATION_JSON)) {

                    return converter.read(clazz, new MockHttpInputMessage(new CharSequenceInputStream(body,
                            StandardCharsets.UTF_8)));
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        throw new RuntimeException("Can't find message converter for object " + clazz);
    }
}

