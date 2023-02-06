package ru.yandex.market.logistics.management.client;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;

import ru.yandex.market.logistics.management.entity.response.LocationZoneResponse;

import static org.springframework.http.HttpStatus.OK;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static ru.yandex.market.logistics.management.client.util.TestUtil.jsonResource;

class LmsClientGetLocationZonesTest extends AbstractClientTest {
    private static final String URI = "/externalApi/location-zone/location/";
    private static final Long MOSCOW_LOCATION_ID  = 213L;
    private static final Long NOVOSIBIRSK_LOCATION_ID = 65L;
    private static final Long UNKNOWN_LOCATION_ID = -1L;

    @Test
    void getMultipleLocationZones() {
        mockServer.expect(requestTo(uri + URI + MOSCOW_LOCATION_ID))
            .andExpect(method(HttpMethod.GET))
            .andRespond(withStatus(OK)
                .contentType(APPLICATION_JSON)
                .body(jsonResource("data/controller/locationZone/multiple_location_zones.json"))
            );

        List<LocationZoneResponse> actualResponse = client.getLocationZonesByLocationId(MOSCOW_LOCATION_ID);

        softly.assertThat(actualResponse).containsExactly(
            new LocationZoneResponse.Builder().locationId(213L).locationZoneId(1L).name("Центр").build(),
            new LocationZoneResponse.Builder().locationId(213L).locationZoneId(2L).name("У МКАДа").build()
        );
    }

    @Test
    void getSingleLocationZone() {
        mockServer.expect(requestTo(uri + URI + NOVOSIBIRSK_LOCATION_ID))
            .andExpect(method(HttpMethod.GET))
            .andRespond(withStatus(OK)
                .contentType(APPLICATION_JSON)
                .body(jsonResource("data/controller/locationZone/single_location_zone.json"))
            );

        List<LocationZoneResponse> actualResponse = client.getLocationZonesByLocationId(NOVOSIBIRSK_LOCATION_ID);

        softly.assertThat(actualResponse).containsExactly(
            new LocationZoneResponse.Builder().locationId(65L).locationZoneId(3L).name("Центр").build()
        );
    }

    @Test
    void getNoLocationZones() {
        mockServer.expect(requestTo(uri + URI + UNKNOWN_LOCATION_ID))
            .andExpect(method(HttpMethod.GET))
            .andRespond(withStatus(OK)
                .contentType(APPLICATION_JSON)
                .body(jsonResource("data/controller/empty_entities.json"))
            );

        List<LocationZoneResponse> actualResponse = client.getLocationZonesByLocationId(UNKNOWN_LOCATION_ID);

        softly.assertThat(actualResponse).isEmpty();
    }

}
