package ru.yandex.market.logistics.nesu.controller.internal;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import javax.annotation.Nonnull;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.test.web.servlet.ResultActions;

import ru.yandex.market.logistics.management.client.LMSClient;
import ru.yandex.market.logistics.management.entity.request.polygonalZone.PolygonalLocationZoneDto;
import ru.yandex.market.logistics.nesu.AbstractContextualTest;
import ru.yandex.market.logistics.nesu.dto.geo.GeoJson;
import ru.yandex.market.logistics.nesu.utils.MatcherUtils;
import ru.yandex.market.logistics.nesu.utils.ValidationErrorData;
import ru.yandex.market.logistics.nesu.utils.ValidationErrorData.ErrorType;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static ru.yandex.market.logistics.nesu.utils.ValidationErrorData.fieldErrorBuilder;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.errorMessage;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.extractFileContent;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.noContent;
import static ru.yandex.market.logistics.test.integration.utils.IntegrationTestUtils.status;

@DisplayName("Сохранение полигональных зон")
@DatabaseSetup("/controller/partner/polygonalZone/prepare.xml")
public class InternalPolygonalZoneControllerTest extends AbstractContextualTest {
    @Autowired
    private LMSClient lmsClient;

    @AfterEach
    void after() {
        verifyNoMoreInteractions(lmsClient);
    }

    @Test
    @DisplayName("Успешная десериализация геоджсона")
    void dsbsDeserialize() throws Exception {
        softly.assertThat(objectMapper.readValue(
            extractFileContent("controller/partner/polygonalZone/request/success.json"),
            GeoJson.class
        ))
            .isEqualTo(
                geoJson()
                    .zones(List.of(
                        GeoJson.Zone.of(polygon().build()),
                        GeoJson.Zone.of(
                            GeoJson.PolygonDto.builder()
                                .id("id-2")
                                .enabled(true)
                                .type("Polygon")
                                .coordinates(List.of(List.of(List.of(BigDecimal.valueOf(8L), BigDecimal.valueOf(7L)))))
                                .name("name2").build()
                        )
                    ))
                    .build()
            );
    }

    @Test
    @DisplayName("Успешное сохранение полигональных зон")
    void dsbsSaveZones() throws Exception {
        saveZones(geoJson().build(), 2)
            .andExpect(status().isOk())
            .andExpect(noContent());
        verify(lmsClient).savePolygonalZones(2L, polygonDto());
    }

    @Test
    @DisplayName("Ошибка сохранения - нет настройки между партнером и магазином")
    void dsbsSaveZonesNoSetting() throws Exception {
        saveZones(geoJson().build(), 1)
            .andExpect(status().isNotFound())
            .andExpect(errorMessage("Failed to find [PARTNER] with ids [1]"));
    }

    @ParameterizedTest(name = TUPLE_PARAMETERIZED_DISPLAY_NAME)
    @MethodSource("validationArgs")
    @DisplayName("Ошибки валидации")
    void validation(
        @SuppressWarnings("unused") String name,
        GeoJson geoJson,
        ValidationErrorData.ValidationErrorDataBuilder error
    ) throws Exception {
        saveZones(geoJson, 1)
            .andExpect(MatcherUtils.validationErrorMatcher(error.forObject("geoJson")))
            .andExpect(status().isBadRequest());
    }

    @Nonnull
    private static Stream<Arguments> validationArgs() {
        return Stream.of(
            Arguments.of(
                "Не указан список зон",
                geoJson().zones(Collections.singletonList(null)).build(),
                fieldErrorBuilder("zones[0]", ErrorType.NOT_NULL)
            ),
            Arguments.of(
                "Пустой список зон",
                geoJson().zones(List.of()).build(),
                fieldErrorBuilder("zones", ErrorType.NOT_EMPTY)
            ),
            Arguments.of(
                "Не указан geo",
                geoJson().zones(List.of(GeoJson.Zone.of(null))).build(),
                fieldErrorBuilder("zones[0].geo", ErrorType.NOT_NULL)
            ),
            Arguments.of(
                "Не указан идентификатор полигона",
                geoJson().zones(List.of(GeoJson.Zone.of(polygon().id(null).build()))).build(),
                fieldErrorBuilder("zones[0].geo.id", ErrorType.NOT_EMPTY)
            ),
            Arguments.of(
                "Пустой идентификатор полигона",
                geoJson().zones(List.of(GeoJson.Zone.of(polygon().id("").build()))).build(),
                fieldErrorBuilder("zones[0].geo.id", ErrorType.NOT_EMPTY)
            ),
            Arguments.of(
                "Не указан тип полигона",
                geoJson().zones(List.of(GeoJson.Zone.of(polygon().type(null).build()))).build(),
                fieldErrorBuilder("zones[0].geo.type", ErrorType.NOT_NULL)
            ),
            Arguments.of(
                "Некорректный тип зоны",
                geoJson().zones(List.of(GeoJson.Zone.of(polygon().type(" Polygon").build()))).build(),
                fieldErrorBuilder("zones[0].geo.type", "must match \"Polygon\"", "Pattern")
                    .withArguments(Map.of("regexp", "Polygon"))
            ),
            Arguments.of(
                "Не указано название полигона",
                geoJson().zones(List.of(GeoJson.Zone.of(polygon().name(null).build()))).build(),
                fieldErrorBuilder("zones[0].geo.name", ErrorType.NOT_EMPTY)
            ),
            Arguments.of(
                "Пустое название полигона",
                geoJson().zones(List.of(GeoJson.Zone.of(polygon().name("").build()))).build(),
                fieldErrorBuilder("zones[0].geo.name", ErrorType.NOT_EMPTY)
            ),
            Arguments.of(
                "Не указаны координаты полигона",
                geoJson().zones(List.of(GeoJson.Zone.of(polygon().coordinates(null).build()))).build(),
                fieldErrorBuilder("zones[0].geo.coordinates", ErrorType.NOT_EMPTY)
            ),
            Arguments.of(
                "Пустой список координат",
                geoJson().zones(List.of(GeoJson.Zone.of(polygon().coordinates(List.of()).build()))).build(),
                fieldErrorBuilder("zones[0].geo.coordinates", ErrorType.NOT_EMPTY)
            ),
            Arguments.of(
                "Не указан список координат полигона",
                geoJson().zones(List.of(GeoJson.Zone.of(
                    polygon().coordinates(Collections.singletonList(null)).build()
                )))
                    .build(),
                fieldErrorBuilder("zones[0].geo.coordinates[0]", ErrorType.NOT_EMPTY)
            ),
            Arguments.of(
                "Пустой список координат полигона",
                geoJson().zones(List.of(GeoJson.Zone.of(
                    polygon().coordinates(List.of(List.of())).build()
                )))
                    .build(),
                fieldErrorBuilder("zones[0].geo.coordinates[0]", ErrorType.NOT_EMPTY)
            ),
            Arguments.of(
                "Не указаны широта и долгота",
                geoJson().zones(List.of(GeoJson.Zone.of(
                    polygon().coordinates(List.of(Collections.singletonList(null))).build()
                )))
                    .build(),
                fieldErrorBuilder("zones[0].geo.coordinates[0][0]", ErrorType.NOT_EMPTY)
            ),
            Arguments.of(
                "Указана только долгота",
                geoJson().zones(List.of(GeoJson.Zone.of(
                    polygon().coordinates(List.of(List.of(List.of(BigDecimal.ONE)))).build())))
                    .build(),
                fieldErrorBuilder("zones[0].geo.coordinates[0][0]", ErrorType.size(2, 2))
            ),
            Arguments.of(
                "Не указан флаг активности",
                geoJson().zones(List.of(GeoJson.Zone.of(polygon().enabled(null).build()))).build(),
                fieldErrorBuilder("zones[0].geo.enabled", ErrorType.NOT_NULL)
            )
        );
    }

    @Nonnull
    private static GeoJson.GeoJsonBuilder geoJson() {
        return GeoJson.builder().zones(List.of(GeoJson.Zone.of(polygon().build())));
    }

    @Nonnull
    private static GeoJson.PolygonDto.PolygonDtoBuilder polygon() {
        return GeoJson.PolygonDto.builder()
            .id("id-1")
            .enabled(true)
            .type("Polygon")
            .coordinates(List.of(
                List.of(
                    List.of(BigDecimal.valueOf(2L), BigDecimal.ONE),
                    List.of(BigDecimal.valueOf(4L), BigDecimal.valueOf(3L))
                ),
                List.of(List.of(BigDecimal.valueOf(6L), BigDecimal.valueOf(5L)))
            ))
            .name("name1");
    }

    @Nonnull
    private PolygonalLocationZoneDto polygonDto() {
        return PolygonalLocationZoneDto.newBuilder()
            .zones(List.of(
                PolygonalLocationZoneDto.Zone.builder()
                    .enabled(true)
                    .externalId("id-1")
                    .coordinates(List.of(
                        List.of(
                            PolygonalLocationZoneDto.Point.of(BigDecimal.ONE, BigDecimal.valueOf(2L)),
                            PolygonalLocationZoneDto.Point.of(BigDecimal.valueOf(3L), BigDecimal.valueOf(4L))
                        ),
                        List.of(PolygonalLocationZoneDto.Point.of(BigDecimal.valueOf(5L), BigDecimal.valueOf(6L)))
                    ))
                    .name("name1")
                    .build()
            ))
            .build();
    }

    @Nonnull
    private ResultActions saveZones(GeoJson geoJson, long partnerId) throws Exception {
        return mockMvc.perform(
            request(HttpMethod.POST, String.format("/internal/partner/%d/polygonal-zones", partnerId), geoJson)
        );
    }
}
