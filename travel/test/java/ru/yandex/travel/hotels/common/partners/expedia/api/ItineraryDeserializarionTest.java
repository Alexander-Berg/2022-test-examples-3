package ru.yandex.travel.hotels.common.partners.expedia.api;

import java.io.IOException;
import java.nio.charset.Charset;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.io.Resources;
import org.junit.Test;

import ru.yandex.travel.hotels.common.partners.expedia.DefaultExpediaClient;
import ru.yandex.travel.hotels.common.partners.expedia.model.booking.Itinerary;

import static org.assertj.core.api.Assertions.assertThat;

public class ItineraryDeserializarionTest {

    @Test
    public void TestItinerary() throws IOException {
        String data = Resources.toString(Resources.getResource("expediaResponses/ItineraryWithValueAdds.json"), Charset.defaultCharset());
        ObjectMapper mapper = DefaultExpediaClient.createObjectMapper();
        Itinerary i = mapper.readerFor(Itinerary.class).readValue(data);
        assertThat(i).isNotNull();
    }
}
