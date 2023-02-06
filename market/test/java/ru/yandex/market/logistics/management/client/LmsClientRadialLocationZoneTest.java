package ru.yandex.market.logistics.management.client;

import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.HttpMethod;

import ru.yandex.market.logistics.management.entity.request.radialZone.LogisticPointRadialLocationZonesCreateDto;
import ru.yandex.market.logistics.management.entity.request.radialZone.RadialLocationZoneFilter;
import ru.yandex.market.logistics.management.entity.response.partner.RadialLocationZoneResponse;

import static org.springframework.http.HttpStatus.OK;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static ru.yandex.market.logistics.management.client.util.TestUtil.jsonResource;

public class LmsClientRadialLocationZoneTest extends AbstractClientTest {
    private static final String URI = "/externalApi/radial-location-zone";

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void getMultipleRadialZones() throws JsonProcessingException {
        RadialLocationZoneFilter filter = RadialLocationZoneFilter.newBuilder().build();
        mockServer.expect(requestTo(uri + URI + "/search"))
            .andExpect(method(HttpMethod.PUT))
            .andExpect(content().string(objectMapper.writeValueAsString(filter)))
            .andRespond(withStatus(OK)
                .contentType(APPLICATION_JSON)
                .body(jsonResource("data/controller/radialZone/multiple_radial_location_zone.json"))
            );

        List<RadialLocationZoneResponse> actualResponse = client.getRadialLocationZones(filter);

        softly.assertThat(actualResponse).containsExactlyInAnyOrder(
            RadialLocationZoneResponse.newBuilder()
                .id(1L)
                .name("Первая зона МСК")
                .regionId(213)
                .radius(2000L)
                .deliveryDuration(30L)
                .isPrivate(true)
                .build(),
            RadialLocationZoneResponse.newBuilder()
                .id(2L)
                .name("Вторая зона МСК")
                .regionId(213)
                .radius(4000L)
                .deliveryDuration(60L)
                .isPrivate(false)
                .build()
        );
    }

    @Test
    void getSingleRadialZone() throws JsonProcessingException {
        RadialLocationZoneFilter filter = RadialLocationZoneFilter.newBuilder().build();
        mockServer.expect(requestTo(uri + URI + "/search"))
            .andExpect(method(HttpMethod.PUT))
            .andExpect(content().string(objectMapper.writeValueAsString(filter)))
            .andRespond(withStatus(OK)
                .contentType(APPLICATION_JSON)
                .body(jsonResource("data/controller/radialZone/single_radial_location_zone.json"))
            );

        List<RadialLocationZoneResponse> actualResponse = client.getRadialLocationZones(filter);

        softly.assertThat(actualResponse).containsExactly(
            RadialLocationZoneResponse.newBuilder()
                .id(1L)
                .name("Первая зона МСК")
                .regionId(213)
                .radius(2000L)
                .deliveryDuration(30L)
                .isPrivate(true)
                .build()
        );
    }

    @Test
    void getNoRadialZones() throws JsonProcessingException {
        RadialLocationZoneFilter filter = RadialLocationZoneFilter.newBuilder()
            .zoneIds(Set.of())
            .build();
        mockServer.expect(requestTo(uri + URI + "/search"))
            .andExpect(method(HttpMethod.PUT))
            .andExpect(content().string(objectMapper.writeValueAsString(filter)))
            .andRespond(withStatus(OK)
                .contentType(APPLICATION_JSON)
                .body(jsonResource("data/controller/empty_entities.json"))
            );

        List<RadialLocationZoneResponse> actualResponse = client.getRadialLocationZones(filter);

        softly.assertThat(actualResponse).isEmpty();
    }

    @Test
    void linkPointAndZones() throws JsonProcessingException {
        LogisticPointRadialLocationZonesCreateDto dto = LogisticPointRadialLocationZonesCreateDto.newBuilder()
            .zoneIds(Set.of(1L, 2L))
            .logisticPointId(1L)
            .build();
        mockServer.expect(requestTo(uri + URI + "/link"))
            .andExpect(method(HttpMethod.POST))
            .andExpect(content().string(objectMapper.writeValueAsString(dto)))
            .andRespond(withStatus(OK)
                .contentType(APPLICATION_JSON)
                .body(jsonResource("data/controller/empty_entities.json"))
            );

        client.linkRadialZonesToLogisticPoint(1L, Set.of(1L, 2L));
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("getInvalidArguments")
    void linkPointAndZoneFail(
        @SuppressWarnings("unused") String caseName,
        LogisticPointRadialLocationZonesCreateDto dto,
        String responsePath
    ) throws JsonProcessingException {
        mockServer.expect(requestTo(uri + URI + "/link"))
            .andExpect(method(HttpMethod.POST))
            .andExpect(content().string(objectMapper.writeValueAsString(dto)))
            .andRespond(withStatus(OK)
                .contentType(APPLICATION_JSON)
                .body(jsonResource(responsePath))
            );

        client.linkRadialZonesToLogisticPoint(dto.getLogisticPointId(), dto.getZoneIds());
    }

    private static Stream<Arguments> getInvalidArguments() {
        return Stream.of(
            Arguments.of(
                "Нет идентификатора склада",
                LogisticPointRadialLocationZonesCreateDto.newBuilder().zoneIds(Set.of(1L)).build(),
                "data/controller/radialZone/link_no_point_id.json"
            ),
            Arguments.of(
                "Нет идентификатора зоны",
                LogisticPointRadialLocationZonesCreateDto.newBuilder().logisticPointId(1L).build(),
                "data/controller/radialZone/link_no_zone_id.json"
            ),
            Arguments.of(
                "Нет идентификатора зоны и склада",
                LogisticPointRadialLocationZonesCreateDto.newBuilder().build(),
                "data/controller/radialZone/link_no_zone_id_and_point_id.json"
            )
        );
    }

}
