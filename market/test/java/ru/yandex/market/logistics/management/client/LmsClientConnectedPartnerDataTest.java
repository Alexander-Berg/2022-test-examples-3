package ru.yandex.market.logistics.management.client;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;

import ru.yandex.market.logistics.management.client.util.TestUtil;
import ru.yandex.market.logistics.management.entity.request.partner.ConnectedPartnerDataRequest;
import ru.yandex.market.logistics.management.entity.response.partner.ConnectedPartnerDataDto;
import ru.yandex.market.logistics.management.entity.type.PartnerType;
import ru.yandex.market.logistics.management.entity.type.ShipmentType;

import static org.springframework.http.HttpStatus.OK;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static ru.yandex.market.logistics.management.client.util.TestUtil.getBuilder;
import static ru.yandex.market.logistics.management.client.util.TestUtil.jsonResource;

public class LmsClientConnectedPartnerDataTest extends AbstractClientTest {
    @Test
    @DisplayName("Получение информации о количестве магазинов, подключенных к точкам сдачи")
    void getConnectedPartnerData() {
        mockServer.expect(requestTo(
            getBuilder(uri, "/externalApi/connected-partner-data").toUriString()
        ))
            .andExpect(method(HttpMethod.PUT))
            .andExpect(TestUtil.jsonContent("data/controller/connected_partner_request.json"))
            .andRespond(
                withStatus(OK)
                    .contentType(APPLICATION_JSON)
                    .body(jsonResource("data/controller/connected_partner_data.json"))
            );

        List<ConnectedPartnerDataDto> connectedPartnerDto = client.getConnectedPartnerData(
            new ConnectedPartnerDataRequest(
                List.of(1L, 2L),
                Set.of(PartnerType.DROPSHIP)
            )
        );

        softly.assertThat(connectedPartnerDto).contains(getConnectedPartnerDataDto());
    }

    @Test
    @DisplayName("Получение информации о количестве магазинов, подключенных к точкам сдачи - пустой результат")
    void getConnectedPartnerDataEmpty() {
        mockServer.expect(requestTo(
            getBuilder(uri, "/externalApi/connected-partner-data").toUriString()
        ))
            .andExpect(method(HttpMethod.PUT))
            .andExpect(TestUtil.jsonContent("data/controller/connected_partner_request_without_ids.json"))
            .andRespond(
                withStatus(OK)
                    .contentType(APPLICATION_JSON)
                    .body(jsonResource("data/controller/empty_entities.json"))
            );

        List<ConnectedPartnerDataDto> connectedPartnerDto = client.getConnectedPartnerData(
            new ConnectedPartnerDataRequest(
                Set.of(),
                Set.of(PartnerType.DROPSHIP)
            )
        );

        softly.assertThat(connectedPartnerDto).isEmpty();
    }

    private ConnectedPartnerDataDto getConnectedPartnerDataDto() {
        return ConnectedPartnerDataDto.builder()
            .locationId(2)
            .logisticsPointId(1L)
            .shipmentType(ShipmentType.IMPORT)
            .partnerType(PartnerType.DROPSHIP)
            .connectedShopsCount(10L)
            .build();
    }
}
