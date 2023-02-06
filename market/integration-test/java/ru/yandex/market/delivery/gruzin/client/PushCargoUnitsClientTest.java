package ru.yandex.market.delivery.gruzin.client;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;

import ru.yandex.market.delivery.gruzin.model.CargoUnitCreateDto;
import ru.yandex.market.delivery.gruzin.model.CargoUnitsCreateDto;
import ru.yandex.market.delivery.gruzin.model.UnitCargoType;
import ru.yandex.market.delivery.gruzin.model.UnitType;
import ru.yandex.market.delivery.gruzin.model.WarehouseId;
import ru.yandex.market.delivery.gruzin.model.WarehouseIdType;

import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.jsonRequestContent;

public class PushCargoUnitsClientTest extends AbstractClientTest {
    @Autowired
    private GruzinClient gruzinClient;

    @Test
    void pushCargoUnits() {
        mockServer.expect(method(HttpMethod.PUT))
            .andExpect(requestTo(gruzinApiProperties.getUrl() + "/cargo_units/state"))
            .andExpect(jsonRequestContent("request/cargo_units/push_state.json"))
            .andRespond(withSuccess());

        gruzinClient.pushCargoUnits(new CargoUnitsCreateDto()
            .setPartnerId(145L)
            .setSnapshotDateTime(Instant.parse("2017-09-11T07:30:00Z"))
            .setTargetWarehouse(new WarehouseId(WarehouseIdType.LOGISTIC_POINT, 100000172L))
            .setUnits(List.of(
                new CargoUnitCreateDto()
                    .setId("DRP0001")
                    .setUnitType(UnitType.PALLET)
                    .setUnitCargoType(UnitCargoType.INTERWAREHOUSE_FIT)
                    .setCreationDate(Instant.parse("2017-09-10T17:00:00Z"))
                    .setPlannedOutboundDate(Instant.parse("2017-09-11T17:00:00Z")),
                new CargoUnitCreateDto()
                    .setId("BOX0001")
                    .setParentId("DRP0001")
                    .setUnitType(UnitType.BOX)
                    .setUnitCargoType(UnitCargoType.INTERWAREHOUSE_FIT)
                    .setCreationOutboundId("TMU12345")
                    .setCreationDate(Instant.parse("2017-09-11T07:16:01Z"))
                    .setPlannedOutboundDate(Instant.parse("2017-09-11T17:00:00Z"))
            )));
    }
}
