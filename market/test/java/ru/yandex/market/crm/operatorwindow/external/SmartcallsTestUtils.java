package ru.yandex.market.crm.operatorwindow.external;

import java.util.Optional;

import ru.yandex.market.crm.operatorwindow.external.smartcalls.SmartCallsSerializationHelper;
import ru.yandex.market.crm.operatorwindow.serialization.JsonTreeParser;
import ru.yandex.market.crm.operatorwindow.serialization.RawJsonParser;
import ru.yandex.market.jmf.utils.serialize.CustomJsonDeserializer;
import ru.yandex.market.jmf.utils.serialize.CustomJsonSerializer;
import ru.yandex.market.jmf.utils.serialize.ObjectMapperFactory;

public class SmartcallsTestUtils {

    public static SmartCallsSerializationHelper createSerializationHelper() {
        ObjectMapperFactory objectMapperFactorty = new ObjectMapperFactory(Optional.empty());

        CustomJsonDeserializer deserializer
                = new CustomJsonDeserializer(objectMapperFactorty);

        CustomJsonSerializer serializer
                = new CustomJsonSerializer(objectMapperFactorty);

        JsonTreeParser rawJsonParser = new RawJsonParser(objectMapperFactorty);

        return new SmartCallsSerializationHelper(
                serializer,
                deserializer,
                rawJsonParser
        );

    }

    public static JsonTreeParser getJsonTreeParser() {
        ObjectMapperFactory objectMapperFactorty = new ObjectMapperFactory(Optional.empty());
        return new RawJsonParser(objectMapperFactorty);
    }
}
