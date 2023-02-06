package ru.yandex.market.mcrm.utils;

import java.util.Optional;

import ru.yandex.market.mcrm.utils.serialize.ObjectMapperFactory;
import ru.yandex.market.mcrm.utils.serialize.ObjectSerializeService;
import ru.yandex.market.mcrm.utils.serialize.ObjectSerializeServiceImpl;

public class SerializationUtils {

    public static ObjectSerializeService defaultObjectSerializeService() {
        var mapper = new ObjectMapperFactory(Optional.empty()).defaultObjectMapper();
        return new ObjectSerializeServiceImpl(mapper);
    }
}
