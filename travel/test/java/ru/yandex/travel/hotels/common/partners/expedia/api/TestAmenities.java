package ru.yandex.travel.hotels.common.partners.expedia.api;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.io.Resources;
import org.junit.Before;
import org.junit.Test;

import ru.yandex.travel.hotels.common.partners.expedia.DefaultExpediaClient;
import ru.yandex.travel.hotels.common.partners.expedia.KnownAmenity;
import ru.yandex.travel.hotels.common.partners.expedia.model.content.PropertyContent;

import static org.assertj.core.api.Assertions.assertThat;

public class TestAmenities {
    private Map<String, PropertyContent> propertyContents;

    @Before
    public void setUp() throws IOException {
        String data = Resources.toString(Resources.getResource("expediaResponses/PropertyContentResponse.json"), Charset.defaultCharset());
        ObjectMapper mapper = DefaultExpediaClient.createObjectMapper();
        propertyContents = mapper.readerFor(mapper.getTypeFactory().constructMapLikeType(HashMap.class, String.class, PropertyContent.class)).readValue(data);
    }

    @Test
    public void testRateAmenities() {
        Set<KnownAmenity> amenities = KnownAmenity.fromPropertyContent(propertyContents.get("26199"), "238100378");
        assertThat(amenities).contains(KnownAmenity.BREAKFAST_BUFFET);
        assertThat(amenities).hasSize(1);
        amenities = KnownAmenity.fromPropertyContent(propertyContents.get("26199"), "238100504");
        assertThat(amenities).contains(KnownAmenity.FREE_WIRELESS_INTERNET);
        assertThat(amenities).contains(KnownAmenity.BREAKFAST_BUFFET);
        assertThat(amenities).hasSize(2);
    }
}
