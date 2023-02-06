package ru.yandex.market.jmf.utils;

import java.util.Optional;

import com.fasterxml.jackson.databind.ObjectMapper;

import ru.yandex.market.jmf.utils.serialize.ObjectMapperFactory;
import ru.yandex.market.jmf.utils.serialize.ObjectSerializeService;
import ru.yandex.market.jmf.utils.serialize.ObjectSerializeServiceImpl;
import ru.yandex.market.jmf.utils.serialize.SerializationConfiguration;

public class SerializationUtils {

    public static ObjectMapper defaultObjectMapper() {
        return new SerializationConfiguration()
                .defaultObjectMapper(new ObjectMapperFactory(Optional.empty()));
    }

    public static ObjectSerializeService defaultObjectSerializeService() {
        return new ObjectSerializeServiceImpl(SerializationUtils.defaultObjectMapper());
    }
}
