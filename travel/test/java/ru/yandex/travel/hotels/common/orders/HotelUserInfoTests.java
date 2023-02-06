package ru.yandex.travel.hotels.common.orders;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class HotelUserInfoTests {
    private static final ObjectMapper mapper = new ObjectMapper()
            .setPropertyNamingStrategy(PropertyNamingStrategy.SNAKE_CASE);

    @Test
    public void HotelUserInfoDeserializationTests_ok() throws JsonProcessingException {
        String json = "{\"plus_balance\": 100}";
        HotelUserInfo userInfo = mapper.readerFor(HotelUserInfo.class).readValue(json);
        assertThat(userInfo.getPlusBalance()).isEqualTo(100);
    }

    @Test
    public void HotelUserInfoDeserializationTests_isNull() throws JsonProcessingException {
        String json = "{\"plus_balance\": null}";
        HotelUserInfo userInfo = mapper.readerFor(HotelUserInfo.class).readValue(json);
        assertThat(userInfo.getPlusBalance()).isNull();
    }

    @Test
    public void HotelUserInfoDeserializationTests_empty() throws JsonProcessingException {
        String json = "{}";
        HotelUserInfo userInfo = mapper.readerFor(HotelUserInfo.class).readValue(json);
        assertThat(userInfo.getPlusBalance()).isNull();
    }
}
