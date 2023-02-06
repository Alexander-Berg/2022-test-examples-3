package ru.yandex.market.logistics.management.controller.admin;

import java.io.IOException;
import java.time.LocalTime;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Stream;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import ru.yandex.common.geocoder.client.GeoClient;
import ru.yandex.common.geocoder.model.request.GeoSearchParams;
import ru.yandex.common.geocoder.model.response.AddressInfo;
import ru.yandex.common.geocoder.model.response.AreaInfo;
import ru.yandex.common.geocoder.model.response.Boundary;
import ru.yandex.common.geocoder.model.response.CountryInfo;
import ru.yandex.common.geocoder.model.response.LocalityInfo;
import ru.yandex.common.geocoder.model.response.SimpleGeoObject;
import ru.yandex.common.geocoder.model.response.ToponymInfo;
import ru.yandex.common.util.region.RegionService;
import ru.yandex.geobase.HttpGeobase;
import ru.yandex.market.logistics.management.AbstractContextualTest;
import ru.yandex.market.logistics.management.domain.dto.front.logisticsPoint.WarehouseNewDto;
import ru.yandex.market.logistics.management.entity.request.point.filter.AdminLogisticsPointFilter;
import ru.yandex.market.logistics.management.service.plugin.LMSPlugin;
import ru.yandex.market.logistics.management.util.WithBlackBoxUser;

import static org.hamcrest.Matchers.endsWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.logistics.management.util.TestRegions.MOSCOW_REGION_ID;
import static ru.yandex.market.logistics.management.util.TestRegions.buildRegionTree;
import static ru.yandex.market.logistics.management.util.TestUtil.pathToJson;
import static ru.yandex.market.logistics.management.util.TestUtil.point;
import static ru.yandex.market.logistics.management.util.TestUtil.testJson;
import static ru.yandex.market.logistics.test.integration.utils.QueryParamUtils.toParams;

@DisplayName("CRUD логистических точек через админку")
@ParametersAreNonnullByDefault
class AdminLogisticsPointControllerTest extends AbstractContextualTest {
    private static final String METHOD_URL = "/admin/lms/logistics-point";
    private static final ObjectMapper MAPPER = new ObjectMapper();

    static {
        MAPPER.registerModules(new Jdk8Module(), new JavaTimeModule());
    }

    @Autowired
    private GeoClient geoClient;

    @Autowired
    private HttpGeobase httpGeobase;

    @Autowired
    private RegionService regionService;

    @Nonnull
    private String getMethodUrl() {
        return METHOD_URL;
    }

    @AfterEach
    void teardown() {
        verifyNoMoreInteractions(geoClient, httpGeobase, regionService);
    }

    @ParameterizedTest(name = "поиск по значению {0}")
    @MethodSource
    @DatabaseSetup("/data/controller/admin/logisticsPoint/before/prepare_data.xml")
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = LMSPlugin.AUTHORITY_ROLE_LOGISTICS_POINT_EDIT)
    void getGridSearchQuery(String searchQueryString, String responsePath) throws Exception {
        mockMvc.perform(getGrid().params(toParams(
                AdminLogisticsPointFilter.newBuilder().searchQuery(searchQueryString).build()
            )))
            .andExpect(status().isOk())
            .andExpect(testJson(responsePath));
    }

    @Nonnull
    private static Stream<Arguments> getGridSearchQuery() {
        return Stream.of(
            Arguments.of(
                "no result",
                "data/controller/admin/logisticsPoint/searchResults/no_result.json"
            ),
            Arguments.of(
                "Пункт",
                "data/controller/admin/logisticsPoint/searchResults/punkt.json"
            ),
            Arguments.of(
                "клад",
                "data/controller/admin/logisticsPoint/searchResults/klad.json"
            ),
            Arguments.of(
                "1",
                "data/controller/admin/logisticsPoint/searchResults/1.json"
            )
        );
    }

    @Test
    @DisplayName("Получение формы создания склада")
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = LMSPlugin.AUTHORITY_ROLE_LOGISTICS_POINT_EDIT)
    void getNewWarehouseView() throws Exception {
        mockMvc.perform(get(METHOD_URL + "/new"))
            .andExpect(status().isOk())
            .andExpect(testJson("data/controller/admin/logisticsPoint/create/new_wh_view.json"));
    }

    @Test
    @DisplayName("Создание склада")
    @DatabaseSetup("/data/controller/admin/logisticsPoint/create/prepare_data.xml")
    @ExpectedDatabase(
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED,
        value = "/data/controller/admin/logisticsPoint/create/expected_db.xml"
    )
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = LMSPlugin.AUTHORITY_ROLE_LOGISTICS_POINT_EDIT)
    void createWarehouse() throws Exception {
        String query = "Москва и Московская область, Подольский район, Подольск, Ленина, дом 21, строение 2, корпус 1";
        when(geoClient.find(eq(query), any(GeoSearchParams.class))).thenReturn(List.of(geoObject()));
        when(httpGeobase.getRegionId(anyDouble(), anyDouble())).thenReturn(MOSCOW_REGION_ID);
        when(regionService.get()).thenReturn(buildRegionTree());

        mockMvc.perform(post(METHOD_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(pathToJson("data/controller/admin/logisticsPoint/create/new_wh_request.json"))
            )
            .andExpect(status().isCreated())
            .andExpect(header().string("location", endsWith("/admin/lms/logistics-point/1")));

        verify(geoClient).find(eq(query), any(GeoSearchParams.class));
        verify(httpGeobase).getRegionId(55.426421, 37.537743);
        verify(regionService).get();
        checkBuildWarehouseSegmentTask(1L);
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("createInvalidWarehouseData")
    @DisplayName("Попытка создания невалидного склада")
    @DatabaseSetup("/data/controller/admin/logisticsPoint/create/prepare_data.xml")
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = LMSPlugin.AUTHORITY_ROLE_LOGISTICS_POINT_EDIT)
    void createInvalidWarehouse(
        @SuppressWarnings("unused") String caseName,
        Consumer<WarehouseNewDto> requestPreparer,
        String errorMessage,
        String fieldName
    ) throws Exception {
        mockMvc.perform(post(METHOD_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(prepareCreateRequest(requestPreparer))
            )
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("errors.length()").value(1))
            .andExpect(jsonPath("errors[0].field").value(fieldName))
            .andExpect(jsonPath("errors[0].defaultMessage").value(errorMessage));
    }

    @Nonnull
    private static Stream<Arguments> createInvalidWarehouseData() {
        return Stream.of(
            Arguments.of(
                "Склад с пустым наименованием",
                (Consumer<WarehouseNewDto>) dto -> dto.setName(""),
                "Обязательно для заполнения",
                "name"
            ),
            Arguments.of(
                "Склад без externalId",
                (Consumer<WarehouseNewDto>) dto -> dto.setExternalId(null),
                "Обязательно для заполнения",
                "externalId"
            ),
            Arguments.of(
                "Склад без partnerId",
                (Consumer<WarehouseNewDto>) dto -> dto.setPartnerId(null),
                "Обязательно для заполнения",
                "partnerId"
            ),
            Arguments.of(
                "Адрес без почтового индекса",
                (Consumer<WarehouseNewDto>) dto -> dto.getAddress().setPostCode(""),
                "Обязательно для заполнения",
                "address.postCode"
            ),
            Arguments.of(
                "Адрес без региона",
                (Consumer<WarehouseNewDto>) dto -> dto.getAddress().setRegion(""),
                "Обязательно для заполнения",
                "address.region"
            ),
            Arguments.of(
                "Адрес без населенного пункта",
                (Consumer<WarehouseNewDto>) dto -> dto.getAddress().setSettlement(""),
                "Обязательно для заполнения",
                "address.settlement"
            ),
            Arguments.of(
                "Адрес без улицы",
                (Consumer<WarehouseNewDto>) dto -> dto.getAddress().setStreet(""),
                "Обязательно для заполнения",
                "address.street"
            ),
            Arguments.of(
                "Адрес без дома",
                (Consumer<WarehouseNewDto>) dto -> dto.getAddress().setHouse(""),
                "Обязательно для заполнения",
                "address.house"
            ),
            Arguments.of(
                "Адрес без страны",
                (Consumer<WarehouseNewDto>) dto -> dto.getAddress().setCountry(""),
                "Обязательно для заполнения",
                "address.country"
            ),
            Arguments.of(
                "Контактное лицо без имени",
                (Consumer<WarehouseNewDto>) dto -> dto.getContact().setFirstName(""),
                "Обязательно для заполнения",
                "contact.firstName"
            ),
            Arguments.of(
                "Контактное лицо без фамилии",
                (Consumer<WarehouseNewDto>) dto -> dto.getContact().setLastName(""),
                "Обязательно для заполнения",
                "contact.lastName"
            ),
            Arguments.of(
                "Не заполнен телефон",
                (Consumer<WarehouseNewDto>) dto -> dto.getPhone().setNumber(""),
                "Обязательно для заполнения",
                "phone.number"
            ),
            Arguments.of(
                "Не заполнено время начала работы для одного из дней",
                (Consumer<WarehouseNewDto>) dto -> dto.getSchedule().setMondayFrom(null),
                "Обязательно для заполнения",
                "schedule.mondayFrom"
            ),
            Arguments.of(
                "Не заполнено время кончания работы для одного из дней",
                (Consumer<WarehouseNewDto>) dto -> dto.getSchedule().setMondayTo(null),
                "Обязательно для заполнения",
                "schedule.mondayTo"
            ),
            Arguments.of(
                "Не заполнено время окончания работы ворот для одного из дней",
                (Consumer<WarehouseNewDto>) dto -> dto.getGateSchedule().setMondayTo(null),
                "Обязательно для заполнения",
                "gateSchedule.mondayTo"
            ),
            Arguments.of(
                "Не заполнено время начала работы больше, чем время окончания для одного из дней",
                (Consumer<WarehouseNewDto>) dto -> dto.getSchedule()
                    .setMondayFrom(LocalTime.parse("12:00"))
                    .setMondayTo(LocalTime.parse("10:00")),
                "Должно быть больше времени начала",
                "schedule.mondayTo"
            )
        );
    }

    @Nonnull
    private static String prepareCreateRequest(Consumer<WarehouseNewDto> dtoConsumer) throws IOException {
        WarehouseNewDto dto = MAPPER.readValue(
            pathToJson("data/controller/admin/logisticsPoint/create/new_wh_request.json"),
            WarehouseNewDto.class
        );
        dtoConsumer.accept(dto);
        return MAPPER.writeValueAsString(dto);
    }

    @Nonnull
    private MockHttpServletRequestBuilder getGrid() {
        return get(getMethodUrl());
    }

    @Nonnull
    private SimpleGeoObject geoObject() {
        return SimpleGeoObject.newBuilder()
            .withToponymInfo(
                ToponymInfo.newBuilder()
                    .withGeoid("213977")
                    .withPoint(point(55.426421, 37.537743))
                    .build()
            )
            .withAddressInfo(
                AddressInfo.newBuilder()
                    .withCountryInfo(CountryInfo.newBuilder().build())
                    .withAreaInfo(AreaInfo.newBuilder().build())
                    .withLocalityInfo(LocalityInfo.newBuilder().build())
                    .build()
            )
            .withBoundary(Boundary.newBuilder().build())
            .build();
    }
}
