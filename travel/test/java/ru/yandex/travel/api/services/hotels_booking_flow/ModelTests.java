package ru.yandex.travel.api.services.hotels_booking_flow;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.io.Resources;
import org.junit.Test;

import ru.yandex.travel.hotels.models.booking_flow.Offer;

import static org.assertj.core.api.Assertions.assertThat;

public class ModelTests {
    private ObjectMapper mapper = TestHelper.createMapper();

    @Test
    public void testDeserialize() throws IOException {
        String json = Resources.toString(Resources.getResource("offer.json"), StandardCharsets.UTF_8);
        var offer = mapper.readValue(json, Offer.class);
        assertThat(offer).isNotNull();
        assertThat(offer.getPromoCampaignsInfo().getTaxi2020().isEligible()).isFalse();
    }
}
