package ru.yandex.market.logistics.management.controller.admin;

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
import org.testcontainers.shaded.com.fasterxml.jackson.databind.ObjectMapper;

import ru.yandex.common.util.region.RegionService;
import ru.yandex.market.logistics.management.AbstractContextualTest;
import ru.yandex.market.logistics.management.domain.dto.filter.admin.AdminRadialLocationZoneFilter;
import ru.yandex.market.logistics.management.domain.dto.front.radialZone.CreateRadialLocationZoneDto;
import ru.yandex.market.logistics.management.domain.dto.front.radialZone.RadialLocationZoneDetailDto;
import ru.yandex.market.logistics.management.service.plugin.LMSPlugin;
import ru.yandex.market.logistics.management.util.TestUtil;
import ru.yandex.market.logistics.management.util.WithBlackBoxUser;

import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.logistics.management.util.TestRegions.buildRegionTree;
import static ru.yandex.market.logistics.management.util.TestUtil.pathToJson;
import static ru.yandex.market.logistics.test.integration.utils.QueryParamUtils.toParams;

@DisplayName("CRUD ???????????????????? ?????? ?????????? ??????????????")
@DatabaseSetup("/data/controller/admin/radialZone/before/before.xml")
public class AdminRadialLocationZoneControllerTest extends AbstractContextualTest {
    private static final String METHOD_URL = "/admin/lms/radial-location-zone";

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private RegionService regionService;

    @BeforeEach
    void setup() {
        when(regionService.get()).thenReturn(buildRegionTree());
    }

    @DisplayName("?????????? ???????????????????? ????????")
    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("getGridArguments")
    @WithBlackBoxUser(
        login = "lmsUser",
        uid = 1,
        authorities = LMSPlugin.AUTHORITY_ROLE_RADIAL_LOCATION_ZONE
    )
    void getGridSuccessReadOnly(
        @SuppressWarnings("unused") String caseName,
        AdminRadialLocationZoneFilter filter,
        String responsePath
    ) throws Exception {
        mockMvc.perform(get(METHOD_URL).params(toParams(filter)))
            .andExpect(status().isOk())
            .andExpect(TestUtil.jsonContent(responsePath));
    }

    @Test
    @WithBlackBoxUser(
        login = "lmsUser",
        uid = 1,
        authorities = LMSPlugin.AUTHORITY_ROLE_RADIAL_LOCATION_ZONE_EDIT
    )
    @DisplayName("???????????????? ?????????????????? ???????????????? ???????????????????? ????????")
    void getLocationZoneDto() throws Exception {
        mockMvc.perform(get(METHOD_URL + "/1"))
            .andExpect(status().isOk())
            .andExpect(TestUtil.jsonContent("data/controller/admin/radialZone/zone_dto.json"));
    }

    @Test
    @WithBlackBoxUser(
        login = "lmsUser",
        uid = 1,
        authorities = LMSPlugin.AUTHORITY_ROLE_RADIAL_LOCATION_ZONE
    )
    @DisplayName("???????????????? ?????????????????? ???????????????? ???????????????????? ???????? - read only")
    void getLocationZoneDtoReadOnly() throws Exception {
        mockMvc.perform(get(METHOD_URL + "/1"))
            .andExpect(status().isOk())
            .andExpect(TestUtil.jsonContent("data/controller/admin/radialZone/zone_dto_read_only.json"));
    }

    @Test
    @WithBlackBoxUser(
        login = "lmsUser",
        uid = 1,
        authorities = LMSPlugin.AUTHORITY_ROLE_RADIAL_LOCATION_ZONE_EDIT
    )
    @DisplayName("???????????????? ???????????????? ???????????????? ???????????????????? ????????")
    void getCreateLocationZoneDto() throws Exception {
        mockMvc.perform(get(METHOD_URL + "/new"))
            .andExpect(status().isOk())
            .andExpect(TestUtil.jsonContent("data/controller/admin/radialZone/create_dto.json"));
    }

    @Test
    @ExpectedDatabase(
        value = "/data/controller/admin/radialZone/after/after_create.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    @WithBlackBoxUser(
        login = "lmsUser",
        uid = 1,
        authorities = LMSPlugin.AUTHORITY_ROLE_RADIAL_LOCATION_ZONE_EDIT
    )
    @DisplayName("???????????????? ???????????????? ???????????????????? ????????")
    void createLocationZoneSuccess() throws Exception {
        mockMvc.perform(
            post(METHOD_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createValidZone())))
            .andExpect(status().isCreated())
            .andExpect(redirectedUrl("http://localhost/admin/lms/radial-location-zone/6"))
            .andExpect(TestUtil.noContent());
    }

    @Test
    @ExpectedDatabase(
        value = "/data/controller/admin/radialZone/after/after_create_with_existing_parent.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    @WithBlackBoxUser(
        login = "lmsUser",
        uid = 1,
        authorities = LMSPlugin.AUTHORITY_ROLE_RADIAL_LOCATION_ZONE_EDIT
    )
    @DisplayName("???????????????? ???????????????? ???????????????????? ???????? -  ???????? ???????????????????????? ???????? ?? ?????? ???? ????????????????")
    void createLocationZoneSuccessHasParent() throws Exception {
        mockMvc.perform(
            post(METHOD_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createValidZone().setRadius(999000L)))
        )
            .andExpect(status().isCreated())
            .andExpect(redirectedUrl("http://localhost/admin/lms/radial-location-zone/6"))
            .andExpect(TestUtil.noContent());
        verifyZeroInteractions(regionService);
    }

    @Test
    @ExpectedDatabase(
        value = "/data/controller/admin/radialZone/before/before.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @WithBlackBoxUser(
        login = "lmsUser",
        uid = 1,
        authorities = LMSPlugin.AUTHORITY_ROLE_RADIAL_LOCATION_ZONE_EDIT
    )
    @DisplayName("???????????????? ???????????????????? ???????? - ???????? ?? ???????????????? ???????????????? ?? ???????????????? ?????? ????????????????????")
    void createLocationZoneFail() throws Exception {
        mockMvc.perform(
            post(METHOD_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createValidZone().setRadius(2000L))))
            .andExpect(status().isBadRequest())
            .andExpect(status().reason("Radial zone for region 10 and radius 2000 already exist (5)"))
            .andExpect(TestUtil.noContent());
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @DisplayName("???????????? ?????????????????? ?????? ???????????????? ???????????????????? ????????")
    @MethodSource("createOrUpdateZoneArguments")
    @WithBlackBoxUser(
        login = "lmsUser",
        uid = 1,
        authorities = LMSPlugin.AUTHORITY_ROLE_RADIAL_LOCATION_ZONE_EDIT
    )
    void createLocationZoneFail(
        @SuppressWarnings("unused") String caseName,
        CreateRadialLocationZoneDto createRadialLocationZoneDto,
        @SuppressWarnings("unused") RadialLocationZoneDetailDto radialLocationZoneDetailDto,
        String responsePath
    ) throws Exception {
        mockMvc.perform(
            post(METHOD_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createRadialLocationZoneDto))
        )
            .andExpect(status().isBadRequest())
            .andExpect(content().json(pathToJson(String.format(responsePath, "create")), false));
    }

    @Test
    @ExpectedDatabase(
        value = "/data/controller/admin/radialZone/after/after_update.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    @WithBlackBoxUser(
        login = "lmsUser",
        uid = 1,
        authorities = LMSPlugin.AUTHORITY_ROLE_RADIAL_LOCATION_ZONE_EDIT
    )
    @DisplayName("???????????????? ???????????????????? ???????????????????? ????????")
    void updateLocationZoneSuccess() throws Exception {
        mockMvc.perform(
            put(METHOD_URL + "/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createValidUpdateZone().setRadius(2500L))))
            .andExpect(status().isOk())
            .andExpect(TestUtil.jsonContent("data/controller/admin/radialZone/response/update_response.json"));
    }

    @Test
    @ExpectedDatabase(
        value = "/data/controller/admin/radialZone/after/after_delete.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT
    )
    @WithBlackBoxUser(
        login = "lmsUser",
        uid = 1,
        authorities = LMSPlugin.AUTHORITY_ROLE_RADIAL_LOCATION_ZONE_EDIT
    )
    @DisplayName("???????????????? ???????????????????? ????????")
    void deleteLocationZone() throws Exception {
        mockMvc.perform(delete(METHOD_URL + "/3"))
            .andExpect(status().isOk())
            .andExpect(TestUtil.noContent());
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("createOrUpdateZoneArguments")
    @WithBlackBoxUser(
        login = "lmsUser",
        uid = 1,
        authorities = LMSPlugin.AUTHORITY_ROLE_RADIAL_LOCATION_ZONE_EDIT
    )
    @DisplayName("???????????? ?????? ???????????????????? ???????????????????? ????????")
    void updateLocationZoneFail(
        @SuppressWarnings("unused") String caseName,
        @SuppressWarnings("unused") CreateRadialLocationZoneDto createRadialLocationZoneDto,
        RadialLocationZoneDetailDto radialLocationZoneDetailDto,
        String responsePath
    ) throws Exception {
        mockMvc.perform(
            put(METHOD_URL + "/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(radialLocationZoneDetailDto))
        )
            .andExpect(status().isBadRequest())
            .andExpect(content().json(pathToJson(String.format(responsePath, "update")), false));
    }

    @Nonnull
    private static Stream<Arguments> createOrUpdateZoneArguments() {
        return Stream.of(
            Arguments.of(
                "???? ?????????????? ???????????????? ????????",
                createValidZone().setName(null),
                createValidUpdateZone().setName(null),
                "data/controller/admin/radialZone/response/%s/null_name.json"
            ),
            Arguments.of(
                "???????????? ???????????????? ????????",
                createValidZone().setName(""),
                createValidUpdateZone().setName(""),
                "data/controller/admin/radialZone/response/%s/empty_name.json"
            ),
            Arguments.of(
                "???? ???????????? ?????????????????????????? ??????????????",
                createValidZone().setRegionId(null),
                createValidUpdateZone().setRegionId(null),
                "data/controller/admin/radialZone/response/%s/null_region_id.json"
            ),
            Arguments.of(
                "???? ???????????? ????????????",
                createValidZone().setRadius(null),
                createValidUpdateZone().setRadius(null),
                "data/controller/admin/radialZone/response/%s/null_radius.json"
            ),
            Arguments.of(
                "?????????????????????????? ????????????",
                createValidZone().setRadius(-100L),
                createValidUpdateZone().setRadius(-100L),
                "data/controller/admin/radialZone/response/%s/negative_radius.json"
            ),
            Arguments.of(
                "???? ?????????????? ?????????? ????????????????",
                createValidZone().setDeliveryDuration(null),
                createValidUpdateZone().setDeliveryDuration(null),
                "data/controller/admin/radialZone/response/%s/null_delivery_duration.json"
            ),
            Arguments.of(
                "?????????????????????????? ?????????? ????????????????",
                createValidZone().setDeliveryDuration(-100L),
                createValidUpdateZone().setDeliveryDuration(-100L),
                "data/controller/admin/radialZone/response/%s/negative_delivery_duration.json"
            ),
            Arguments.of(
                "???? ???????????? ???????? ??????????????????????",
                createValidZone().setIsPrivate(null),
                createValidUpdateZone().setIsPrivate(null),
                "data/controller/admin/radialZone/response/%s/null_is_private.json"
            )
        );
    }

    @Nonnull
    private static Stream<Arguments> getGridArguments() {
        return Stream.of(
            Arguments.of(
                "?????? ????????????????????",
                AdminRadialLocationZoneFilter.newBuilder().build(),
                "data/controller/admin/radialZone/response/search/search_no_filter.json"
            ),
            Arguments.of(
                "???? ???????????????????? ?????????????? (??????????????????????????)",
                AdminRadialLocationZoneFilter.newBuilder().searchQuery("1").build(),
                "data/controller/admin/radialZone/response/search/search_query_id.json"
            ),
            Arguments.of(
                "???? ???????????????????? ?????????????? (????????????????)",
                AdminRadialLocationZoneFilter.newBuilder().searchQuery("??????").build(),
                "data/controller/admin/radialZone/response/search/search_query_name.json"
            ),
            Arguments.of(
                "???? ????????????????",
                AdminRadialLocationZoneFilter.newBuilder().name("??????").build(),
                "data/controller/admin/radialZone/response/search/search_name.json"
            ),
            Arguments.of(
                "???? ???????????????????????????? ??????????????",
                AdminRadialLocationZoneFilter.newBuilder().regionId(4).build(),
                "data/controller/admin/radialZone/response/search/search_region_id.json"
            ),
            Arguments.of(
                "???? ??????????????",
                AdminRadialLocationZoneFilter.newBuilder().radius(999000L).build(),
                "data/controller/admin/radialZone/response/search/search_radius.json"
            ),
            Arguments.of(
                "???? ?????????????? ???????????????? ?? ????????",
                AdminRadialLocationZoneFilter.newBuilder().deliveryDuration(60L).build(),
                "data/controller/admin/radialZone/response/search/search_delivery_duration.json"
            ),
            Arguments.of(
                "???? ?????????? ??????????????????????",
                AdminRadialLocationZoneFilter.newBuilder().isPrivate(true).build(),
                "data/controller/admin/radialZone/response/search/search_is_private.json"
            ),
            Arguments.of(
                "???? ???????? ????????????????????",
                AdminRadialLocationZoneFilter.newBuilder()
                    .name("????????????")
                    .radius(2000L)
                    .regionId(10)
                    .deliveryDuration(30L)
                    .searchQuery("5")
                    .build(),
                "data/controller/admin/radialZone/response/search/search_all.json"
            )
        );
    }

    @Nonnull
    private static CreateRadialLocationZoneDto createValidZone() {
        return new CreateRadialLocationZoneDto()
            .setDeliveryDuration(100L)
            .setName("?????????? ???????? ??????")
            .setRadius(10000L)
            .setRegionId(10);
    }

    @Nonnull
    private static RadialLocationZoneDetailDto createValidUpdateZone() {
        return new RadialLocationZoneDetailDto()
            .setId(1L)
            .setTitle("???????????? ???????? ??????")
            .setZoneId(1L)
            .setDeliveryDuration(30L)
            .setName("???????????? ???????? ??????")
            .setRadius(2000L)
            .setRegionId(213)
            .setIsPrivate(false);
    }
}
