package ru.yandex.market.crm;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import ru.yandex.market.crm.serialization.AbstractJacksonDeserializer;
import ru.yandex.market.crm.serialization.JsonDeserializer;

public class TestJsonDeserializer extends AbstractJacksonDeserializer implements JsonDeserializer {

    public TestJsonDeserializer() {
        super(new ObjectMapper()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                .registerModule(new JavaTimeModule())
                .disable(MapperFeature.AUTO_DETECT_FIELDS)
                .disable(MapperFeature.AUTO_DETECT_GETTERS)
                .disable(MapperFeature.AUTO_DETECT_SETTERS)
                .disable(MapperFeature.AUTO_DETECT_CREATORS)
                .disable(MapperFeature.AUTO_DETECT_IS_GETTERS)
                .setSerializationInclusion(JsonInclude.Include.NON_NULL)
        );
    }
}
