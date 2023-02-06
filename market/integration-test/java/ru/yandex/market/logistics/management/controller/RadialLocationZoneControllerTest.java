package ru.yandex.market.logistics.management.controller;

import java.util.Set;
import java.util.stream.Stream;

import javax.annotation.Nonnull;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.testcontainers.shaded.com.fasterxml.jackson.databind.ObjectMapper;

import ru.yandex.common.util.region.RegionService;
import ru.yandex.market.logistics.management.AbstractContextualTest;
import ru.yandex.market.logistics.management.configuration.properties.FeatureProperties;
import ru.yandex.market.logistics.management.domain.dto.front.logisticPointRadialZone.LinkLogisticPointRadialLocationZonesDto;
import ru.yandex.market.logistics.management.entity.request.radialZone.RadialLocationZoneFilter;
import ru.yandex.market.logistics.management.service.plugin.LMSPlugin;
import ru.yandex.market.logistics.management.util.TestUtil;
import ru.yandex.market.logistics.management.util.WithBlackBoxUser;
import ru.yandex.market.logistics.test.integration.jpa.JpaQueriesCount;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.logistics.management.util.TestRegions.buildRegionTree;

/**
 * Интеграционный тест {@link RadialLocationZoneController}.
 */
class RadialLocationZoneControllerTest extends AbstractContextualTest {
    private static final String METHOD_URL = "/externalApi/radial-location-zone";
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private RegionService regionService;
    @Autowired
    private FeatureProperties featureProperties;

    @BeforeEach
    void setup() {
        when(regionService.get()).thenReturn(buildRegionTree());
        featureProperties.setDefaultRadiusValues(Set.of(5000L, 10000L, 15000L, 20000L, 25000L));
    }

    @DisplayName("Получить радиальные зоны по фильтру")
    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("getGridArguments")
    @WithBlackBoxUser(
        login = "lmsUser",
        uid = 1,
        authorities = LMSPlugin.AUTHORITY_ROLE_RADIAL_LOCATION_ZONE
    )
    @DatabaseSetup("/data/controller/radialZone/before/before.xml")
    void getRadialLocationZones(
        @SuppressWarnings("unused") String caseName,
        RadialLocationZoneFilter filter,
        String responsePath
    ) throws Exception {
        mockMvc.perform(put(METHOD_URL + "/search")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(filter)))
            .andExpect(status().isOk())
            .andExpect(TestUtil.jsonContent(responsePath));
    }

    @Test
    @DisplayName("Связать радиальные зоны со складом")
    @ExpectedDatabase(
        value = "/data/controller/radialZone/after/after_link.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @DatabaseSetup("/data/controller/radialZone/before/before.xml")
    void linkLogisticPointWithZone() throws Exception {
        mockMvc.perform(post(METHOD_URL + "/link")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(
                LinkLogisticPointRadialLocationZonesDto
                    .newBuilder()
                    .logisticPointId(1L)
                    .zoneIds(Set.of(1L, 2L, 8L, 4L, 3L))
                    .build()
            ))
        )
            .andExpect(status().isOk())
            .andExpect(TestUtil.noContent());
    }

    @Test
    @DisplayName("Связать радиальные зоны со складом - склад не найден")
    @ExpectedDatabase(
        value = "/data/controller/radialZone/before/before.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @DatabaseSetup("/data/controller/radialZone/before/before.xml")
    void linkLogisticPointWithZoneNoPoint() throws Exception {
        mockMvc.perform(post(METHOD_URL + "/link")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(
                LinkLogisticPointRadialLocationZonesDto
                    .newBuilder()
                    .logisticPointId(1000000L)
                    .zoneIds(Set.of(1L, 2L, 8L, 4L, 3L))
                    .build()
            ))
        )
            .andExpect(status().isNotFound())
            .andExpect(status().reason("Cannot find point with id 1000000"));
    }

    @Test
    @DisplayName("Успешная привязка зон из файла")
    @DatabaseSetup("/data/controller/radialZone/before/before_linking_new_zones.xml")
    @ExpectedDatabase(
        value = "/data/controller/radialZone/after/after_link_from_file.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    @JpaQueriesCount(53)
        //TODO: уменьшить количество
    void linkZonesFromFile() throws Exception {
        MockMultipartFile mockMultipartFile = new MockMultipartFile(
            "multipartFile",
            "zones.xlsx",
            "application/x-xls",
            getClass().getClassLoader().getResourceAsStream("data/controller/radialZone/zones.xlsx")
        );

        mockMvc.perform(MockMvcRequestBuilders.multipart(METHOD_URL + "/upload").file(mockMultipartFile))
            .andExpect(status().isOk())
            .andExpect(TestUtil.noContent());
    }

    @Test
    @DisplayName("Ошибка чтения - другое название столбца")
    @DatabaseSetup("/data/controller/radialZone/before/before_linking_new_zones.xml")
    @ExpectedDatabase(
        value = "/data/controller/radialZone/before/before_linking_new_zones.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void linkZonesFromFileFailDifferentColumnName() throws Exception {
        MockMultipartFile mockMultipartFile = new MockMultipartFile(
            "multipartFile",
            "invalid_zones.xlsx",
            "application/x-xls",
            getClass().getClassLoader().getResourceAsStream("data/controller/radialZone/invalid_zones.xlsx")
        );

        mockMvc.perform(MockMvcRequestBuilders.multipart(METHOD_URL + "/upload").file(mockMultipartFile))
            .andExpect(status().isBadRequest())
            .andExpect(status().reason(
                "All rows must contain values in the columns [partnerId, deliveryDuration, radius]"
            ))
            .andExpect(TestUtil.noContent());
    }

    @Test
    @DisplayName("Ошибка - в файле повторяется партнер")
    @DatabaseSetup("/data/controller/radialZone/before/before_linking_new_zones.xml")
    @ExpectedDatabase(
        value = "/data/controller/radialZone/before/before_linking_new_zones.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    void linkZonesFromFileFailRepeatedPartner() throws Exception {
        MockMultipartFile mockMultipartFile = new MockMultipartFile(
            "multipartFile",
            "repeated_partner.xlsx",
            "application/x-xls",
            getClass().getClassLoader().getResourceAsStream("data/controller/radialZone/repeated_partner.xlsx")
        );

        mockMvc.perform(MockMvcRequestBuilders.multipart(METHOD_URL + "/upload").file(mockMultipartFile))
            .andExpect(status().isBadRequest())
            .andExpect(status().reason("There are repeating partners"))
            .andExpect(TestUtil.noContent());
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("getInvalidArguments")
    @DisplayName("Невалидный запрос на связь радиальной зоны со складом")
    @DatabaseSetup("/data/controller/radialZone/before/before.xml")
    void linkPointAndZoneFail(
        @SuppressWarnings("unused") String caseName,
        LinkLogisticPointRadialLocationZonesDto dto,
        String responsePath
    )
        throws Exception {
        mockMvc.perform(post(METHOD_URL + "/link")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(dto))
        )
            .andExpect(status().isBadRequest())
            .andExpect(content().json(TestUtil.pathToJson(responsePath), false));
    }

    @Nonnull
    private static Stream<Arguments> getInvalidArguments() {
        return Stream.of(
            Arguments.of(
                "Нет идентификатора склада",
                LinkLogisticPointRadialLocationZonesDto.newBuilder().zoneIds(Set.of(1L)).build(),
                "data/controller/radialZone/link_no_point_id.json"
            ),
            Arguments.of(
                "Нет идентификатора зоны",
                LinkLogisticPointRadialLocationZonesDto.newBuilder().logisticPointId(1L).build(),
                "data/controller/radialZone/link_no_zone_id.json"
            ),
            Arguments.of(
                "Нет идентификатора зоны и склада",
                LinkLogisticPointRadialLocationZonesDto.newBuilder().build(),
                "data/controller/radialZone/link_no_zone_id_and_point_id.json"
            )
        );
    }

    @Nonnull
    private static Stream<Arguments> getGridArguments() {
        return Stream.of(
            Arguments.of(
                "Без фильтрации",
                RadialLocationZoneFilter.newBuilder().build(),
                "data/controller/radialZone/search_no_filter.json"
            ),
            Arguments.of(
                "По поисковому запросу (идентификатор)",
                RadialLocationZoneFilter.newBuilder().searchQuery("1").build(),
                "data/controller/radialZone/search_query_id.json"
            ),
            Arguments.of(
                "По поисковому запросу (название)",
                RadialLocationZoneFilter.newBuilder().searchQuery("спб").build(),
                "data/controller/radialZone/search_query_name.json"
            ),
            Arguments.of(
                "По названию",
                RadialLocationZoneFilter.newBuilder().name("рос").build(),
                "data/controller/radialZone/search_name.json"
            ),
            Arguments.of(
                "По идентификатору региона",
                RadialLocationZoneFilter.newBuilder().regionId(4).build(),
                "data/controller/radialZone/search_region_id.json"
            ),
            Arguments.of(
                "По радиусу",
                RadialLocationZoneFilter.newBuilder().radius(999000L).build(),
                "data/controller/radialZone/search_radius.json"
            ),
            Arguments.of(
                "По времени доставки в зону",
                RadialLocationZoneFilter.newBuilder().deliveryDuration(60L).build(),
                "data/controller/radialZone/search_delivery_duration.json"
            ),
            Arguments.of(
                "По складу",
                RadialLocationZoneFilter.newBuilder().logisticsPoint(3L).build(),
                "data/controller/radialZone/search_logistic_point.json"
            ),
            Arguments.of(
                "По приватности",
                RadialLocationZoneFilter.newBuilder().isPrivate(true).build(),
                "data/controller/radialZone/search_private.json"
            ),
            Arguments.of(
                "По всем параметрам",
                RadialLocationZoneFilter.newBuilder()
                    .name("РОССИЯ")
                    .radius(999000L)
                    .regionId(2)
                    .deliveryDuration(190L)
                    .searchQuery("3")
                    .build(),
                "data/controller/radialZone/search_all.json"
            )
        );
    }
}
