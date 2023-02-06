package ru.yandex.market.logistics.management.controller.point;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Set;

import javax.annotation.Nonnull;

import com.github.springtestdbunit.annotation.DatabaseOperation;
import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

import ru.yandex.common.geocoder.client.GeoClient;
import ru.yandex.common.util.region.RegionService;
import ru.yandex.geobase.HttpGeobase;
import ru.yandex.market.logistics.management.AbstractContextualTest;
import ru.yandex.market.logistics.management.entity.request.point.LogisticsPointUpdateRequest;
import ru.yandex.market.logistics.management.entity.response.core.Address;
import ru.yandex.market.logistics.management.entity.response.point.Contact;
import ru.yandex.market.logistics.management.entity.response.point.LogisticsPointResponse;

import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.logistics.management.util.TestRegions.SARKANDAUGAVA_ID;
import static ru.yandex.market.logistics.management.util.TestRegions.buildRegionTree;
import static ru.yandex.market.logistics.management.util.TestUtil.pathToJson;
import static ru.yandex.market.logistics.management.util.TestUtil.testJson;

@DisplayName("Обновление точки")
class LogisticsPointUpdateTest extends AbstractContextualTest {
    private static final String URI = "/externalApi/logisticsPoints/";

    private final ArrayList<Runnable> verifications = new ArrayList<>();

    @Autowired
    private GeoClient geoClient;

    @Autowired
    private HttpGeobase httpGeobase;

    @Autowired
    private RegionService regionService;

    @BeforeEach
    void setup() {
        when(regionService.get()).thenReturn(buildRegionTree());
    }

    @AfterEach
    void verifyInteractions() {
        verifications.forEach(Runnable::run);
        verifications.clear();
        verifyNoMoreInteractions(geoClient);
        verifyNoMoreInteractions(httpGeobase);
    }

    @Test
    @DisplayName("Успешное изменение точки")
    @DatabaseSetup("/data/controller/point/before/prepare_data.xml")
    @ExpectedDatabase(
        value = "/data/controller/point/after/update_logistics_point.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void updateLogisticsPointSuccessful() throws Exception {
        update(1, "data/controller/point/update_point_successful.json")
            .andExpect(status().isOk())
            .andExpect(testJson("data/controller/point/updated_point.json"));
    }

    @Test
    @DisplayName("Успешное изменение точки с типом WAREHOUSE")
    @DatabaseSetup("/data/controller/point/before/prepare_data.xml")
    @ExpectedDatabase(
        value = "/data/controller/point/after/update_logistics_point_6.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void updateWarehouseLogisticsPointSuccessful() throws Exception {
        update(6, "data/controller/point/update_point_6_successful.json")
            .andExpect(status().isOk())
            .andExpect(testJson("data/controller/point/updated_point_6.json"));
        checkBuildWarehouseSegmentTask(6L);
    }

    @Test
    @DisplayName("Корректный запрос на изменение, полученный из LogisticsPointResponse")
    @DatabaseSetup("/data/controller/point/before/prepare_data.xml")
    @DatabaseSetup(value = "/data/controller/point/before/get_and_update.xml",
        type = DatabaseOperation.INSERT
    )
    @ExpectedDatabase(
        value = "/data/controller/point/after/get_and_update.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void getAndUpdatePoint() throws Exception {
        String json = mockMvc.perform(get(URI + "/8"))
            .andReturn()
            .getResponse()
            .getContentAsString();

        LogisticsPointResponse logisticsPoint = objectMapper.readValue(json, LogisticsPointResponse.class);

        LogisticsPointUpdateRequest update = LogisticsPointUpdateRequest.fromResponse(logisticsPoint).build();

        mockMvc.perform(
                post(URI + "/8")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsBytes(update))
            )
            .andExpect(status().isOk());
        checkBuildWarehouseSegmentTask(8L);
    }

    @Test
    @DisplayName("Успешное обновление точки с типом WAREHOUSE при попытке обнулить exact_location_id")
    @DatabaseSetup("/data/controller/point/before/prepare_data.xml")
    @ExpectedDatabase(
        value = "/data/controller/point/after/update_logistics_point_6.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void updateWarehouseLogisticsPointOnExactLocationRemovalAttemptSuccessful() throws Exception {
        update(6, "data/controller/point/update_point_6_successful_null_exact_location_id.json")
            .andExpect(status().isOk())
            .andExpect(testJson("data/controller/point/updated_point_6.json"));
        checkBuildWarehouseSegmentTask(6L);
    }

    @Test
    @DisplayName("Успешная замена точки c типом WAREHOUSE и обновление exact_location_id")
    @DatabaseSetup("/data/controller/point/before/prepare_data.xml")
    @ExpectedDatabase(
        value = "/data/controller/point/after/replace_logistics_point_5.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void replaceWarehouseLogisticsPointIfExactLocationIdWasUnknown() throws Exception {
        mockHttpGeobaseRegionRequest();
        update(5, "data/controller/point/replace_point_5_successful_null_exact_location_id.json")
            .andExpect(status().isOk())
            .andExpect(testJson("data/controller/point/replace_point_5_exact_location_id.json"));
        checkBuildWarehouseSegmentTask(5L, 8L);
    }

    @Test
    @DisplayName("Успешное обновление точки без exact_location_id и типом WAREHOUSE")
    @DatabaseSetup("/data/controller/point/before/prepare_data.xml")
    @ExpectedDatabase(
        value = "/data/controller/point/after/update_logistics_point_5.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void updateWarehouseLogisticsPointIfExactLocationIdWasUnknown() throws Exception {
        mockHttpGeobaseRegionRequest();
        update(5, "data/controller/point/update_point_5_successful_null_exact_location_id.json")
            .andExpect(status().isOk())
            .andExpect(testJson("data/controller/point/updated_point_5_exact_location_id.json"));
        checkBuildWarehouseSegmentTask(5L);
    }

    //    @Test // * DELIVERY-29742. Убрал автоматичекое обогащение subRegion, чтобы лог. точка не пересоздавалась
    @DisplayName("Успешное изменение точки с обновлением поля subRegion")
    @DatabaseSetup("/data/controller/point/before/prepare_data.xml")
    void updateLogisticsPointEnrichSubRegionSuccessful() throws Exception {
        update(6, "data/controller/point/update_point_enrich_subregion.json")
            .andExpect(status().isOk())
            .andExpect(testJson("data/controller/point/updated_point_enrich_subregion.json"));
    }

    @Test
    @DisplayName("Успешная замена логистической точки")
    @DatabaseSetup("/data/controller/point/before/prepare_data.xml")
    @ExpectedDatabase(
        value = "/data/controller/point/after/copy_and_change.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void changeLogisticsPointSuccessful() throws Exception {
        update(6, "data/controller/point/change_logistic_point_successful.json")
            .andExpect(status().isOk())
            .andExpect(testJson("data/controller/point/created_active_point_after_change.json"));

        mockMvc.perform(get(URI + "/6"))
            .andExpect(status().isOk())
            .andExpect(testJson("data/controller/point/changed_point_6.json"));
        checkBuildWarehouseSegmentTask(6L, 8L);
    }

    @Test
    @DisplayName("Успешная замена логистической точки с пустыми контактными данными и расписанием")
    @DatabaseSetup("/data/controller/point/before/prepare_data.xml")
    @ExpectedDatabase(
        value = "/data/controller/point/after/copy_and_change_7.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void changeLogisticsPointSuccessfulContactIsNullScheduleIsNull() throws Exception {
        update(7, "data/controller/point/change_logistic_point_successful.json")
            .andExpect(status().isOk())
            .andExpect(testJson("data/controller/point/created_active_point_after_change_7.json"));

        mockMvc.perform(get(URI + "/7"))
            .andExpect(status().isOk())
            .andExpect(testJson("data/controller/point/changed_point_7.json"));
        checkBuildWarehouseSegmentTask(7L, 8L);
    }

    @Test
    @DisplayName("Ошибка при замене неактивной логистической точки")
    @DatabaseSetup("/data/controller/point/before/prepare_data.xml")
    void changeInactiveLogisticsPoint() throws Exception {
        update(3, "data/controller/point/change_logistic_point_successful.json")
            .andExpect(status().isNotFound())
            .andExpect(status().reason(
                "No active LogisticsPoint found by id = 3, type = 'WAREHOUSE'."
            ));

        mockMvc.perform(get(URI + "/3"))
            .andExpect(status().isOk())
            .andExpect(testJson("data/controller/point/non_changed_point_3.json"));
    }

    @Test
    @DisplayName("Изменение точки по неизвестному идентификатору")
    @DatabaseSetup("/data/controller/point/before/prepare_data.xml")
    void updateNonExistentLogisticsPoint() throws Exception {
        update(123, "data/controller/point/update_point_successful.json")
            .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Успешное изменение услуг точки")
    @DatabaseSetup("/data/controller/point/before/prepare_data.xml")
    @ExpectedDatabase(
        value = "/data/controller/point/after/update_logistics_point_services.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void updateServicesForLogisticsPoint() throws Exception {
        update(1, "data/controller/point/update_services.json")
            .andExpect(status().isOk())
            .andExpect(testJson("data/controller/point/updated_point_with_services.json"));
    }

    @Test
    @DisplayName("Успешное обновление точки при редактировании расписания")
    @DatabaseSetup("/data/controller/point/before/prepare_data.xml")
    @ExpectedDatabase(
        value = "/data/controller/point/after/update_schedule_logistics_point_5.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void updateLogisticsPointWithDifferentSchedule() throws Exception {
        update(5, "data/controller/point/update_point_5_schedule_successful.json")
            .andExpect(status().isOk())
            .andExpect(testJson("data/controller/point/updated_point_5_exact_location_id_and_coords.json"));
        checkBuildWarehouseSegmentTask(5L);
    }

    @Test
    @DisplayName("Смена locationId")
    @DatabaseSetup("/data/controller/point/before/prepare_data_for_update.xml")
    @ExpectedDatabase(
        value = "/data/controller/point/after/update_location_id.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void updateLocationId() throws Exception {
        when(regionService.get()).thenReturn(buildRegionTree());

        update(1000, logisticsPointUpdateRequest(4))
            .andExpect(status().isOk())
            .andExpect(testJson("data/controller/point/update_location_id_4.json"));
        checkBuildWarehouseSegmentTask(1L, 1000L);
    }

    @Test
    @DisplayName("Обновление locationId, нет подходящих зон")
    @DatabaseSetup("/data/controller/point/before/prepare_data_for_update.xml")
    @ExpectedDatabase(
        value = "/data/controller/point/after/update_location_id_without_zones.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void updateLocationIdNoZones() throws Exception {
        when(regionService.get()).thenReturn(buildRegionTree());

        update(1000, logisticsPointUpdateRequest(1))
            .andExpect(status().isOk())
            .andExpect(testJson("data/controller/point/update_location_id_1.json"));
        checkBuildWarehouseSegmentTask(1L, 1000L);
    }

    @Nonnull
    private ResultActions update(long pointId, String requestPath) throws Exception {
        return mockMvc.perform(
            post(URI + pointId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(pathToJson(requestPath))
        );
    }

    @Nonnull
    private ResultActions update(long pointId, LogisticsPointUpdateRequest request) throws Exception {
        return mockMvc.perform(
            post(URI + pointId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        );
    }

    private void mockHttpGeobaseRegionRequest() {
        doReturn(SARKANDAUGAVA_ID).when(httpGeobase).getRegionId(anyDouble(), anyDouble());
        verifications.add(() -> verify(httpGeobase).getRegionId(anyDouble(), anyDouble()));
    }

    @Nonnull
    private LogisticsPointUpdateRequest logisticsPointUpdateRequest(int locationId) {
        return
            LogisticsPointUpdateRequest.newBuilder()
                .name("UPDATED_POINT")
                .active(true)
                .schedule(Set.of())
                .contact(new Contact("Григорий", "Синьков", ""))
                .address(
                    Address.newBuilder()
                        .locationId(locationId)
                        .latitude(BigDecimal.valueOf(1L))
                        .longitude(BigDecimal.valueOf(2))
                        .exactLocationId(225)
                        .build()
                )
                .build();
    }
}
