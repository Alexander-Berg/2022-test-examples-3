package ru.yandex.market.logistics.management.controller.admin;

import java.time.LocalDate;
import java.time.Month;
import java.time.ZoneId;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

import com.github.springtestdbunit.annotation.DatabaseOperation;
import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.github.springtestdbunit.annotation.ExpectedDatabase;
import com.github.springtestdbunit.assertion.DatabaseAssertionMode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

import ru.yandex.market.logistics.management.AbstractContextualTest;
import ru.yandex.market.logistics.management.configuration.properties.FeatureProperties;
import ru.yandex.market.logistics.management.util.TestableClock;
import ru.yandex.market.logistics.management.util.WithBlackBoxUser;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.logistics.management.service.plugin.LMSPlugin.AUTHORITY_ROLE_LOGISTICS_POINT;
import static ru.yandex.market.logistics.management.service.plugin.LMSPlugin.AUTHORITY_ROLE_LOGISTICS_POINT_EDIT;
import static ru.yandex.market.logistics.management.service.plugin.LMSPlugin.SLUG_LOGISTICS_POINT_DEACTIVATE;
import static ru.yandex.market.logistics.management.service.plugin.LMSPlugin.SLUG_LOGISTICS_POINT_FREEZE;
import static ru.yandex.market.logistics.management.service.plugin.LMSPlugin.SLUG_LOGISTICS_POINT_UNFREEZE;
import static ru.yandex.market.logistics.management.util.TestUtil.pathToJson;
import static ru.yandex.market.logistics.management.util.TestUtil.testJson;

@DatabaseSetup("/data/controller/admin/logisticsPoint/before/prepare_data.xml")
@ParametersAreNonnullByDefault
class LmsControllerLogisticsPointTest extends AbstractContextualTest {

    @Autowired
    private TestableClock clock;

    @Autowired
    private FeatureProperties featureProperties;

    @Test
    void logisticsPointGridUnauthorized() throws Exception {
        getLogisticsPointGrid()
            .andExpect(status().isUnauthorized());
    }

    @Test
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = {})
    void logisticsPointGridForbidden() throws Exception {
        getLogisticsPointGrid()
            .andExpect(status().isForbidden());
    }

    @Test
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = AUTHORITY_ROLE_LOGISTICS_POINT)
    void logisticsPointGrid() throws Exception {
        getLogisticsPointGrid()
            .andExpect(status().isOk())
            .andExpect(testJson(
                "data/controller/admin/logisticsPoint/grid.json",
                false
            ));
    }

    @Test
    void logisticsPointDetailUnauthorized() throws Exception {
        getLogisticsPointDetail(1L)
            .andExpect(status().isUnauthorized());
    }

    @Test
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = {})
    void logisticsPointDetailForbidden() throws Exception {
        getLogisticsPointDetail(1L)
            .andExpect(status().isForbidden());
    }

    @Test
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = AUTHORITY_ROLE_LOGISTICS_POINT)
    void logisticsPointDetailNotFound() throws Exception {
        getLogisticsPointDetail(0L)
            .andExpect(status().isNotFound());
    }

    @Test
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = AUTHORITY_ROLE_LOGISTICS_POINT)
    void pickupPointDetail() throws Exception {
        getLogisticsPointDetail(3L)
            .andExpect(status().isOk())
            .andExpect(testJson(
                "data/controller/admin/logisticsPoint/pickup_point_detail.json",
                false
            ));
    }

    @Test
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = AUTHORITY_ROLE_LOGISTICS_POINT)
    void warehouseDetail() throws Exception {
        clock.setFixed(
            LocalDate.of(2019, Month.JANUARY, 1).atStartOfDay(ZoneId.systemDefault()).toInstant(),
            ZoneId.systemDefault()
        );
        getLogisticsPointDetail(1L)
            .andExpect(status().isOk())
            .andExpect(testJson(
                "data/controller/admin/logisticsPoint/warehouse_detail.json",
                false
            ));
        clock.clearFixed();
    }

    @Test
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = AUTHORITY_ROLE_LOGISTICS_POINT)
    void warehouseDetailDropoff() throws Exception {
        clock.setFixed(
            LocalDate.of(2019, Month.JANUARY, 1).atStartOfDay(ZoneId.systemDefault()).toInstant(),
            ZoneId.systemDefault()
        );
        featureProperties.setDropoffDisablingEnabled(true);
        getLogisticsPointDetail(8L)
            .andExpect(status().isOk())
            .andExpect(testJson(
                "data/controller/admin/logisticsPoint/warehouse_detail_dropoff.json",
                false
            ));
        featureProperties.setDropoffDisablingEnabled(false);
        clock.clearFixed();
    }

    @Test
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = AUTHORITY_ROLE_LOGISTICS_POINT)
    void warehouseDetailNonDisableDropoff() throws Exception {
        clock.setFixed(
            LocalDate.of(2019, Month.JANUARY, 1).atStartOfDay(ZoneId.systemDefault()).toInstant(),
            ZoneId.systemDefault()
        );
        featureProperties.setDropoffDisablingEnabled(true);
        getLogisticsPointDetail(8L)
            .andExpect(status().isOk())
            .andExpect(testJson(
                "data/controller/admin/logisticsPoint/warehouse_detail_non_disable_dropoff.json",
                false
            ));
        featureProperties.setDropoffDisablingEnabled(false);
        clock.clearFixed();
    }

    @Test
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = AUTHORITY_ROLE_LOGISTICS_POINT)
    void warehouseWithoutPartnerDetail() throws Exception {
        getLogisticsPointDetail(7L)
            .andExpect(status().isOk())
            .andExpect(testJson(
                "data/controller/admin/logisticsPoint/warehouse_without_partner_detail.json",
                false
            ));
    }

    @Test
    void deactivateLogisticsPointsUnauthorized() throws Exception {
        performActionOnLogisticsPoints(SLUG_LOGISTICS_POINT_DEACTIVATE)
            .andExpect(status().isUnauthorized());
    }

    @Test
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = {})
    void deactivateLogisticsPointsForbidden() throws Exception {
        performActionOnLogisticsPoints(SLUG_LOGISTICS_POINT_DEACTIVATE)
            .andExpect(status().isForbidden());
    }

    @Test
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = AUTHORITY_ROLE_LOGISTICS_POINT_EDIT)
    @ExpectedDatabase(
        value = "/data/controller/admin/logisticsPoint/after/deactivate.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    void deactivateLogisticsPoints() throws Exception {
        performActionOnLogisticsPoints(SLUG_LOGISTICS_POINT_DEACTIVATE)
            .andExpect(status().isOk());
        checkBuildWarehouseSegmentTask(1L);
    }

    @Test
    void unfreezeLogisticsPointsUnauthorized() throws Exception {
        performActionOnLogisticsPoints(SLUG_LOGISTICS_POINT_UNFREEZE)
            .andExpect(status().isUnauthorized());
    }

    @Test
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = {})
    void unfreezeLogisticsPointsForbidden() throws Exception {
        performActionOnLogisticsPoints(SLUG_LOGISTICS_POINT_UNFREEZE)
            .andExpect(status().isForbidden());
    }

    @Test
    @ExpectedDatabase(
        value = "/data/controller/admin/logisticsPoint/after/unfreeze.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = AUTHORITY_ROLE_LOGISTICS_POINT_EDIT)
    void unfreezeLogisticsPoints() throws Exception {
        performActionOnLogisticsPoints(SLUG_LOGISTICS_POINT_UNFREEZE)
            .andExpect(status().isOk());
        checkBuildWarehouseSegmentTask(1L);
    }

    @Test
    void freezeLogisticsPointsUnauthorized() throws Exception {
        performActionOnLogisticsPoints(SLUG_LOGISTICS_POINT_FREEZE)
            .andExpect(status().isUnauthorized());
    }

    @Test
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = {})
    void freezeLogisticsPointsForbidden() throws Exception {
        performActionOnLogisticsPoints(SLUG_LOGISTICS_POINT_FREEZE)
            .andExpect(status().isForbidden());
    }

    @Test
    @ExpectedDatabase(
        value = "/data/controller/admin/logisticsPoint/after/freeze.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = AUTHORITY_ROLE_LOGISTICS_POINT_EDIT)
    void freezeLogisticsPoints() throws Exception {
        performActionOnLogisticsPoints(SLUG_LOGISTICS_POINT_FREEZE)
            .andExpect(status().isOk());
        checkBuildWarehouseSegmentTask(1L);
    }

    @Test
    void updateLogisticsPointUnauthorized() throws Exception {
        updateLogisticsPoint(3L)
            .andExpect(status().isUnauthorized());
    }

    @Test
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = {})
    void updateLogisticsPointForbidden() throws Exception {
        updateLogisticsPoint(3L)
            .andExpect(status().isForbidden());
    }

    @Test
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = AUTHORITY_ROLE_LOGISTICS_POINT_EDIT)
    void updateLogisticsPointNotFound() throws Exception {
        updateLogisticsPoint(11L)
            .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Обновление ПВЗ")
    @ExpectedDatabase(
        value = "/data/controller/admin/logisticsPoint/after/update_pickup.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = AUTHORITY_ROLE_LOGISTICS_POINT_EDIT)
    void updatePickupPoint() throws Exception {
        updateLogisticsPoint(3L)
            .andExpect(status().isOk())
            .andExpect(testJson(
                "data/controller/admin/logisticsPoint/update_result_pickup.json",
                false
            ));
    }

    @Test
    @DisplayName("Установка флага доступности C2C у дропоффа")
    @ExpectedDatabase(
        value = "/data/controller/admin/logisticsPoint/after/update_dropoff.xml",
        assertionMode = DatabaseAssertionMode.NON_STRICT_UNORDERED
    )
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = AUTHORITY_ROLE_LOGISTICS_POINT_EDIT)
    void updateDropoffToC2C() throws Exception {
        updateLogisticsPoint(10L, "data/controller/admin/logisticsPoint/update_to_c2c.json")
            .andExpect(status().isOk())
            .andExpect(testJson(
                "data/controller/admin/logisticsPoint/update_result_dropoff.json",
                false
            ));
        Mockito.verify(buildWarehouseSegmentsProducer).produceTask(10L);
    }

    @Test
    @DisplayName("Установка флага доступности C2C у точки, не являющейся дропоффом, ошибка")
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = AUTHORITY_ROLE_LOGISTICS_POINT_EDIT)
    @DatabaseSetup(
        value = "/data/controller/admin/logisticsPoint/before/inactive_segment_service.xml",
        type = DatabaseOperation.UPDATE
    )
    void updatePickupToC2CError() throws Exception {
        updateLogisticsPoint(10L, "data/controller/admin/logisticsPoint/update_to_c2c.json")
            .andExpect(status().isBadRequest())
            .andExpect(status().reason("Point can't become available for C2C"));
    }

    @Test
    @DisplayName("Установка флага доступности C2C у дропоффа с возвратным СЦ, ошибка")
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = AUTHORITY_ROLE_LOGISTICS_POINT_EDIT)
    @DatabaseSetup(
        value = "/data/controller/admin/logisticsPoint/before/return_sorting_center.xml",
        type = DatabaseOperation.INSERT
    )
    void updateDropoffWithoutReturnsToC2CError() throws Exception {
        updateLogisticsPoint(10L, "data/controller/admin/logisticsPoint/update_to_c2c.json")
            .andExpect(status().isBadRequest())
            .andExpect(status().reason("Point can't become available for C2C"));
    }

    @Nonnull
    private ResultActions getLogisticsPointGrid() throws Exception {
        return mockMvc.perform(get("/admin/lms/logistics-point"));
    }

    @Nonnull
    private ResultActions getLogisticsPointDetail(long id) throws Exception {
        return mockMvc.perform(get("/admin/lms/logistics-point/" + id));
    }

    @Nonnull
    private ResultActions performActionOnLogisticsPoints(String actionName) throws Exception {
        return mockMvc.perform(
            post("/admin/lms/logistics-point" + actionName)
                .contentType(MediaType.APPLICATION_JSON)
                .content(pathToJson("data/controller/admin/logisticsPoint/selected.json"))
        );
    }

    @Nonnull
    private ResultActions updateLogisticsPoint(long id, String fileName) throws Exception {
        return mockMvc.perform(
            put("/admin/lms/logistics-point/" + id)
                .contentType(MediaType.APPLICATION_JSON)
                .content(pathToJson(fileName))
        );
    }

    @Nonnull
    private ResultActions updateLogisticsPoint(long id) throws Exception {
        return updateLogisticsPoint(id, "data/controller/admin/logisticsPoint/update.json");
    }
}
