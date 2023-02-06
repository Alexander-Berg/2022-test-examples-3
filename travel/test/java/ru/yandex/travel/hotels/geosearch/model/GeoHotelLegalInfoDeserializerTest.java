package ru.yandex.travel.hotels.geosearch.model;

import java.io.IOException;
import java.net.URL;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class GeoHotelLegalInfoDeserializerTest {
    private static URL responseText;

    @BeforeClass
    public static void loadResponse() {
        responseText = GeoHotelLegalInfoDeserializerTest.class.getClassLoader().getResource("hotelLegalInfo.json");
    }

    @Test
    public void testResponseDeserializationTest() throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        GeoHotelLegalInfo li = mapper.readerFor(GeoHotelLegalInfo.class).readValue(responseText);
        assertThat(li.getAddress()).isEqualTo("121059 МОСКВА ГОРОД ПЛОЩАДЬ ЕВРОПЫ 2");
        assertThat(li.getName()).isEqualTo("ООО \"СЛАВЯНСКАЯ\"");
        assertThat(li.getInn()).isEqualTo("7730001183");
        assertThat(li.getOgrn()).isEqualTo("1027739155620");
    }
}
