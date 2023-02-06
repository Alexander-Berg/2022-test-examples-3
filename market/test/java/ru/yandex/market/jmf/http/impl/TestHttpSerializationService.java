package ru.yandex.market.jmf.http.impl;

import org.springframework.stereotype.Component;

import ru.yandex.market.jmf.utils.serialize.SerializationService;

@Component("TestHttpSerializationService")
public class TestHttpSerializationService implements SerializationService {

    @Override
    public byte[] serialize(Object value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public <T> T deserialize(byte[] value, Class<T> type) {
        return (T)new HttpClientImplTest.TestResponse("custom-value");
    }
}
