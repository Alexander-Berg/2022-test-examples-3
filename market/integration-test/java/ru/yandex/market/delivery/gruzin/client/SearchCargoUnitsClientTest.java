package ru.yandex.market.delivery.gruzin.client;

import java.time.Instant;
import java.util.Collection;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;

import ru.yandex.market.delivery.gruzin.model.CargoUnitDto;
import ru.yandex.market.delivery.gruzin.model.CargoUnitDtoFilter;
import ru.yandex.market.delivery.gruzin.model.UnitCargoType;
import ru.yandex.market.delivery.gruzin.model.UnitType;
import ru.yandex.market.delivery.transport_manager.model.enums.SortDirection;

import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.extractFileContent;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.jsonRequestContent;

public class SearchCargoUnitsClientTest extends AbstractClientTest {
    @Autowired
    private GruzinClient gruzinClient;

    @Test
    void searchCargoUnits() {
        mockServer.expect(method(HttpMethod.POST))
            .andExpect(
                requestTo(
                    gruzinApiProperties.getUrl() +
                        "/cargo_units/search?toId=4&sortDirection=DESC&limit=2&fromId=3"
                )
            )
            .andExpect(jsonRequestContent("request/cargo_units/search.json"))
            .andRespond(
                withSuccess()
                    .body(extractFileContent("response/cargo_units/search.json"))
            );

        Collection<CargoUnitDto> response = gruzinClient.search(
            CargoUnitDtoFilter.builder()
                .logisticsPointFromId(100L)
                .logisticsPointToId(200L)
                .cargoUnitType(UnitCargoType.XDOCK)
                .build(),
            2,
            3L,
            4L,
            SortDirection.DESC
        );

        softly.assertThat(response)
            .containsExactlyInAnyOrder(
                CargoUnitDto.builder()
                    .id(11L)
                    .logisticPointFrom(100L)
                    .logisticPointTo(200L)
                    .unitId("WP2")
                    .unitType(UnitType.PALLET)
                    .unitCargoType(UnitCargoType.INTERWAREHOUSE_FIT)
                    .inboundTime(Instant.parse("2021-04-29T10:00:00Z"))
                    .inboundExternalId("lol1")
                    .volume(500)
                    .frozen(false)
                    .subUnitCount(2)
                    .build(),
                CargoUnitDto.builder()
                    .id(8L)
                    .logisticPointFrom(100L)
                    .logisticPointTo(200L)
                    .unitId("WP100")
                    .unitType(UnitType.PALLET)
                    .unitCargoType(UnitCargoType.INTERWAREHOUSE_FIT)
                    .inboundTime(Instant.parse("2021-04-29T10:00:00Z"))
                    .inboundExternalId("lol1")
                    .volume(null)
                    .frozen(false)
                    .subUnitCount(2)
                    .build()
            );
    }
}
