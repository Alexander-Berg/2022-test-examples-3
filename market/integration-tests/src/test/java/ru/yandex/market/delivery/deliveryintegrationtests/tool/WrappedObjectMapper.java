package ru.yandex.market.delivery.deliveryintegrationtests.tool;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.type.CollectionType;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import lombok.SneakyThrows;

public class WrappedObjectMapper {

    private static WrappedObjectMapper instance;

    private static XmlMapper mapper;

    public static synchronized WrappedObjectMapper getInstance() {
        if (instance == null) {
            instance = new WrappedObjectMapper();
            mapper =  new XmlMapper();
            mapper.setSerializationInclusion(JsonInclude.Include.NON_EMPTY);
        }
        return instance;
    }

    public static XmlMapper getMapper() {
        return mapper;
    }

    @SneakyThrows
    public <T> String serialize(T t) {
        return mapper.writeValueAsString(t);
    }

    @SneakyThrows
    public <T> T deserialize(String value, Class<T> tClass) {
        return mapper.readValue(value, tClass);
    }

    @SneakyThrows
    public <T> List<T> deserializeList(String content, Class<T> targetClass) {
        CollectionType collectionType = mapper.getTypeFactory().constructCollectionType(List.class, targetClass);
        return mapper.readValue(content, collectionType);
    }

}
