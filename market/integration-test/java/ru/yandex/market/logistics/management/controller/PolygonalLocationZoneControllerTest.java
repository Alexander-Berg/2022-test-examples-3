package ru.yandex.market.logistics.management.controller;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

import javax.annotation.Nonnull;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import net.javacrumbs.jsonunit.core.Option;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

import ru.yandex.market.logistics.management.AbstractContextualTest;
import ru.yandex.market.logistics.management.entity.request.polygonalZone.PolygonalLocationZoneDto;
import ru.yandex.market.logistics.management.util.TestUtil;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DatabaseSetup("/data/controller/polygonZone/prepare.xml")
public class PolygonalLocationZoneControllerTest extends AbstractContextualTest {
    @Test
    @DisplayName("Успешное сохранение полигональных зон")
    @ExpectedDatabase(
        value = "/data/controller/polygonZone/success.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void findShipmentSettingsSuccess() throws Exception {
        PolygonalLocationZoneDto dto = PolygonalLocationZoneDto.newBuilder()
            .zones(List.of(
                getValidZone().build(),
                PolygonalLocationZoneDto.Zone
                    .builder()
                    .name("name2-new")
                    .enabled(true)
                    .externalId("zone2")
                    .coordinates(List.of(List.of(
                        PolygonalLocationZoneDto.Point.of(BigDecimal.valueOf(3L), BigDecimal.valueOf(4L)))
                    ))
                    .build(),
                PolygonalLocationZoneDto.Zone
                    .builder()
                    .name("name4")
                    .enabled(false)
                    .externalId("zone4")
                    .coordinates(List.of(List.of(
                        PolygonalLocationZoneDto.Point.of(BigDecimal.valueOf(7L), BigDecimal.valueOf(8L)))
                    ))
                    .build(),
                PolygonalLocationZoneDto.Zone
                    .builder()
                    .name("name5")
                    .enabled(true)
                    .externalId("zone5")
                    .coordinates(List.of(List.of(
                        PolygonalLocationZoneDto.Point.of(BigDecimal.valueOf(9L), BigDecimal.TEN))
                    ))
                    .build()
            ))
            .build();
        saveZones(1L, dto)
            .andExpect(status().isOk())
            .andExpect(TestUtil.noContent());
    }

    @Test
    @DisplayName("Ошибка сохранения полигональных зон - несуществующий партнер")
    @ExpectedDatabase(
        value = "/data/controller/polygonZone/prepare.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void findShipmentSettingsFailNoPartner() throws Exception {
        saveZones(
            2L,
            PolygonalLocationZoneDto
                .newBuilder()
                .zones(List.of(getValidZone().build()))
                .build()
        )
            .andExpect(status().isNotFound())
            .andExpect(status().reason("Can't find Partner with id=2"))
            .andExpect(TestUtil.noContent());
    }

    private static PolygonalLocationZoneDto.Zone.ZoneBuilder getValidZone() {
        return PolygonalLocationZoneDto.Zone
            .builder()
            .name("name1")
            .enabled(true)
            .externalId("zone1")
            .coordinates(List.of(List.of(
                PolygonalLocationZoneDto.Point.of(BigDecimal.ONE, BigDecimal.valueOf(2L)),
                PolygonalLocationZoneDto.Point.of(BigDecimal.valueOf(3L), BigDecimal.valueOf(4L))
            )));
    }

    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @DisplayName("Ошибка сохранения полигональных зон - невалидный запрос")
    @MethodSource("validationArgs")
    void findShipmentSettingsFail(
        @SuppressWarnings("unused") String name,
        PolygonalLocationZoneDto dto,
        String response
    ) throws Exception {
        saveZones(1L, dto)
            .andExpect(status().isBadRequest())
            .andExpect(TestUtil.testJson(response, Option.IGNORING_EXTRA_FIELDS, Option.IGNORING_ARRAY_ORDER));
    }

    @Nonnull
    private static Stream<Arguments> validationArgs() {
        return Stream.of(
            Arguments.of(
                "Не указаны зоны",
                PolygonalLocationZoneDto.newBuilder().build(),
                "data/controller/polygonZone/no_zones.json"
            ),
            Arguments.of(
                "В зонах есть null",
                PolygonalLocationZoneDto.newBuilder().zones(Collections.singletonList(null)).build(),
                "data/controller/polygonZone/null_in_zones.json"
            ),
            Arguments.of(
                "Не указан идентификатор зоны",
                PolygonalLocationZoneDto.newBuilder().zones(List.of(getValidZone().externalId(null).build())).build(),
                "data/controller/polygonZone/no_external_id.json"
            ),
            Arguments.of(
                "Не указано название зоны",
                PolygonalLocationZoneDto.newBuilder().zones(List.of(getValidZone().name(null).build())).build(),
                "data/controller/polygonZone/no_zone_name.json"
            ),
            Arguments.of(
                "Не указан флаг активности зоны",
                PolygonalLocationZoneDto.newBuilder().zones(List.of(getValidZone().enabled(null).build())).build(),
                "data/controller/polygonZone/no_enabled.json"
            ),
            Arguments.of(
                "Не указаны координаты зоны",
                PolygonalLocationZoneDto.newBuilder().zones(List.of(getValidZone().coordinates(null).build())).build(),
                "data/controller/polygonZone/no_coordinates.json"
            ),
            Arguments.of(
                "Null в координатах зоны",
                PolygonalLocationZoneDto.newBuilder()
                    .zones(List.of(getValidZone().coordinates(Collections.singletonList(null)).build())).build(),
                "data/controller/polygonZone/null_in_coordinates.json"
            ),
            Arguments.of(
                "Null как точка в координатах",
                PolygonalLocationZoneDto
                    .newBuilder()
                    .zones(List.of(
                        getValidZone().coordinates(List.of(Collections.singletonList(null))).build()
                    ))
                    .build(),
                "data/controller/polygonZone/null_as_point.json"
            ),
            Arguments.of(
                "Не указана широта",
                PolygonalLocationZoneDto.newBuilder().zones(
                    List.of(getValidZone()
                        .coordinates(
                            List.of(List.of(PolygonalLocationZoneDto.Point.of(null, BigDecimal.valueOf(4L))))
                        )
                        .build())
                )
                    .build(),
                "data/controller/polygonZone/no_latitude.json"
            ),
            Arguments.of(
                "Не указана долгота",
                PolygonalLocationZoneDto.newBuilder().zones(List.of(
                    getValidZone()
                        .coordinates(List.of(List.of(
                            PolygonalLocationZoneDto.Point.of(BigDecimal.valueOf(4L), null))
                        ))
                        .build()
                ))
                    .build(),
                "data/controller/polygonZone/no_longitude.json"
            )
        );
    }

    @Nonnull
    private ResultActions saveZones(Long partnerId, PolygonalLocationZoneDto dtos) throws Exception {
        return mockMvc.perform(
            put(String.format("/externalApi/partner/%d/polygonal-location-zone", partnerId))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsBytes(dtos))
        );
    }
}
