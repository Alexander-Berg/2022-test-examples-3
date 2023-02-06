package ru.yandex.travel.api.services.hotels_booking_flow;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import ru.yandex.travel.api.config.common.JacksonConfiguration;

public class TestHelper {
    public static ObjectMapper createMapper() {
        var jacksonConfig = new JacksonConfiguration();
        var mapper = new ObjectMapper();
        mapper.registerModule(new Jdk8Module());
        mapper.registerModule(new JavaTimeModule());
        mapper.setPropertyNamingStrategy(PropertyNamingStrategy.SNAKE_CASE);
        mapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
        mapper.registerModule(jacksonConfig.moneySerializersModule());
        return mapper;
    }
}
