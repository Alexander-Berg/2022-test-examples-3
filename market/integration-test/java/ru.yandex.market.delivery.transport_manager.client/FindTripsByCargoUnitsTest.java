package ru.yandex.market.delivery.transport_manager.client;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;

import ru.yandex.market.delivery.transport_manager.model.dto.trip.CargoUnitIdWithDirection;

import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.extractFileContent;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.jsonRequestContent;

public class FindTripsByCargoUnitsTest extends AbstractClientTest {
    @Autowired
    private TransportManagerClient transportManagerClient;

    @Test
    void getMostRecentTripsByCargoUnitIdsWithDirection() {
        mockServer.expect(method(HttpMethod.POST))
            .andExpect(requestTo(tmApiProperties.getUrl() + "/trip/trip-ids-by-cargo-units"))
            .andExpect(jsonRequestContent("request/cargo_units/trip_id_by_cargo_units.json"))
            .andRespond(withSuccess(
                extractFileContent("response/cargo_units/trip_id_by_cargo_units.json"),
                MediaType.APPLICATION_JSON
            ));

        CargoUnitIdWithDirection id1 = new CargoUnitIdWithDirection("DRP0001", 100000172L, 100000145L);
        CargoUnitIdWithDirection id2 = new CargoUnitIdWithDirection("BOX00001", 100000172L, 100000145L);

        Map<CargoUnitIdWithDirection, String> includedOutboundIdsInTrips =
            transportManagerClient.getMostRecentTripsByCargoUnitIdsWithDirection(List.of(id1, id2));

        softly.assertThat(includedOutboundIdsInTrips)
            .isEqualTo(Map.of(
                id1, "TMT00001",
                id2, "TMT00001"
            ));
    }
}
