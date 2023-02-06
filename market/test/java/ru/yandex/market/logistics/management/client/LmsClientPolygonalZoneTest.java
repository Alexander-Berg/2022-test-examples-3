package ru.yandex.market.logistics.management.client;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;

import ru.yandex.market.logistics.management.entity.request.polygonalZone.PolygonalLocationZoneDto;

import static org.springframework.http.HttpStatus.OK;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static ru.yandex.market.logistics.management.client.util.TestUtil.jsonResource;

@DisplayName("Сохранение полигональных зон")
class LmsClientPolygonalZoneTest extends AbstractClientTest {
    @Test
    void saveZones() {
        PolygonalLocationZoneDto dto = PolygonalLocationZoneDto.newBuilder()
            .zones(List.of(
                PolygonalLocationZoneDto.Zone
                    .builder()
                    .name("zone1")
                    .enabled(true)
                    .externalId("ext1")
                    .coordinates(List.of(List.of(
                        PolygonalLocationZoneDto.Point.of(BigDecimal.ONE, BigDecimal.valueOf(2)))
                    ))
                    .build()
            ))
            .build();
        mockServer.expect(requestTo(uri + "/externalApi/partner/1/polygonal-location-zone"))
            .andExpect(content().json(jsonResource("data/controller/polygonalZone/request.json")))
            .andExpect(method(HttpMethod.PUT))
            .andRespond(withStatus(OK));

        client.savePolygonalZones(1L, dto);
    }
}
