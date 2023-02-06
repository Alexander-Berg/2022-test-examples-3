package ru.yandex.travel.hotels.common.partners.dolphin.model;

import java.io.IOException;
import java.nio.charset.Charset;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.google.common.io.Resources;
import org.junit.Test;

import ru.yandex.travel.commons.jackson.MoneySerializersModule;
import ru.yandex.travel.hotels.common.orders.DolphinHotelItinerary;

import static org.assertj.core.api.Assertions.assertThat;

public class ItineraryTest {
    private ObjectMapper objectMapper;

    public ItineraryTest() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.registerModule(new Jdk8Module());
        objectMapper.registerModule(new MoneySerializersModule());
        objectMapper.setPropertyNamingStrategy(PropertyNamingStrategy.SNAKE_CASE);
    }

    @Test
    public void testSequentialDeserialization() throws IOException {
        var json = Resources.toString(Resources.getResource("itineraries/dolphin_itinerary.json"),
                Charset.defaultCharset());
        DolphinHotelItinerary it1 = objectMapper.readerFor(DolphinHotelItinerary.class).readValue(json);
        DolphinHotelItinerary it2 = objectMapper.readerFor(DolphinHotelItinerary.class).readValue(json);
        assertThat(it1).isEqualTo(it2);
    }

}
