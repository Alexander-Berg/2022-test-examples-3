package ru.yandex.market.logistics.management.controller.admin;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

import ru.yandex.market.logistics.management.AbstractContextualTest;
import ru.yandex.market.logistics.management.util.WithBlackBoxUser;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static ru.yandex.market.logistics.management.service.plugin.LMSPlugin.AUTHORITY_ROLE_LOGISTICS_POINT_GATE;
import static ru.yandex.market.logistics.management.service.plugin.LMSPlugin.AUTHORITY_ROLE_LOGISTICS_POINT_GATE_EDIT;
import static ru.yandex.market.logistics.management.util.TestUtil.pathToJson;
import static ru.yandex.market.logistics.management.util.TestUtil.testJson;

@ParametersAreNonnullByDefault
@DatabaseSetup("/data/controller/admin/logisticsPoint/before/prepare_data.xml")
class LmsControllerLogisticsPointGateTest extends AbstractContextualTest {

    @Test
    void logisticsPointGateGridUnauthorized() throws Exception {
        getLogisticsPointGateGrid().andExpect(status().isUnauthorized());
    }

    @Test
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = {})
    void logisticsPointGateGridForbidden() throws Exception {
        getLogisticsPointGateGrid().andExpect(status().isForbidden());
    }

    @Test
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = AUTHORITY_ROLE_LOGISTICS_POINT_GATE)
    void logisticsPointGateGrid() throws Exception {
        getLogisticsPointGateGrid()
            .andExpect(status().isOk())
            .andExpect(testJson(
                "data/controller/admin/logisticsPoint/gate/result/gate_grid.json",
                false
            ));
    }

    @Test
    void logisticsPointGateDetailUnauthorized() throws Exception {
        getLogisticsPointGateDetail(1L).andExpect(status().isUnauthorized());
    }

    @Test
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = {})
    void logisticsPointGateDetailForbidden() throws Exception {
        getLogisticsPointGateDetail(1L).andExpect(status().isForbidden());
    }

    @Test
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = AUTHORITY_ROLE_LOGISTICS_POINT_GATE)
    void logisticsPointGateDetailNotFound() throws Exception {
        getLogisticsPointGateDetail(0L).andExpect(status().isNotFound());
    }

    @Test
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = AUTHORITY_ROLE_LOGISTICS_POINT_GATE)
    void logisticsPointGateDetail() throws Exception {
        getLogisticsPointGateDetail(1L)
            .andExpect(status().isOk())
            .andExpect(testJson(
                "data/controller/admin/logisticsPoint/gate/result/gate_detail.json",
                false
            ));
    }

    @Test
    void logisticsPointGateNewUnauthorized() throws Exception {
        getLogisticsPointGateNew().andExpect(status().isUnauthorized());
    }

    @Test
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = {})
    void logisticsPointGateNewForbidden() throws Exception {
        getLogisticsPointGateNew().andExpect(status().isForbidden());
    }

    @Test
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = AUTHORITY_ROLE_LOGISTICS_POINT_GATE_EDIT)
    void logisticsPointGateNew() throws Exception {
        getLogisticsPointGateNew()
            .andExpect(status().isOk())
            .andExpect(testJson(
                "data/controller/admin/logisticsPoint/gate/result/gate_new.json",
                false
            ));
    }

    @Test
    void logisticsPointGateCreateUnauthorized() throws Exception {
        createLogisticsPointGate("gate/request/gate_create").andExpect(status().isUnauthorized());
    }

    @Test
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = {})
    void logisticsPointGateCreateForbidden() throws Exception {
        createLogisticsPointGate("gate/request/gate_create").andExpect(status().isForbidden());
    }

    @Test
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = AUTHORITY_ROLE_LOGISTICS_POINT_GATE_EDIT)
    void logisticsPointGateCreateNotFound() throws Exception {
        createLogisticsPointGate("gate/request/gate_create_not_found").andExpect(status().isNotFound());
    }

    @Test
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = AUTHORITY_ROLE_LOGISTICS_POINT_GATE_EDIT)
    void logisticsPointGateCreateExisting() throws Exception {
        createLogisticsPointGate("gate/request/gate_create_existing").andExpect(status().isBadRequest());
    }

    @Test
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = AUTHORITY_ROLE_LOGISTICS_POINT_GATE_EDIT)
    void logisticsPointGateCreateNotWarehouse() throws Exception {
        createLogisticsPointGate("gate/request/gate_create_not_warehouse").andExpect(status().isBadRequest());
    }

    @Test
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = AUTHORITY_ROLE_LOGISTICS_POINT_GATE_EDIT)
    void logisticsPointGateCreate() throws Exception {
        createLogisticsPointGate("gate/request/gate_create")
            .andExpect(status().isCreated())
            .andExpect(header().string("location", "http://localhost/admin/lms/logistics-point-gate/5"));
    }

    @Test
    void logisticsPointGateDeleteUnauthorized() throws Exception {
        deleteLogisticsPointGate(5L).andExpect(status().isUnauthorized());
    }

    @Test
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = {})
    void logisticsPointGateDeleteForbidden() throws Exception {
        deleteLogisticsPointGate(5L).andExpect(status().isForbidden());
    }

    @Test
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = AUTHORITY_ROLE_LOGISTICS_POINT_GATE_EDIT)
    void logisticsPointGateDeleteNotFound() throws Exception {
        deleteLogisticsPointGate(10L).andExpect(status().isNotFound());
    }

    @Test
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = AUTHORITY_ROLE_LOGISTICS_POINT_GATE_EDIT)
    void logisticsPointGateDelete() throws Exception {
        deleteLogisticsPointGate(4L).andExpect(status().isOk());
    }

    @Test
    void logisticsPointGateUpdateUnauthorized() throws Exception {
        updateLogisticsPointGate(1L).andExpect(status().isUnauthorized());
    }

    @Test
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = {})
    void logisticsPointGateUpdateForbidden() throws Exception {
        updateLogisticsPointGate(1L).andExpect(status().isForbidden());
    }

    @Test
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = AUTHORITY_ROLE_LOGISTICS_POINT_GATE_EDIT)
    void logisticsPointGateUpdateNotFound() throws Exception {
        updateLogisticsPointGate(11L).andExpect(status().isNotFound());
    }

    @Test
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = AUTHORITY_ROLE_LOGISTICS_POINT_GATE_EDIT)
    void logisticsPointGateUpdate() throws Exception {
        updateLogisticsPointGate(1L)
            .andExpect(status().isOk())
            .andExpect(testJson(
                "data/controller/admin/logisticsPoint/gate/result/gate_update_result.json",
                false
            ));
    }

    @Test
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = AUTHORITY_ROLE_LOGISTICS_POINT_GATE_EDIT)
    void logisticsPointGateUpdateSchedule() throws Exception {
        updateLogisticsPointGateSchedule()
            .andExpect(status().isOk())
            .andExpect(testJson(
                "data/controller/admin/logisticsPoint/gate/result/gate_schedule_update_result.json",
                false
            ));
    }

    @Test
    @WithBlackBoxUser(login = "lmsUser", uid = 1, authorities = AUTHORITY_ROLE_LOGISTICS_POINT_GATE_EDIT)
    void logisticsPointGateDisable() throws Exception {
        disableLogisticsPointGate()
            .andExpect(status().isOk())
            .andExpect(testJson(
                "data/controller/admin/logisticsPoint/gate/result/gate_disable_result.json",
                false
            ));
    }

    @Nonnull
    private ResultActions getLogisticsPointGateGrid() throws Exception {
        return mockMvc.perform(get("/admin/lms/logistics-point-gate"));
    }

    @Nonnull
    private ResultActions getLogisticsPointGateDetail(long id) throws Exception {
        return mockMvc.perform(get("/admin/lms/logistics-point-gate/" + id));
    }

    @Nonnull
    private ResultActions getLogisticsPointGateNew() throws Exception {
        return mockMvc.perform(get("/admin/lms/logistics-point-gate/new"));
    }

    @Nonnull
    private ResultActions createLogisticsPointGate(String fileName) throws Exception {
        return mockMvc.perform(
            post("/admin/lms/logistics-point-gate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(pathToJson("data/controller/admin/logisticsPoint/" + fileName + ".json"))
        );
    }

    @Nonnull
    private ResultActions updateLogisticsPointGate(long id) throws Exception {
        return mockMvc.perform(
            put("/admin/lms/logistics-point-gate/" + id)
                .contentType(MediaType.APPLICATION_JSON)
                .content(pathToJson("data/controller/admin/logisticsPoint/gate/request/gate_update.json"))
        );
    }

    @Nonnull
    private ResultActions updateLogisticsPointGateSchedule() throws Exception {
        return mockMvc.perform(
            put("/admin/lms/logistics-point-gate/" + 1 + "/schedule")
                .contentType(MediaType.APPLICATION_JSON)
                .content(pathToJson("data/controller/admin/logisticsPoint/gate/request/gate_schedule_update.json"))
        );
    }

    @Nonnull
    private ResultActions disableLogisticsPointGate() throws Exception {
        return mockMvc.perform(
            put("/admin/lms/logistics-point-gate/" + 1)
                .contentType(MediaType.APPLICATION_JSON)
                .content(pathToJson("data/controller/admin/logisticsPoint/gate/request/gate_disable.json"))
        );
    }

    @Nonnull
    private ResultActions deleteLogisticsPointGate(long id) throws Exception {
        return mockMvc.perform(delete("/admin/lms/logistics-point-gate/" + id));
    }
}
