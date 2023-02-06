package ru.yandex.market.delivery.transport_manager.client;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;

import ru.yandex.market.delivery.transport_manager.model.dto.trip.TripIncludedOutboundsAddDto;
import ru.yandex.market.delivery.transport_manager.model.dto.trip.TripIncludedOutboundsRemoveDto;

import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.jsonRequestContent;

public class TripIncludedOutboundsModificationTest extends AbstractClientTest {

    @Autowired
    private TransportManagerClient transportManagerClient;

    @Test
    void addTripIncludedOutbounds() {
        mockServer.expect(method(HttpMethod.POST))
            .andExpect(requestTo(tmApiProperties.getUrl() + "/trip/TMT1/included-outbounds"))
            .andExpect(jsonRequestContent("request/trip/add_included_outbounds.json"))
            .andRespond(withSuccess());

        transportManagerClient.addTripIncludedOutbounds(new TripIncludedOutboundsAddDto(
            "TMT1",
            List.of("TMU0001", "TMU0002")
        ));
    }

    @Test
    void removeTripIncludedOutbounds() {
        mockServer.expect(method(HttpMethod.DELETE))
                .andExpect(requestTo(tmApiProperties.getUrl() + "/trip/included-outbounds"))
                .andExpect(jsonRequestContent("request/trip/remove_included_outbounds.json"))
                .andRespond(withSuccess());

        transportManagerClient.removeTripIncludedOutbounds(new TripIncludedOutboundsRemoveDto(
                List.of("TMT1", "TMT2"),
                List.of("TMU0001", "TMU0002")
        ));
    }
}
